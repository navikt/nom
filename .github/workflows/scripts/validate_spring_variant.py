import os
import pathlib
import re
import sys
from urllib.parse import urlsplit

base = pathlib.Path(os.environ['BASE_MANIFEST'])
variant = pathlib.Path(os.environ['VARIANT_MANIFEST'])
suffix = os.environ['SUFFIX']


def scalar(value):
    match = re.fullmatch(r'''(?:"([^"]*)"|'([^']*)'|([^\s#'"`]+))\s*(?:#.*)?''', value.strip())
    if not match:
        raise ValueError(f'invalid manifest scalar: {value}')
    return next(part for part in match.groups() if part is not None)


def app_name(path):
    lines = path.read_text().splitlines()
    if not any(line.startswith('kind:') and scalar(line.partition(':')[2]) == 'Application'
               for line in lines if line.startswith('kind:')):
        raise ValueError(f'{path}: expected a Nais Application')
    in_metadata = False
    for line in lines:
        if line == 'metadata:':
            in_metadata = True
        elif in_metadata and line and not line.startswith((' ', '#')):
            break
        elif in_metadata:
            if line.startswith('  name:'):
                name = scalar(line.partition(':')[2])
                if re.fullmatch(r'[a-z0-9]+(?:-[a-z0-9]+)*', name):
                    return name
                raise ValueError(f'{path}: invalid metadata.name')
    raise ValueError(f'{path}: missing metadata.name')


def list_values(lines, heading, item_indent, prefix=''):
    indent = len(heading) - len(heading.lstrip())
    values = []
    in_section = False
    found_section = False
    for line in lines:
        if line == heading:
            in_section = True
            found_section = True
        elif in_section and line.strip() and not line.lstrip().startswith('#'):
            if len(line) - len(line.lstrip()) <= indent:
                break
            if line.startswith(item_indent + '- '):
                entry = line[len(item_indent) + 2:]
                if prefix:
                    if not entry.startswith(prefix):
                        raise ValueError(f'malformed entry in {heading}: {line}')
                    entry = entry[len(prefix):]
                values.append(scalar(entry))
            elif len(line) - len(line.lstrip()) == len(item_indent) and line.lstrip().startswith('-'):
                raise ValueError(f'malformed entry in {heading}: {line}')
    if found_section and not values:
        raise ValueError(f'empty or malformed {heading}')
    return values


def ingress_urls(path):
    # Do not mistake an unsupported YAML spelling for an absent ingress.
    contents = path.read_bytes()
    if b'\r' in contents or b'\t' in contents:
        raise ValueError(f'{path}: unsupported line endings or indentation')
    lines = contents.decode('utf-8').splitlines()
    urls = []
    found = False
    later_document = False
    in_ingresses = False
    for line in lines:
        if line.strip() == '---':
            later_document = True
        if (re.match(r'^\s*<<\s*:', line)
            or re.search(r'(?<!\S)[&*][a-zA-Z0-9_-]+(?=\s|$)', line)):
            raise ValueError(f'{path}: YAML aliases and merge keys are not supported in variant manifests')
        if (not line.lstrip().startswith('#')
            and re.search(r'''(?<![\w-])["']?ingresses["']?\s*:''', line)):
            if line != '  ingresses:' or found or later_document:
                raise ValueError(f'{path}: unsupported or repeated ingresses section')
            found = True
            in_ingresses = True
            continue
        if not in_ingresses or not line.strip() or line.lstrip().startswith('#'):
            continue
        indent = len(line) - len(line.lstrip())
        if indent <= 2:
            in_ingresses = False
            continue
        if not line.startswith('    - '):
            raise ValueError(f'{path}: malformed ingress: {line}')
        urls.append(scalar(line[len('    - '):]))
    if found and not urls:
        raise ValueError(f'{path}: empty ingresses section')
    return urls


def ingress_host(url, path):
    parts = urlsplit(url)
    host = parts.hostname
    if (not host or '.' not in host or url != f'https://{host}' or len(host) > 253
        or any(len(label) > 63 or label.startswith('xn--') or not re.fullmatch(
            r'[a-z0-9](?:[a-z0-9-]*[a-z0-9])?', label) for label in host.split('.'))):
        raise ValueError(f'{path}: invalid HTTPS ingress: {url}')
    return host


try:
    if not re.fullmatch(r'[a-z0-9]+(?:-[a-z0-9]+)*', suffix):
        raise ValueError('invalid variant suffix')
    environment = next((end for end in ('-dev-gcp', '-dev') if base.stem.endswith(end)), '')
    expected_path = base.with_name(f'{base.stem.removesuffix(environment)}-{suffix}{environment}{base.suffix}')
    if variant != expected_path or not variant.is_file():
        raise ValueError(f'variant manifest must be {expected_path}')
    expected_name = f'{app_name(base)}-{suffix}'
    if len(expected_name) > 63 or app_name(variant) != expected_name:
        raise ValueError(f'{variant}: metadata.name must be {expected_name}')
    lines = variant.read_text().splitlines()
    sql_names = list_values(lines, '    sqlInstances:', '      ', 'name: ')
    if any(not re.fullmatch(r'[a-z0-9-]+', name) or not name.startswith(expected_name) for name in sql_names):
        raise ValueError(f'{variant}: every gcp.sqlInstances.name must start with {expected_name}')
    base_ingresses = ingress_urls(base)
    ingresses = ingress_urls(variant)
    base_hosts = [ingress_host(url, base) for url in base_ingresses]
    hosts = [ingress_host(url, variant) for url in ingresses]
    if len(set(base_hosts)) != len(base_hosts) or len(set(hosts)) != len(hosts):
        raise ValueError(f'{variant}: duplicate ingress')
    expected_hosts = set()
    for host in base_hosts:
        first, domain = host.split('.', 1)
        expected = f'{first}-{suffix}.{domain}'
        ingress_host(f'https://{expected}', variant)
        expected_hosts.add(expected)
    if len(hosts) != len(expected_hosts) or set(hosts) != expected_hosts:
        raise ValueError(f'{variant}: ingresses must use the base hosts with -{suffix}')
except (KeyError, OSError, ValueError, AttributeError) as error:
    sys.exit(str(error))
