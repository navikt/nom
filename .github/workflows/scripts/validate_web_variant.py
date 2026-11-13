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

def section_values(lines, heading):
    values = []
    in_section = False
    found_section = False
    for line in lines:
        if line == heading:
            in_section = True
            found_section = True
        elif in_section and line.strip() and not line.lstrip().startswith('#'):
            if len(line) - len(line.lstrip()) <= len(heading) - len(heading.lstrip()):
                break
            if line.startswith('    - '):
                values.append(scalar(line.removeprefix('    - ')))
            elif line.startswith('    -'):
                raise ValueError(f'malformed ingress: {line}')
    if found_section and not values:
        raise ValueError(f'empty or malformed {heading}')
    return values

def manifest_info(path):
    lines = path.read_text().splitlines()
    if not any(line.startswith('kind:') and scalar(line.partition(':')[2]) == 'Application'
               for line in lines if line.startswith('kind:')):
        raise ValueError(f'{path}: expected a Nais Application')
    metadata = lines.index('metadata:')
    name = next((scalar(line.partition(':')[2]) for line in lines[metadata + 1:]
                 if line.startswith('  name:')), None)
    if not name or not re.fullmatch(r'[a-z0-9]+(?:-[a-z0-9]+)*', name):
        raise ValueError(f'{path}: invalid metadata.name')
    return name, section_values(lines, '  ingresses:')

try:
    if not isinstance(suffix, str) or not re.fullmatch(r'[a-z0-9]+(?:-[a-z0-9]+)*', suffix):
        raise ValueError('invalid variant suffix')
    environment = next((end for end in ('-dev-gcp', '-dev') if base.stem.endswith(end)), '')
    expected_path = base.with_name(f'{base.stem.removesuffix(environment)}-{suffix}{environment}{base.suffix}')
    if variant != expected_path or not variant.is_file():
        raise ValueError(f'variant manifest must be {expected_path}')
    base_name, base_ingresses = manifest_info(base)
    name, ingresses = manifest_info(variant)
    expected_name = f'{base_name}-{suffix}'
    if name != expected_name or len(name) > 63:
        raise ValueError(f'{variant}: metadata.name must be {expected_name}')
    expected_hosts = {f'{urlsplit(url).hostname.split(".", 1)[0]}-{suffix}.{urlsplit(url).hostname.split(".", 1)[1]}'
                      for url in base_ingresses}
    hosts = [urlsplit(url).hostname for url in ingresses]
    if not ingresses or len(hosts) != len(set(hosts)) or any(
        urlsplit(url).scheme != 'https' or urlsplit(url).hostname not in expected_hosts
        or urlsplit(url).port is not None or urlsplit(url).path not in ('', '/')
        for url in ingresses
    ) or not set(hosts).issubset(expected_hosts):
        raise ValueError(f'{variant}: ingresses must be a non-empty subset of the base hosts with -{suffix}')
except (OSError, ValueError, AttributeError) as error:
    sys.exit(str(error))
