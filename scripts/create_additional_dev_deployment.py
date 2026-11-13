#!/usr/bin/env python3
"""Generate a review-required, isolated NOM dev variant. Never deploys or overwrites files.

Run from any directory: python3 scripts/create_additional_dev_deployment.py <suffix>
Every line prefixed with @ must be reviewed before committing or deploying.
"""

import argparse
import json
import re
import subprocess
from pathlib import Path
from urllib.parse import urlsplit, urlunsplit


ROOT = Path(__file__).resolve().parent.parent
SCRIPT = Path(__file__).name
AUTO = f"# Auto-utfylt av '{SCRIPT}'"

APPS = {
    "nom-api": ("api", "api-dev-gcp.yaml", "api", "deploy-api.yaml"),
    "nom-ui2": ("ui2", "ui2-dev.yaml", None, "deploy-ui2.yaml"),
    "nav-ident": ("nav-ident", "nav-ident-dev-gcp.yaml", "nav-ident", "deploy-nav-ident.yaml"),
    "nom-admin": ("admin", "admin-dev-gcp.yaml", "admin", "deploy-admin.yaml"),
    "nom-admin-web": ("admin-web", "admin-web-dev.yaml", None, "deploy-admin-web.yaml"),
    "nom-rdbms-data": ("nom-rdbms-data", "nom-rdbms-data-dev-gcp.yaml", "nom-rdbms-data", "deploy-nom-rdbms-data.yaml"),
    "nom-paarorende": ("paarorende", "paarorende-dev.yml", "paarorende", "deploy-paarorende.yml"),
    "nom-sf-integrasjon": ("nom-sf-integrasjon", "nom-sf-integrasjon-dev-gcp.yaml", "nom-sf-integrasjon", "deploy-nom-sf-integrasjon.yaml"),
}
STREAMS = {
    "nom-intern-organisering-state": ("internal/organisering-state", "intern-organisering-state.yaml", "deploy-streams-intern-organisering-state.yaml"),
    "nom-intern-orgenhet-leder-state-v2": ("internal/orgenhetleder-state", "intern-orgenhet-leder-state.yaml", "deploy-streams-intern-orgenhet-leder-state.yaml"),
    "nom-intern-orgenhet-state": ("internal/orgenhet-state", "intern-orgenhet-state.yaml", "deploy-streams-intern-orgenhet-state.yaml"),
    "nom-intern-orgtilknytning-state": ("internal/orgtilknytning-state", "intern-orgtilknytning-state.yaml", "deploy-streams-intern-orgtilknytning-state.yaml"),
    "nom-intern-ressurs-state": ("internal/ressurs-state", "intern-ressurs-state.yaml", "deploy-streams-intern-ressurs-state.yaml"),
    "nom-kafka-api-orgenhet-state": ("kafka-api/api-orgenhet-state", "kafka-api-orgenhet-state.yaml", "deploy-streams-kafka-api-orgenhet-state.yaml"),
    "nom-kafka-api-ressurs-state-v4": ("kafka-api/api-ressurs-state-v4", "kafka-api-ressurs-state-v4-dev.yaml", "deploy-streams-kafka-api-ressurs-state-v4.yaml"),
}
TOPICS = (
    "nom.ressurs-eventsource", "nom.intern-ressurs-state", "nom.orgenhet-eventsource",
    "nom.intern-orgenhet-state", "nom.orgtilknytning-eventsource",
    "nom.intern-orgtilknytning-state", "nom.organisering-eventsource",
    "nom.intern-organisering-state", "nom.nom-authorize-eventsource",
    "nom.orgenhet-leder-eventsource", "nom.intern-orgenhet-leder-state",
    "nom.nom-mastered-ressurs", "nav-ident", "nav-ident-rnr",
    "nom.api-ressurs-state-v4", "nom.api-orgenhet-state-v3",
)
ADMIN_INACTIVE_TOPICS = {
    "nom.api-hendelser-v1",
    "nom.api-ressurs-hendelse-v1",
    "nom.intern-orgtilknytning-state-dlt",
    "nom.intern-ressurs-state-dlt",
}
STATE_TYPES = frozenset({"ressurs", "orgenhet", "orgtilknytning", "organisering",
                         "mastered-ressurs", "delegert-leder", "orgenhet-leder"})
STATE_APPS = {"api", "admin", "nom-sf-integrasjon", "nom-rdbms-data"}
NAMES = set(APPS) | set(STREAMS) | {"nav-ident-redis"}
SHARED_INBOUND = {"org-token-tool", "org-ekstern-proxy"}
SHARED_OUTBOUND = {"logging", "norg2", "orgenhet-kostnadssted-mapping-api", "nom-azure"}
SHARED_DEV_CALLERS = (
    (Path(".nais/apps/nom-azure/nom-azure-dev-gcp.yaml"), "nom-ui2", "org"),
    (Path(".nais/apps/proxy/proxy-dev-fss.yaml"), "nom-api", "nom"),
)
SHARED_EXTERNAL_HOSTS = (
    "org-unleash-api.nav.cloud.nais.io",
    "nom-proxy.dev-fss-pub.nais.io",
)
OMITTED_EXTERNAL_HOSTS = {"nom-remedy-integrasjon.dev-fss-pub.nais.io"}


def marked(line: str, reason: str) -> str:
    """Prefix the untouched original line with @ and append its review reason."""
    return f"@{line} # KONTROLLER MANUELT - Kontroller verdi og tilgang før deploy - '{SCRIPT}': {reason}"


def changed(line: str, old: str, new: str) -> str:
    if old == new:
        return line
    result = line.replace(old, new)
    return result if AUTO in result else result + f" {AUTO}"


def source_header(root: Path, sources: list[Path]) -> str:
    lines = []
    for source in sources:
        relative = source.as_posix()
        repository = root if (root / ".git").exists() else ROOT
        committed = subprocess.run(
            ["git", "-C", str(repository), "show", f"HEAD:{relative}"],
            capture_output=True, check=True,
        ).stdout
        if (root / source).read_bytes() != committed:
            raise ValueError(f"Kilden avviker fra HEAD: {relative}")
        commit = subprocess.run(
            ["git", "-C", str(repository), "log", "-1", "--format=%H", "--", relative],
            capture_output=True, text=True, check=True,
        ).stdout.strip()
        if not re.fullmatch(r"[a-f0-9]{40}", commit):
            raise ValueError(f"Fant ikke commit SHA for {relative}")
        lines.append(f"# Kopiert fra {relative} (commit: {commit})")
    return "\n".join(lines) + "\n"


def variant_manifest_path(path: Path, suffix: str) -> Path:
    stem = path.stem
    environment = next((end for end in ("-dev-gcp", "-dev") if stem.endswith(end)), "")
    new_stem = stem.removesuffix(environment) + f"-{suffix}" + environment
    return path.with_name(new_stem + path.suffix)


def section(text: str, key: str) -> str:
    """Extract a top-level YAML mapping section without a third-party YAML dependency."""
    lines = text.splitlines()
    start = next((i for i, line in enumerate(lines) if re.fullmatch(re.escape(key) + r":\s*(?:#.*)?", line)), None)
    if start is None:
        return ""
    end = next((i for i in range(start + 1, len(lines)) if re.match(r"^[\w][^#]*:", lines[i]) or lines[i].startswith("---")), len(lines))
    return "\n".join(lines[start:end]).rstrip() + "\n"


def state_sources(root: Path, module: str) -> list[Path]:
    """Return enabled library defaults, failing if the library adds an unknown type."""
    directory = root / "libs/nom-state/src/main/resources"
    found = {path.name.removesuffix("-state.yaml") for path in directory.glob("*-state.yaml")}
    if found != STATE_TYPES:
        raise ValueError(f"Ukjente eller manglende nom-state typer: {sorted(found ^ STATE_TYPES)}")
    if module not in STATE_APPS:
        return []
    base = (root / "apps" / module / "src/main/resources/application.yaml").read_text()
    enabled = re.findall(r"(?m)^nom\.libs\.state\.([\w-]+)\.enabled:\s*true\s*(?:#.*)?$", base)
    other = re.findall(r"(?m)^nom\.libs\.state\.([\w-]+)\.enabled:", base)
    if len(enabled) != len(other) or len(enabled) != len(set(enabled)) or not enabled or not set(enabled) <= STATE_TYPES:
        raise ValueError(f"Ukjent eller ugyldig nom-state aktivering i {module}")
    expected = STATE_TYPES if module != "nom-rdbms-data" else {"delegert-leder"}
    if set(enabled) != expected:
        raise ValueError(f"Uventede nom-state typer i {module}: {sorted(set(enabled) ^ expected)}")
    return [Path("libs/nom-state/src/main/resources") / f"{kind}-state.yaml" for kind in enabled]


def state_overrides(root: Path, module: str, suffix: str) -> list[str]:
    sources = state_sources(root, module)
    if not sources:
        return []
    app = "nom-api" if module == "api" else "nom-admin" if module == "admin" else module
    team = "nom" if module == "api" else "org"
    documents = []
    for source in ("topics-internal.yaml", "nav-ident-topics.yaml", "topics-kafka-api.yaml"):
        documents.extend(re.split(r"(?m)^---\s*$", (root / ".nais/topics" / source).read_text()))
    lines = []
    for source in sources:
        kind = source.name.removesuffix("-state.yaml")
        matches = re.findall(r"(?m)^\s+state-topic:\s*(org\.[\w.-]+)\s*(?:#.*)?$", (root / source).read_text())
        if len(matches) != 1 or matches[0].removeprefix("org.") not in TOPICS:
            raise ValueError(f"Mangler kjent state-topic i {source}")
        topic = matches[0].removeprefix("org.")
        template = [doc for doc in documents if re.search(r"(?m)^  name: " + re.escape(topic) + r"\s*$", doc)]
        if len(template) != 1:
            raise ValueError(f"Mangler entydig topic-mal for {topic}")
        acl = re.findall(r"(?m)^    - team: ([\w-]+)\s*\n      application: ([\w-]+)\s*\n      access: ([\w-]+)", template[0])
        if (team, app, "read") not in acl and (team, app, "readwrite") not in acl:
            raise ValueError(f"Mangler lese-ACL for {app}-{suffix} på {topic}-{suffix}")
        lines.append(f"nom.libs.state.{kind}.state-topic: {matches[0]}-{suffix} {AUTO}")
    return lines


def merge_mapping(base: list[str], overrides: list[str], indent: int = 0) -> list[str]:
    """Merge mapping nodes without adding a second app key or losing base siblings."""
    def nodes(lines):
        starts = [(i, match.group(1)) for i, line in enumerate(lines)
                  if (match := re.match(r"^@?" + " " * indent + r"([\w.-]+):", line))]
        return [(key, i,
                  starts[pos + 1][0] if pos + 1 < len(starts) else len(lines))
                for pos, (i, key) in enumerate(starts)]

    result = base[:]
    for key, start, end in nodes(overrides):
        replacement = overrides[start:end]
        existing = next(((a, b) for name, a, b in nodes(result) if name == key), None)
        if existing is None:
            result.extend(replacement)
            continue
        a, b = existing
        old = result[a:b]
        children = [line for line in replacement[1:] if line.strip() and not line.lstrip().startswith('#')]
        old_children = [line for line in old[1:] if line.strip() and not line.lstrip().startswith('#')]
        mapping = (children and old_children and
                   all(re.match(r"^@?" + " " * (indent + 2) + r"[\w.-]+:", line)
                       for line in children[:1] + old_children[:1]))
        result[a:b] = ([old[0]] + merge_mapping(old[1:], replacement[1:], indent + 2)
                       if mapping else replacement)
    return result


def access_rules(block: str) -> list[tuple[str, str, str]]:
    """Read explicit application/namespace/cluster triples; reject incomplete rules."""
    result = []
    for entry in re.split(r"(?m)^        - application: ", block)[1:]:
        app = entry.splitlines()[0].split(" #", 1)[0].strip()
        namespace = re.search(r"(?m)^          namespace: ([\w-]+)\s*$", entry)
        cluster = re.search(r"(?m)^          cluster: ([\w-]+)\s*$", entry)
        if not re.fullmatch(r"[a-z0-9-]+", app):
            raise ValueError(f"Ukjent tilgangsregel: {entry}")
        if namespace is None and app not in {"nav-ident-redis", "nav-ident"}:
            raise ValueError(f"Ukjent tilgangsregel: {entry}")
        result.append((app, namespace.group(1) if namespace else "org", cluster.group(1) if cluster else None))
    return result


def add_shared_dev_caller(source: str, app: str, namespace: str, suffix: str) -> str:
    """Add exactly one variant caller while preserving every existing shared-app rule."""
    name = f"{app}-{suffix}"
    match = re.search(r"(?ms)^  accessPolicy:\s*\n.*?^    inbound:\s*\n      rules:\s*\n(.*?)(?=^    outbound:|^  [A-Za-z]|^---|\Z)", source)
    if not match:
        raise ValueError(f"Fant ikke accessPolicy.inbound.rules for {app}")
    rules = access_rules(match.group(1))
    existing = [rule for rule in rules if rule[0] == name]
    if existing:
        if existing != [(name, namespace, "dev-gcp")]:
            raise ValueError(f"Eksisterende tilgangsregel for {name} avviker")
        return source
    if (app, namespace, "dev-gcp") not in rules:
        raise ValueError(f"Fant ikke forventet originalregel for {app}")
    addition = (f"        - application: {name} {AUTO}\n"
                f"          namespace: {namespace}\n"
                "          cluster: dev-gcp\n")
    return source[:match.end(1)] + addition + source[match.end(1):]


def manifest(source: str, suffix: str, name: str) -> str:
    documents = re.split(r"(?m)^---\s*$", source)
    output = []
    for number, document in enumerate(documents):
        current = "nav-ident-redis" if name == "nav-ident" and number == 1 else name
        if not re.search(r"(?m)^  name: ['\"]?" + re.escape(current) + r"['\"]?\s*(?:#.*)?$", document):
            raise ValueError(f"Missing metadata.name {current}")
        lines = document.splitlines()
        transformed = []
        in_metadata = False
        in_sql = False
        in_buckets = False
        in_azure = False
        in_envfrom = False
        in_access = False
        in_external = False
        in_limits = False
        in_ingresses = False
        in_env = False
        last_env = ""
        in_resources = False
        for line in lines:
            if line in ('kind: "Application"', "kind: 'Application'"):
                line = f"kind: Application {AUTO}"
            if line == "metadata:":
                in_metadata = True
            elif line.startswith("spec:"):
                in_metadata = False
            if in_metadata and re.match(r"^  name: ", line):
                line = f"  name: {current}-{suffix} {AUTO}"
            if re.match(r"^  accessPolicy:\s*(?:#.*)?$", line):
                in_access = True
                # Only explicitly known in-variant peers are allowed; never inherit external callers.
                transformed.append("  accessPolicy:")
                # Extract only explicit application entries in the inbound block of the source.
                inbound = re.search(r"(?ms)^    inbound:\s*\n(.*?)(?=^    outbound:|^  [A-Za-z]|\Z)", document)
                inbound_rules = access_rules(inbound.group(1)) if inbound else []
                allowed_inbound = [rule for rule in inbound_rules if rule[0] in NAMES and rule[0] != current
                                   or rule[0] in SHARED_INBOUND]
                if allowed_inbound:
                    transformed.extend(["    inbound:", "      rules:"])
                    for peer, namespace, cluster in allowed_inbound:
                        if peer in SHARED_INBOUND:
                            transformed.extend([f"        - application: {peer}", f"          namespace: {namespace}"])
                            if cluster:
                                transformed.append(f"          cluster: {cluster}")
                        else:
                            transformed.extend([f"        - application: {peer}-{suffix} {AUTO}",
                                                f"          namespace: {namespace}", "          cluster: dev-gcp"])
                outbound = re.search(r"(?ms)^    outbound:\s*\n(.*?)(?=^  [A-Za-z]|\Z)", document)
                rules = re.search(r"(?ms)^      rules:\s*\n(.*?)(?=^      external:|^    [A-Za-z]|\Z)", outbound.group(1) if outbound else "")
                outbound_rules = access_rules(rules.group(1)) if rules else []
                allowed_outbound = [rule for rule in outbound_rules if rule[0] in SHARED_OUTBOUND
                                    or rule[0] in NAMES and rule[0] != current]
                external = re.search(r"(?ms)^      external:\s*\n(.*?)(?=^    [A-Za-z]|^  [A-Za-z]|\Z)", outbound.group(1) if outbound else "")
                external_lines = []
                if external:
                    for external_line in external.group(1).splitlines():
                        host = re.fullmatch(r"\s*- host: ([\w.-]+)(?:\s+#.*)?", external_line)
                        if not host:
                            if external_line.strip() and not external_line.lstrip().startswith('#'):
                                raise ValueError(f"Ukjent ekstern regel i {name}: {external_line}")
                            continue
                        if host.group(1) in OMITTED_EXTERNAL_HOSTS:
                            continue
                        external_lines.append(external_line if host.group(1) in SHARED_EXTERNAL_HOSTS
                                              else marked(external_line, "ekstern host må vurderes"))
                if allowed_outbound or external_lines:
                    transformed.append("    outbound:")
                if allowed_outbound:
                    transformed.append("      rules:")
                    for peer, namespace, cluster in allowed_outbound:
                        shared = peer in SHARED_OUTBOUND
                        transformed.extend([f"        - application: {peer if shared else peer + '-' + suffix}"
                                            + ("" if shared else f" {AUTO}"),
                                            f"          namespace: {namespace}"])
                        if cluster or not shared:
                            transformed.append(f"          cluster: {cluster if shared else 'dev-gcp'}")
                if external_lines:
                    transformed.extend(["      external:", *external_lines])
                continue
            if in_access:
                if not re.match(r"^  [A-Za-z]", line) and not line.startswith("---"):
                    continue
                in_access = False
            if re.match(r"^  (gcp|resources|ingresses|azure|envFrom|env):", line):
                in_ingresses = line.startswith("  ingresses:")
                in_azure = line.startswith("  azure:")
                in_envfrom = line.startswith("  envFrom:")
                in_env = line.startswith("  env:")
                in_limits = False
            if re.match(r"^  [A-Za-z]", line):
                in_resources = line.startswith("  resources:")
            if re.match(r"^  [A-Za-z]", line) and not line.startswith("  env:"):
                in_env = False
            if re.match(r"^    (sqlInstances|buckets):", line):
                in_sql = line.startswith("    sqlInstances:")
                in_buckets = line.startswith("    buckets:")
            elif re.match(r"^    [A-Za-z].*:", line):
                in_sql = in_buckets = False
            if line == "    limits:" and in_resources:
                in_limits = True
            elif in_limits and re.match(r"^    [a-zA-Z]", line):
                in_limits = False
            if in_limits and re.match(r"^      cpu:", line):
                continue  # No CPU limits in newly created manifests.
            if in_sql and re.match(r"^      - name: ([\w-]+)", line):
                old = re.search(r"name: ([\w-]+)", line).group(1)
                line = changed(line, old, f"{name}-{suffix}")
            if in_sql and re.match(r"^      - type: ", line):
                transformed.append(f"      - name: {name}-{suffix} {AUTO}")
                line = line.replace("      - type: ", "        type: ", 1)
            if in_sql and re.match(r"^          - name: ([\w-]+)", line):
                old = re.search(r"name: ([\w-]+)", line).group(1)
                line = changed(line, old, f"{name}-{suffix}-db")
            if in_buckets and re.match(r"^      - name: ([\w-]+)", line):
                old = re.search(r"name: ([\w-]+)", line).group(1)
                line = changed(line, old, f"{old}-{suffix}")
            if in_ingresses and re.match(r"^    - https://", line):
                url = line.strip()[2:].split()[0]
                parsed = urlsplit(url)
                host = parsed.hostname or ""
                newhost = f"{host.split('.')[0]}-{suffix}.{host.split('.', 1)[1]}"
                line = changed(line, url, urlunsplit((parsed.scheme, newhost, parsed.path, parsed.query, parsed.fragment)))
            if in_env and re.match(r"^    - name: ", line):
                last_env = line.split(":", 1)[1].strip()
            if in_env and last_env == "SPRING_PROFILES_ACTIVE" and re.match(r"^      value: dev\s*$", line):
                line = changed(line, "value: dev", f"value: dev,{suffix}")
            if in_env and last_env == "SPRING_PROFILES_ACTIVE" and re.match(r"^      value:", line) and "dev," + suffix not in line:
                raise ValueError(f"Unexpected Spring dev profile in {name}: {line}")
            if in_env and last_env == "REDIS_HOST" and re.match(r"^      value:", line):
                line = changed(line, "nav-ident-redis.org", f"nav-ident-redis-{suffix}.org")
            if name == "nom-admin" and in_resources and re.match(r"^      memory:", line):
                line = marked(line, "minnebehov i ekstra DEV-miljø må vurderes")
            if name == "nav-ident" and "name: NAVIDENT_SEQUENCE" in line:
                line = marked(line, "sekvensområde må være unikt mellom miljøer")
            if name in {"nom-admin", "nom-sf-integrasjon"} and "http://nom-api.nom" in line:
                line = changed(line, "http://nom-api.nom", f"http://nom-api-{suffix}.nom")
            short_url_original = line
            if in_env and re.match(r"^      value: http://", line):
                for peer in sorted(NAMES - {"nav-ident-redis"}, key=len, reverse=True):
                    namespace = "nom" if peer == "nom-api" else "org"
                    line = re.sub(r"(?<![\w-])http://" + re.escape(peer) + r"\." + namespace + r"(?=\s|/|$)",
                                  lambda match: "http://" + peer + "-" + suffix + "." + namespace, line)
            if short_url_original != line and AUTO not in line:
                line += f" {AUTO}"
            # Rewrite only known variant identities, in internal service URLs and Azure audience scopes.
            if not in_metadata and not in_access and re.search(r"(?:\.svc\.cluster\.local|api://dev-gcp\.)", line):
                original = line
                for peer in sorted(NAMES, key=len, reverse=True):
                    line = re.sub(r"(?<![\w-])" + re.escape(peer) + r"(?=(?:\.(?:nom|org)\.svc|/\.default))",
                                  lambda m: f"{m.group(0)}-{suffix}", line)
                if line != original and AUTO not in line:
                    line += f" {AUTO}"
            if in_envfrom and re.match(r"^    - secret:", line) and not re.search(r"secret: [\w-]+-unleash-api-token(?:\s|$)", line):
                line = marked(line, "hemmelighet må godkjennes for varianten")
            if in_azure and re.match(r"^    application:", line):
                line = marked(line, "Azure-klient, grupper, redirect og azp må vurderes")
            transformed.append(line)
        if (name in STREAMS or name == "nom-paarorende") and number == 0 and not re.search(r"(?m)^    - name: SPRING_PROFILES_ACTIVE\s*$", "\n".join(transformed)):
            # These source manifests have no active profile; merge into env rather than duplicate env keys.
            active = f"dev,{suffix}"
            if "  env:" in transformed:
                at = transformed.index("  env:") + 1
                transformed[at:at] = ["    - name: SPRING_PROFILES_ACTIVE", f"      value: {active} {AUTO}"]
            else:
                transformed += ["  env:", "    - name: SPRING_PROFILES_ACTIVE",
                                f"      value: {active} {AUTO}"]
        output.append("\n".join(transformed).strip() + "\n")
    return "---\n".join(output)


def profile(root: Path, module: str, suffix: str, stream: bool = False) -> str:
    resources = root / "apps" / ("streams" if stream else "") / module / "src/main/resources"
    base = (resources / "application.yaml").read_text()
    devpath = resources / "application-dev.yaml"
    dev = devpath.read_text() if devpath.is_file() and not stream else ""
    # An explicit suffix profile overrides baseline app.topic settings (dev profile alone does not).
    app = section(base, "app")
    if not app and not dev:
        raise ValueError(f"No app or dev profile: {module}")
    if "org.nom." in base or "org.nav-ident" in base:
        if not app:
            raise ValueError(f"Could not extract topic settings from {module}")
    rows = app.splitlines()
    result = []
    for line in rows:
        unused_topic = re.match(
            r"^(\s*)dvhLastResultTopic:\s*org\.nom\.intern-dvh-last-result(?:\s+#.*)?$", line
        )
        if module == "api" and unused_topic:
            line = f"{unused_topic.group(1)}dvhLastResultTopic: IKKE_I_BRUK {AUTO}"
        match = re.search(r"\borg\.(?:nom\.[\w.-]+|nav-ident(?:-rnr)?)\b", line)
        if match:
            topic = match.group(0).removeprefix("org.")
            if module == "admin" and topic in ADMIN_INACTIVE_TOPICS:
                line = changed(line, match.group(0), "IKKE_I_BRUK")
            elif topic in TOPICS:
                line = changed(line, match.group(0), f"{match.group(0)}-{suffix}")
            else:
                line = marked(line, f"topic {topic} mangler i variantens topic-fil")
        if module == "paarorende" and "http://nom-api.nom" in line:
            line = changed(line, "http://nom-api.nom", f"http://nom-api-{suffix}.nom")
        if module == "api" and re.match(r"^\s+ressurs-endringer:\s*", line):
            bucket = re.match(r"^(\s+ressurs-endringer:\s*)([\w-]+)(.*)$", line)
            if bucket:
                line = changed(line, bucket.group(2), f"{bucket.group(2)}-{suffix}")
        if module == "paarorende" and re.match(r"^  name: nom-paarorende\s*$", line):
            line = changed(line, "nom-paarorende", f"nom-paarorende-{suffix}")
        if module == "nom-sf-integrasjon" and "group-id: nom-sf-integrasjon-evtsrc" in line:
            line = changed(line, "nom-sf-integrasjon-evtsrc", f"nom-sf-integrasjon-{suffix}-evtsrc")
        if module == "nav-ident" and "sequence:" in line:
            line = marked(line, "sekvensområde for NAV-ident må være unikt")
        result.append(line)
    if module == "admin":
        at = result.index("  kafka:") + 1
        result.insert(at, "    skru-av-lyttere-for-ubrukte-topics-i-ekstra-dev-miljo: true # Inaktive topics: "
                      + ", ".join(sorted(ADMIN_INACTIVE_TOPICS)))
    if module == "kafka-api/api-ressurs-state-v4":
        result += [marked("    skip-producing-state-events-until-ressurs-evtsrc-offset: 0", "offset i nytt topic"),
                   marked("    skip-producing-state-events-until-orgtilknytning-evtsrc-offset: 0", "offset i nytt topic")]
    # application-dev.yaml is already active via "dev". Override its app settings
    # in a suffix profile; never silently retain unsafe dev offsets or client IDs.
    dev_app = section(dev, "app")
    if dev_app:
        app_active = False
        reviewed = []
        for line in dev_app.splitlines():
            if line == "app:":
                app_active = True
            elif re.match(r"^[\w.-]+:", line) and app_active:
                app_active = False
            if not app_active:
                continue
            match = re.search(r"\borg\.(?:nom\.[\w.-]+|nav-ident(?:-rnr)?)\b", line)
            if match:
                topic = match.group(0).removeprefix("org.")
                line = changed(line, match.group(0), f"{match.group(0)}-{suffix}") if topic in TOPICS else marked(line, f"topic {topic} mangler")
            if re.match(r"^\s+[\w.-]+:\s*(?:no-nav-[\w-]+|org-nom-[\w-]+)\s*(?:#.*)?$", line):
                bucket = re.match(r"^\s+[\w.-]+:\s*([\w-]+)", line).group(1)
                line = changed(line, bucket, f"{bucket}-{suffix}")
            if re.match(r"^\s*(?:- )?[\w.-]+:", line) and re.search(r"(?:offset|client-id|group-id|bucket|enabled|^\s+navident:)", line, re.I):
                line = marked(line, "DEV-verdi eller tilgang må godkjennes for varianten")
            reviewed.append(line)
        if "app:" not in result:
            result = reviewed
        else:
            base_app = result.index("app:")
            result[base_app + 1:] = merge_mapping(result[base_app + 1:], reviewed[1:], 2)
    if dev:
        result += [""]
        for line in dev.splitlines():
            if not line or line.startswith("#") or line[0].isspace() or not re.match(r"^[\w.-]+:", line):
                continue
            if line.startswith("REDIS_HOST:"):
                result.append(changed(line, "nav-ident-redis.org", f"nav-ident-redis-{suffix}.org"))
            else:
                review = bool(re.search(r"(?:SCOPE|BASEURL|BASE_URL|GROUPID|CLIENT_ID)", line))
                if review:
                    for peer in sorted(NAMES, key=len, reverse=True):
                        line = re.sub(r"(?<![\w-])" + re.escape(peer) + r"(?=(?:\.(?:nom|org)\.svc|/\.default))",
                                      lambda m: f"{m.group(0)}-{suffix}", line)
                url_changed = False
                for peer in sorted(NAMES - {"nav-ident-redis"}, key=len, reverse=True):
                    namespace = "nom" if peer == "nom-api" else "org"
                    host = re.compile(
                        r"(?<![\w.-])http://" + re.escape(peer) + r"\." + namespace
                        + r"(?=:\d+(?:/|[\"'\s#]|$)|(?:/|[\"'\s#]|$))"
                    )
                    line, count = host.subn(lambda _: f"http://{peer}-{suffix}.{namespace}", line)
                    url_changed = url_changed or count > 0
                if review or url_changed:
                    result.append(marked(line, "scope, delt tjeneste eller klienttilgang må godkjennes"))
    app_names = {app_module: app_name for app_name, (_, _, app_module, _) in APPS.items() if app_module}
    if not stream and module != "paarorende":
        expected_name = app_names[module]
        app_section = section("\n".join(result), "app")
        name_lines = [line for line in app_section.splitlines() if re.match(r"^  name:", line)]
        if len(name_lines) != 1:
            raise ValueError(f"Expected exactly one app.name for {module}")
        match = re.fullmatch(r"  name: \$\{NAIS_APP_NAME:([a-z0-9-]+)\}(?:\s+#.*)?", name_lines[0])
        if not match or match.group(1) != expected_name:
            raise ValueError(f"Unexpected app.name fallback for {module}: {name_lines[0]}")
        old = f"${{NAIS_APP_NAME:{expected_name}}}"
        new = f"${{NAIS_APP_NAME:{expected_name}-{suffix}}}"
        result[result.index(name_lines[0])] = changed(name_lines[0], old, new)
    if stream:
        result += ["", "spring:", "  kafka:", "    streams:",
                   "      application-id: ${KAFKA_STREAMS_APPLICATION_ID}"]
    overrides = state_overrides(root, module, suffix)
    if overrides:
        result += ["", *overrides]
    return "\n".join(result).rstrip() + "\n"


def topic_documents(root: Path, suffix: str) -> str:
    sources = [root / ".nais/topics" / p for p in ("topics-internal.yaml", "nav-ident-topics.yaml", "topics-kafka-api.yaml")]
    templates = {}
    for source in sources:
        for doc in re.split(r"(?m)^---\s*$", source.read_text()):
            found = re.search(r"(?m)^  name: ([\w.-]+)\s*$", doc)
            if found:
                if found.group(1) in templates:
                    raise ValueError(f"Duplicate topic template: {found.group(1)}")
                templates[found.group(1)] = doc
    missing = set(TOPICS) - templates.keys()
    if missing:
        raise ValueError(f"Missing topic templates: {sorted(missing)}")
    results = []
    for name in TOPICS:
        source = templates[name]
        acl = re.search(r"(?m)^  acl:\s*$", source)
        if not acl:
            raise ValueError(f"Missing ACL for {name}")
        document = source[:acl.start()].rstrip()
        document, replacements = re.subn(r"(?m)^  name: " + re.escape(name) + r"$",
                                          f"  name: {name}-{suffix} {AUTO}", document)
        if replacements != 1:
            raise ValueError(f"Unsafe topic name rewrite for {name}: {replacements} matches")
        acl_lines = []
        entries = re.findall(r"(?ms)^    - team: ([\w-]+)\s*\n      application: ([\w-]+)\s*\n      access: ([\w-]+)", source[acl.end():])
        for team, app, access in entries:
            if app in NAMES and team == ("nom" if app == "nom-api" else "org"):
                acl_lines += [f"    - team: {team}", f"      application: {app}-{suffix} {AUTO}", f"      access: {access}"]
        if not acl_lines:
            raise ValueError(f"No variant ACL for {name}")
        results.append(document + "\n  acl:\n" + "\n".join(acl_lines) + "\n"
                       + marked("  topic_policy: true", "retention, opphav og ACL må godkjennes") + "\n")
    return "---\n".join(results)


def workflow(text: str, manifest_path: str, suffix: str, web: bool, path_filter: str) -> str:
    entry = {"suffix": suffix, "manifest": manifest_path}
    if web:
        pattern = r"(?m)^(\s*ADDITIONAL_DEV_DEPLOYMENTS: )'([^']*)'$"
    else:
        pattern = r"(?m)^(\s*)nais-config-file-path-dev: ([^\n]+)$"
    if web:
        match = re.search(pattern, text)
        if not match:
            raise ValueError("Missing web variant input")
        entries = json.loads(match.group(2))
        if not isinstance(entries, list):
            raise ValueError("Invalid web variant input")
        if any(item.get("suffix") == suffix for item in entries):
            raise ValueError(f"Variant {suffix} already listed in workflow")
        entries.append(entry)
        text = text[:match.start()] + match.group(1) + "'" + json.dumps(entries, separators=(",", ":")) + "'" + text[match.end():]
    else:
        existing = re.search(r"(?m)^\s+additional-dev-deployments: '([^']*)'$", text)
        if existing:
            entries = json.loads(existing.group(1))
            if not isinstance(entries, list):
                raise ValueError("Invalid Spring variant input")
            if any(item.get("suffix") == suffix for item in entries):
                raise ValueError(f"Variant {suffix} already listed in workflow")
            entries.append(entry)
            text = text[:existing.start()] + "      additional-dev-deployments: '" + json.dumps(entries, separators=(",", ":")) + "'" + text[existing.end():]
        else:
            match = re.search(pattern, text)
            if not match:
                raise ValueError("Missing Spring dev manifest input")
            text = text[:match.end()] + "\n      additional-dev-deployments: '" + json.dumps([entry], separators=(",", ":")) + "'" + text[match.end():]
    # Trigger even when the only changed file is a new variant manifest or this workflow.
    if "      - '" + path_filter + "'" not in text and "    paths:\n" in text:
        text = text.replace("    paths:\n", "    paths:\n      - '" + path_filter + "'\n", 1)
    return text


def topic_action(root: Path, action: str) -> str:
    """Use the same pinned action as the existing org topic deployment."""
    workflow = (root / ".github/workflows/kafka-topics-org.yaml").read_text()
    matches = re.findall(r"(?m)^\s*- uses: (" + re.escape(action) + r"@[a-f0-9]{40}(?:\s+#.*)?)$", workflow)
    if not matches or len(set(matches)) != 1:
        raise ValueError(f"Mangler entydig SHA for {action} i kafka-topics-org.yaml")
    return matches[0]


def topic_workflow(root: Path, suffix: str) -> str:
    checkout = topic_action(root, "actions/checkout")
    deploy = topic_action(root, "nais/deploy/actions/deploy")
    return f"""name: 'update-kafka-topics: additional-dev-{suffix}'
on:
  push:
    branches:
      - 'main'
      - 'dev/*'
    paths:
      - '.nais/topics/topics-additional-dev-{suffix}.yaml'
      - '.github/workflows/kafka-topics-additional-dev-{suffix}.yaml'
  workflow_dispatch:

jobs:
  deploy-kafka-topics-dev:
    name: Deploy to Aiven Dev
    permissions:
      contents: read
      id-token: write
    runs-on: ubuntu-latest
    timeout-minutes: 20
    steps:
      - uses: {checkout}
      - uses: {deploy}
        env:
          CLUSTER: dev-gcp
          VAR: kafka-pool=nav-dev
          RESOURCE: .nais/topics/topics-additional-dev-{suffix}.yaml
"""


def generate(root: Path, suffix: str) -> list[Path]:
    if not re.fullmatch(r"[a-z0-9]+(?:-[a-z0-9]+)*", suffix):
        raise ValueError("Suffix must consist of lowercase letters, numbers and internal hyphens")
    if any(len(f"{name}-{suffix}") > 63 for name in NAMES):
        raise ValueError("Variant Application name exceeds 63 characters")
    writes: dict[Path, str] = {}
    for name, (directory, base, module, deploy) in APPS.items():
        path = Path(".nais/apps") / directory / base
        target = variant_manifest_path(path, suffix)
        writes[target] = source_header(root, [path]) + manifest((root / path).read_text(), suffix, name)
        if module:
            app_path = Path("apps") / module / "src/main/resources" / f"application-{suffix}.yaml"
            sources = [Path("apps") / module / "src/main/resources/application.yaml"]
            dev = sources[0].with_name("application-dev.yaml")
            if (root / dev).exists():
                sources.append(dev)
            writes[app_path] = source_header(root, sources) + profile(root, module, suffix)
        flow = Path(".github/workflows") / deploy
        original = writes.get(flow, (root / flow).read_text())
        writes[flow] = workflow(original, target.as_posix(), suffix, name in {"nom-ui2", "nom-admin-web"}, target.as_posix())
    for name, (module, base, deploy) in STREAMS.items():
        path = Path(".nais/streams") / base
        target = variant_manifest_path(path, suffix)
        writes[target] = source_header(root, [path]) + manifest((root / path).read_text(), suffix, name)
        app_path = Path("apps/streams") / module / "src/main/resources" / f"application-{suffix}.yaml"
        writes[app_path] = source_header(root, [Path("apps/streams") / module / "src/main/resources/application.yaml"]) + profile(root, module, suffix, stream=True)
        flow = Path(".github/workflows") / deploy
        original = writes.get(flow, (root / flow).read_text())
        writes[flow] = workflow(original, target.as_posix(), suffix, False, target.as_posix())
    topics = Path(".nais/topics") / f"topics-additional-dev-{suffix}.yaml"
    writes[topics] = topic_documents(root, suffix)
    topic_flow = Path(".github/workflows") / f"kafka-topics-additional-dev-{suffix}.yaml"
    writes[topic_flow] = topic_workflow(root, suffix)
    for path, app, namespace in SHARED_DEV_CALLERS:
        writes[path] = add_shared_dev_caller((root / path).read_text(), app, namespace, suffix)
    # Refuse any existing generated target and any repository with missing sources.
    shared_paths = {path for path, _, _ in SHARED_DEV_CALLERS}
    for relative in writes:
        target = root / relative
        if not target.parent.is_dir():
            raise ValueError(f"Parent directory does not exist: {target.parent}")
        if target.exists() and relative not in shared_paths | {Path('.github/workflows') / v[-1] for v in APPS.values()} | {Path('.github/workflows') / v[-1] for v in STREAMS.values()}:
            raise FileExistsError(target)
    for relative, content in writes.items():
        if relative.parts[0] == ".nais" and relative.parts[1] == "apps":
            for bucket in re.findall(r"(?m)^      - name: ([\w-]+)\s+# Auto-utfylt", content):
                if len(bucket) > 63:
                    raise ValueError(f"Generated SQL instance or bucket name exceeds 63 characters: {bucket}")
    # Existing workflows are updated only after every new target has passed preflight.
    for relative, content in writes.items():
        (root / relative).write_text(content)
    return list(writes)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("suffix", help="lowercase letters, digits and internal hyphens")
    args = parser.parse_args()
    try:
        paths = generate(ROOT, args.suffix)
        print(f"Opprettet/oppdatert {len(paths)} filer. Finn linjer som starter med @ og kontroller alt før commit/push.")
        print("Les scripts/README.md: https://github.com/navikt/nom/blob/main/scripts/README.md")
    except (ValueError, OSError) as error:
        parser.exit(1, f"Ingen filer skal deployes før manuell gjennomgang: {error}\n")
