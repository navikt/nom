"""Run: /usr/bin/python3 -B -m unittest discover -s scripts/test -p 'test_*.py'"""

import json
import os
import re
import shutil
import subprocess
import tempfile
import textwrap
import unittest
from pathlib import Path

from scripts import create_additional_dev_deployment as generator


class GenerateTests(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        for folder in (".nais/apps", ".nais/streams", ".nais/topics", ".github/workflows", "apps"):
            (self.root / folder).mkdir(parents=True)
        sources = []
        for _, (directory, manifest, module, flow) in generator.APPS.items():
            sources += [Path(".nais/apps") / directory / manifest, Path(".github/workflows") / flow]
            if module:
                resources = Path("apps") / module / "src/main/resources"
                sources.append(resources / "application.yaml")
                for profile in ("application-dev.yaml", "application-prod.yaml"):
                    profile_path = resources / profile
                    if (generator.ROOT / profile_path).exists():
                        sources.append(profile_path)
        for _, (module, manifest, flow) in generator.STREAMS.items():
            sources += [Path(".nais/streams") / manifest, Path(".github/workflows") / flow,
                        Path("apps/streams") / module / "src/main/resources/application.yaml"]
        sources += [Path(".nais/topics") / name for name in
                    ("topics-internal.yaml", "nav-ident-topics.yaml", "topics-kafka-api.yaml")]
        sources += list(Path("libs/nom-state/src/main/resources") / f"{kind}-state.yaml"
                        for kind in generator.STATE_TYPES)
        sources.extend((Path(".github/workflows/deploy-spring-app.yaml"),
                        Path(".github/workflows/kafka-topics-org.yaml")))
        sources.extend(path for path, _, _ in generator.SHARED_DEV_CALLERS)
        sources.extend((Path(".nais/apps/nom-azure/nom-azure-prod-gcp.yaml"),
                        Path(".nais/apps/proxy/proxy-prod-fss.yaml")))
        for source in set(sources):
            dest = self.root / source
            dest.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(generator.ROOT / source, dest)

    def test_generate_isolated_variant(self):
        base_profiles = {
            path: (self.root / path).read_text()
            for path in self.root.glob("apps/admin/src/main/resources/application*.yaml")
        }
        files = generator.generate(self.root, "q9")
        self.assertEqual(len(files), 15 + 13 + 1 + 1 + 15 + 2)
        self.assertTrue(all((self.root / path).is_file() for path in files))
        for path in files:
            if path.parts[0] == ".github":
                continue
            if path.parts[0] == ".nais" and path.parts[1] == "topics":
                continue
            if path in {source for source, _, _ in generator.SHARED_DEV_CALLERS}:
                continue
            content = (self.root / path).read_text()
            for provenance in re.findall(r"(?m)^# Kopiert fra (\S+) \(commit: ([0-9a-f]{40})\)$", content):
                self.assertEqual(
                    provenance[1],
                    subprocess.check_output(
                        ["git", "-C", str(generator.ROOT), "log", "-1", "--format=%H", "--", provenance[0]],
                        text=True,
                    ).strip(),
                )
            self.assertRegex(content, r"^# Kopiert fra ")
        topics = (self.root / ".nais/topics/topics-additional-dev-q9.yaml").read_text()
        self.assertEqual(len(re.findall(r"(?m)^kind: Topic$", topics)), 16)
        for topic in generator.TOPICS:
            self.assertIn(f"name: {topic}-q9", topics)
        for name in generator.NAMES:
            self.assertNotRegex(topics, rf"(?m)^\s+application: {re.escape(name)}(?:\s|$)")
        self.assertNotIn("application: org-kafka-manager", topics)
        self.assertIn("application: nom-api-q9", topics)

        ident = (self.root / ".nais/apps/nav-ident/nav-ident-q9-dev-gcp.yaml").read_text()
        self.assertIn("name: nav-ident-redis-q9", ident)
        self.assertIn("application: nav-ident-q9", ident)
        self.assertIn("application: org-token-tool", ident)
        self.assertIn("value: dev,q9", ident)
        self.assertIn("- name: nav-ident-q9-db", ident)
        self.assertIn("- secret: nav-ident-unleash-api-token", ident)

        api = (self.root / "apps/api/src/main/resources/application-q9.yaml").read_text()
        self.assertIn("# Kopiert fra apps/api/src/main/resources/application.yaml", api)
        self.assertIn("# Kopiert fra apps/api/src/main/resources/application-dev.yaml", api)
        self.assertNotIn("on-profile:", api)
        self.assertNotIn("\n---\n", api)
        self.assertIn('http://nav-ident-q9.org', api)
        self.assertIn('api://dev-gcp.org.nav-ident-q9/.default', api)
        self.assertIn('http://nom-paarorende-q9.org', api)
        self.assertIn('api://dev-gcp.org.nom-paarorende-q9/.default', api)
        self.assertIn("@    offsets: # KONTROLLER MANUELT", api)
        self.assertIn("      ressurs: 865000", api)
        self.assertIn("org.nom.ressurs-eventsource-q9", api)
        self.assertIn("      dvhLastResultTopic: IKKE_I_BRUK # Auto-utfylt av 'create_additional_dev_deployment.py'", api)
        self.assertNotIn("@      dvhLastResultTopic:", api)
        self.assertIn("ressurs-endringer: no-nav-nom-dev-ressurs-syntetisering-storage-bucket-q9", api)
        self.assertIn("org.nav-ident-q9", api)
        self.assertNotIn("# Kopiert fra libs/nom-state/", api)
        for module in ("api", "admin", "nom-sf-integrasjon", "nom-rdbms-data"):
            profile = (self.root / "apps" / module / "src/main/resources/application-q9.yaml").read_text()
            self.assertNotIn("# Kopiert fra libs/nom-state/", profile)
            source_paths = generator.state_sources(self.root, module)
            self.assertEqual(len(re.findall(r"(?m)^nom\.libs\.state\.[\w-]+\.state-topic:", profile)), len(source_paths))
            for source in source_paths:
                kind = source.name.removesuffix("-state.yaml")
                default = re.search(r"(?m)^\s+state-topic:\s*(org\.[\w.-]+)", (self.root / source).read_text()).group(1)
                self.assertIn(f"nom.libs.state.{kind}.state-topic: {default}-q9 {generator.AUTO}", profile)
                self.assertNotRegex(profile, rf"(?m)^nom\.libs\.state\.{re.escape(kind)}\.state-topic: {re.escape(default)}(?:\s|$)")
        self.assertFalse(any(path.as_posix().startswith("apps/nom-remedy-integrasjon/") for path in files))
        web = (self.root / ".nais/apps/ui2/ui2-q9-dev.yaml").read_text()
        self.assertIn("https://nom-q9.ansatt.dev.nav.no", web)
        self.assertIn("http://nom-api-q9.nom # Auto-utfylt", web)
        self.assertIn("api://dev-gcp.nom.nom-api-q9/.default", web)
        self.assertIn("api://dev-gcp.nom.nom-api-q9/.default # Auto-utfylt", web)
        self.assertIn("- secret:", web)
        admin_web = (self.root / ".nais/apps/admin-web/admin-web-q9-dev.yaml").read_text()
        self.assertIn("  accessPolicy:\n    outbound:\n", admin_web)
        self.assertNotIn("    inbound:", admin_web)
        self.assertIn("application: org-token-tool", web)
        stream = (self.root / ".nais/streams/kafka-api-ressurs-state-v4-q9-dev.yaml").read_text()
        self.assertIn("name: nom-kafka-api-ressurs-state-v4-q9", stream)
        self.assertIn("value: dev,q9", stream)
        self.assertNotIn("accessPolicy:", stream)
        stream_profile = (self.root / "apps/streams/kafka-api/api-ressurs-state-v4/src/main/resources/application-q9.yaml").read_text()
        self.assertIn("@    skip-producing-state-events-until-ressurs-evtsrc-offset: 0 # KONTROLLER MANUELT", stream_profile)
        self.assertNotIn("on-profile:", stream_profile)
        expected_application_id = "      application-id: ${KAFKA_STREAMS_APPLICATION_ID}"
        for name, (module, _, _) in generator.STREAMS.items():
            content = (self.root / "apps/streams" / module / "src/main/resources/application-q9.yaml").read_text()
            self.assertNotIn("on-profile:", content, name)
            self.assertNotIn("\n---\n", content, name)
            self.assertNotRegex(content, r"(?m)^\s+[\w-]+: org\.nom\.[\w.-]+(?<!-q9)$")
            self.assertEqual(content.splitlines().count(expected_application_id), 1, name)
            self.assertNotRegex(content, r"(?m)^      application-id: \$\{KAFKA_STREAMS_APPLICATION_ID\}-")

        rdbms = (self.root / ".nais/apps/nom-rdbms-data/nom-rdbms-data-q9-dev-gcp.yaml").read_text()
        self.assertIn("- name: nom-rdbms-data-q9", rdbms)
        self.assertIn("- name: nom-rdbms-data-q9-db", rdbms)
        self.assertNotIn("@", rdbms)
        self.assertNotRegex(rdbms, r"(?m)^\s+cpu: 1000m$")
        paarorende = (self.root / "apps/paarorende/src/main/resources/application-q9.yaml").read_text()
        self.assertIn("http://nom-api-q9.nom", paarorende)
        self.assertIn("name: nom-paarorende-q9", paarorende)
        for app, (_, _, module, _) in generator.APPS.items():
            if module and module != "paarorende":
                profile = (self.root / "apps" / module / "src/main/resources/application-q9.yaml").read_text()
                self.assertIn(f"name: ${{NAIS_APP_NAME:{app}-q9}}", profile, module)
                self.assertIn("${NAIS_APP_NAME:", profile, module)
        admin = (self.root / ".nais/apps/admin/admin-q9-dev-gcp.yaml").read_text()
        self.assertIn("value: http://nom-api-q9.nom", admin)
        self.assertIn("api://dev-gcp.nom.nom-api-q9/.default # Auto-utfylt", admin)
        self.assertRegex(admin, r"@      memory: \d+Gi # KONTROLLER MANUELT")
        self.assertIn("@      memory: 20Gi # KONTROLLER MANUELT", admin)
        self.assertIn("- secret: nom-admin-unleash-api-token", admin)
        self.assertNotIn("- host: nom-remedy-integrasjon.dev-fss-pub.nais.io", admin)
        self.assertIn("- host: org-unleash-api.nav.cloud.nais.io", admin)
        for app, (directory, manifest, _, _) in generator.APPS.items():
            path = generator.variant_manifest_path(Path(".nais/apps") / directory / manifest, "q9")
            variant = (self.root / path).read_text()
            self.assertNotIn("- host: nom-remedy-integrasjon.dev-fss-pub.nais.io", variant, app)
        admin_profile = (self.root / "apps/admin/src/main/resources/application-q9.yaml").read_text()
        inactive_topics = sorted(generator.ADMIN_INACTIVE_TOPICS)
        inactive_properties = {
            "nom.api-hendelser-v1": "apiEventTopic",
            "nom.api-ressurs-hendelse-v1": "apiEventTopic",
            "nom.intern-orgtilknytning-state-dlt": "orgtilknytning-topic",
            "nom.intern-ressurs-state-dlt": "ressurs-topic",
        }
        switch_lines = re.findall(
            r"(?m)^\s+skru-av-lyttere-for-ubrukte-topics-i-ekstra-dev-miljo: true # Inaktive topics: (.+)$",
            admin_profile,
        )
        self.assertEqual(switch_lines, [", ".join(inactive_topics)])
        self.assertEqual(admin_profile.count("skru-av-lyttere-for-ubrukte-topics-i-ekstra-dev-miljo"), 1)
        self.assertNotIn("on-profile:", admin_profile)
        self.assertNotIn("\n---\n", admin_profile)
        for topic in inactive_topics:
            self.assertIn(topic, switch_lines[0])
        for property_name in inactive_properties.values():
            matches = re.findall(rf"(?m)^\s+{re.escape(property_name)}:\s*(\S+)", admin_profile)
            self.assertIn("IKKE_I_BRUK", matches)
        for property_name in ("apiEventTopic", "orgtilknytning-topic", "ressurs-topic"):
            matches = re.findall(rf"(?m)^\s+{re.escape(property_name)}:\s*(.*)$", admin_profile)
            self.assertTrue(matches)
            for value in matches:
                if "IKKE_I_BRUK" in value:
                    self.assertNotRegex(value, r"(?:org\.)?nom\.")
                    self.assertNotIn("@", value)
        variant_profiles = [path for path in files if path.name == "application-q9.yaml"]
        self.assertEqual(
            [path for path in variant_profiles if "skru-av-lyttere-for-ubrukte-topics-i-ekstra-dev-miljo" in (self.root / path).read_text()],
            [Path("apps/admin/src/main/resources/application-q9.yaml")],
        )
        for path, original in base_profiles.items():
            self.assertEqual((self.root / path).read_text(), original)

        listener_files = (
            "apps/admin/src/main/java/no/nav/nom/apps/admin/events/hendelser/HendelserApiStateListener.java",
            "apps/admin/src/main/java/no/nav/nom/apps/admin/events/ressurs/api/RessursHendelseApiListener.java",
            "apps/admin/src/main/java/no/nav/nom/apps/admin/events/orgtilknytning/deadletter/DeadLetterListener.java",
        )
        auto_startup = 'autoStartup = "#{!${app.kafka.skru-av-lyttere-for-ubrukte-topics-i-ekstra-dev-miljo:false}}"'
        for listener in listener_files:
            source = (generator.ROOT / listener).read_text()
            self.assertEqual(source.count("@KafkaListener("), 1, listener)
            self.assertIn(auto_startup, source, listener)
        sf = (self.root / ".nais/apps/nom-sf-integrasjon/nom-sf-integrasjon-q9-dev-gcp.yaml").read_text()
        self.assertIn("value: http://nom-api-q9.nom", sf)
        self.assertIn("api://dev-gcp.nom.nom-api-q9/.default # Auto-utfylt", sf)
        sf_profile = (self.root / "apps/nom-sf-integrasjon/src/main/resources/application-q9.yaml").read_text()
        self.assertIn("http://nav-ident-q9.org", sf_profile)
        self.assertIn("api://dev-gcp.org.nav-ident-q9/.default", sf_profile)
        for event_source, topic in (
            ("orgenhet", "orgenhet-eventsource"),
            ("orgenhet-leder", "orgenhet-leder-eventsource"),
            ("organisering", "organisering-eventsource"),
        ):
            self.assertIn(
                f"      {event_source}:\n        topic: org.nom.{topic}-q9",
                sf_profile,
            )
        self.assertIn("application: nav-ident-q9", sf)
        self.assertIn("- name: nom-sf-integrasjon-q9-db", sf)
        self.assertIn("      external:\n        - host: org-unleash-api.nav.cloud.nais.io", sf)
        self.assertIn("@        - host: api55.sapsf.eu", sf)
        self.assertIn("application: org-ekstern-proxy", sf)
        self.assertIn("application: org-token-tool", sf)
        api_manifest = (self.root / ".nais/apps/api/api-q9-dev-gcp.yaml").read_text()
        self.assertIn("application: nav-ident-q9", api_manifest)
        self.assertIn("application: nom-paarorende-q9", api_manifest)
        for app in ("logging", "norg2", "orgenhet-kostnadssted-mapping-api"):
            self.assertIn(f"application: {app}\n", api_manifest)
        for manifest_path, caller, namespace in generator.SHARED_DEV_CALLERS:
            shared = (self.root / manifest_path).read_text()
            self.assertIn(f"application: {caller}-q9 # Auto-utfylt av", shared)
            self.assertIn(f"application: {caller}\n          namespace: {namespace}\n          cluster: dev-gcp", shared)
            self.assertEqual(shared.count(f"application: {caller}-q9 # Auto-utfylt"), 1)

        workflow = (self.root / ".github/workflows/deploy-api.yaml").read_text()
        match = re.search(r"(?m)^\s+additional-dev-deployments: '([^']+)'", workflow)
        self.assertEqual(json.loads(match.group(1))[-1], {"suffix": "q9", "manifest": ".nais/apps/api/api-q9-dev-gcp.yaml"})
        self.assertIn("- '.nais/apps/api/api-q9-dev-gcp.yaml'", workflow)
        webflow = (self.root / ".github/workflows/deploy-ui2.yaml").read_text()
        self.assertIn('"suffix":"q9"', webflow)
        self.assertIn("- '.nais/apps/ui2/ui2-q9-dev.yaml'", webflow)
        topicflow = (self.root / ".github/workflows/kafka-topics-additional-dev-q9.yaml").read_text()
        self.assertIn("CLUSTER: dev-gcp", topicflow)
        self.assertNotIn("prod-gcp", topicflow)
        self.assertNotIn("nav-prod", topicflow)
        for action in ("actions/checkout", "nais/deploy/actions/deploy"):
            self.assertIn(f"- uses: {generator.topic_action(self.root, action)}", topicflow)


        # A second variant extends the matrices instead of replacing the previous one.
        generator.generate(self.root, "q8")
        for manifest_path, caller, namespace in generator.SHARED_DEV_CALLERS:
            shared = (self.root / manifest_path).read_text()
            for variant in ("q9", "q8"):
                self.assertEqual(shared.count(f"application: {caller}-{variant} # Auto-utfylt"), 1)
                self.assertIn(f"application: {caller}-{variant} {generator.AUTO}\n"
                              f"          namespace: {namespace}\n          cluster: dev-gcp", shared)
        for directory in ("nom-azure", "proxy"):
            production = ("nom-azure-prod-gcp.yaml" if directory == "nom-azure" else "proxy-prod-fss.yaml")
            path = Path(".nais/apps") / directory / production
            self.assertEqual((self.root / path).read_bytes(), (generator.ROOT / path).read_bytes())
        workflow = (self.root / ".github/workflows/deploy-api.yaml").read_text()
        match = re.search(r"(?m)^\s+additional-dev-deployments: '([^']+)'", workflow)
        self.assertEqual([item["suffix"] for item in json.loads(match.group(1))][-2:], ["q9", "q8"])

        ruby = shutil.which("ruby")
        if ruby:
            for path in files:
                if not path.as_posix().endswith((".yaml", ".yml")) or path.parts[0] == ".github":
                    continue
                contents = (self.root / path).read_text()
                if not re.search(r"(?m)^@", contents):
                    continue
                result = subprocess.run([ruby, "-e", "require 'yaml'; YAML.parse_stream(File.read(ARGV.fetch(0)))", str(self.root / path)],
                                        capture_output=True, text=True)
                self.assertNotEqual(result.returncode, 0, str(path))

    def test_changed_topic_template_fails_before_writes(self):
        topic = self.root / ".nais/topics/nav-ident-topics.yaml"
        topic.write_text(topic.read_text().replace("  name: nav-ident\n", "  name: nav-ident # new comment\n"))
        original = (self.root / ".github/workflows/deploy-api.yaml").read_text()
        with self.assertRaises(ValueError):
            generator.generate(self.root, "q9")
        self.assertEqual(original, (self.root / ".github/workflows/deploy-api.yaml").read_text())
        self.assertFalse((self.root / ".nais/topics/topics-additional-dev-q9.yaml").exists())

    def test_topic_workflow_uses_current_pinned_actions(self):
        path = self.root / ".github/workflows/kafka-topics-org.yaml"
        original = path.read_text()
        checkout = "f" * 40
        deploy = "e" * 40
        updated = re.sub(r"actions/checkout@[a-f0-9]{40}", f"actions/checkout@{checkout}", original)
        updated = re.sub(r"nais/deploy/actions/deploy@[a-f0-9]{40}",
                         f"nais/deploy/actions/deploy@{deploy}", updated)
        path.write_text(updated)
        generated = generator.topic_workflow(self.root, "q9")
        self.assertIn(f"actions/checkout@{checkout}", generated)
        self.assertIn(f"nais/deploy/actions/deploy@{deploy}", generated)
        path.write_text(updated.replace(f"nais/deploy/actions/deploy@{deploy}",
                                        "nais/deploy/actions/deploy@v3"))
        with self.assertRaisesRegex(ValueError, "Mangler entydig SHA"):
            generator.topic_workflow(self.root, "q9")

    def test_remedy_only_external_access_is_not_emitted(self):
        source = """apiVersion: nais.io/v1alpha1
kind: Application
metadata:
  name: nom-admin
spec:
  accessPolicy:
    outbound:
      external:
        - host: nom-remedy-integrasjon.dev-fss-pub.nais.io
"""
        generated = generator.manifest(source, "q9", "nom-admin")
        self.assertNotIn("nom-remedy-integrasjon.dev-fss-pub.nais.io", generated)
        self.assertNotIn("outbound:", generated)

    def test_unexpected_app_name_fallback_fails_closed(self):
        source = self.root / "apps/nom-sf-integrasjon/src/main/resources/application.yaml"
        source.write_text(source.read_text().replace(
            "${NAIS_APP_NAME:nom-sf-integrasjon}", "${NAIS_APP_NAME:nom-api}", 1
        ))
        with self.assertRaisesRegex(ValueError, "Unexpected app.name fallback for nom-sf-integrasjon"):
            generator.profile(self.root, "nom-sf-integrasjon", "sf-test")

    def test_dev_http_service_urls_only_suffix_known_variant_hosts(self):
        source = self.root / "apps/api/src/main/resources/application-dev.yaml"
        entries = textwrap.dedent('''
            CUSTOM_ENDPOINT_URL: "http://nav-ident.org:8080/path"
            URL_WITH_SPACE: http://nom-api.nom
            URL_AT_END: http://nav-ident.org
            ALREADY_BASEURL: "http://nav-ident-q9.org"
            EXTERNAL_BASEURL: "https://nav-ident.org"
            SHARED_BASEURL: "http://orgenhet-kostnadssted-mapping-api.team-nof"
            HOST_PREFIX_BASEURL: "http://nav-ident.org.example"
        ''').replace("URL_WITH_SPACE: http://nom-api.nom\n", "URL_WITH_SPACE: http://nom-api.nom \n")
        source.write_text(source.read_text() + entries)

        generated = generator.profile(self.root, "api", "q9")

        self.assertIn('http://nav-ident-q9.org:8080/path', generated)
        self.assertIn('http://nom-api-q9.nom', generated)
        already_suffixed = re.search(r'(?m)^@?ALREADY_BASEURL: "([^\"]+)"', generated)
        self.assertIsNotNone(already_suffixed)
        self.assertEqual(already_suffixed.group(1), "http://nav-ident-q9.org")
        self.assertNotIn('http://nav-ident-q9-q9.org', generated)
        self.assertIn('https://nav-ident.org', generated)
        self.assertIn('http://orgenhet-kostnadssted-mapping-api.team-nof', generated)
        self.assertIn('http://nav-ident.org.example', generated)

    def test_state_overrides_fail_closed(self):
        state_dir = self.root / "libs/nom-state/src/main/resources"
        resource = state_dir / "ressurs-state.yaml"
        original = resource.read_text()
        resource.write_text(original.replace("org.nom.intern-ressurs-state", "org.nom.ikke-definert"))
        with self.assertRaisesRegex(ValueError, "Mangler kjent state-topic"):
            generator.state_overrides(self.root, "api", "q9")
        resource.write_text(original.replace("        state-topic: org.nom.intern-ressurs-state", "        listener-id: mangler-topic"))
        with self.assertRaisesRegex(ValueError, "Mangler kjent state-topic"):
            generator.state_overrides(self.root, "api", "q9")
        resource.write_text(original)

        topics = self.root / ".nais/topics/topics-internal.yaml"
        original_topics = topics.read_text()
        resource_topic = "nom.intern-ressurs-state"
        start = original_topics.index("  name: " + resource_topic)
        end = original_topics.find("\n---\n", start)
        end = len(original_topics) if end < 0 else end
        topic_block = original_topics[start:end]
        self.assertIn("application: nom-api\n      access: read", topic_block)
        without_api = re.sub(r"(?m)^    - team: nom\n      application: nom-api\n      access: read\n", "", topic_block)
        topics.write_text(original_topics[:start] + without_api + original_topics[end:])
        with self.assertRaisesRegex(ValueError, "Mangler lese-ACL"):
            generator.state_overrides(self.root, "api", "q9")
        topics.write_text(original_topics[:start] + topic_block.replace(
            "application: nom-api\n      access: read", "application: nom-admin\n      access: read"
        ) + original_topics[end:])
        with self.assertRaisesRegex(ValueError, "Mangler lese-ACL"):
            generator.state_overrides(self.root, "api", "q9")
        topics.write_text(original_topics[:start] + topic_block.replace(
            "application: nom-api\n      access: read", "application: nom-api\n      access: write"
        ) + original_topics[end:])
        with self.assertRaisesRegex(ValueError, "Mangler lese-ACL"):
            generator.state_overrides(self.root, "api", "q9")
        topics.write_text(original_topics[:original_topics.rfind("\n---\n", 0, start)] + original_topics[end:])
        with self.assertRaisesRegex(ValueError, "Mangler entydig topic-mal"):
            generator.state_overrides(self.root, "api", "q9")
        topics.write_text(original_topics)

        extra = state_dir / "ny-type-state.yaml"
        extra.write_text(original)
        with self.assertRaisesRegex(ValueError, "Ukjente eller manglende nom-state typer"):
            generator.state_sources(self.root, "api")
        extra.unlink()
        app = self.root / "apps/api/src/main/resources/application.yaml"
        app.write_text(app.read_text() + "\nnom.libs.state.ny-type.enabled: true\n")
        with self.assertRaisesRegex(ValueError, "Ukjent eller ugyldig nom-state aktivering"):
            generator.state_sources(self.root, "api")

    def test_shared_dev_callers_are_additive_and_idempotent(self):
        for path, app, namespace in generator.SHARED_DEV_CALLERS:
            original = (self.root / path).read_text()
            first = generator.add_shared_dev_caller(original, app, namespace, "q9")
            self.assertEqual(generator.add_shared_dev_caller(first, app, namespace, "q9"), first)
            second = generator.add_shared_dev_caller(first, app, namespace, "q8")
            self.assertEqual(second.count(f"- application: {app}-q9 # Auto-utfylt"), 1)
            self.assertEqual(second.count(f"- application: {app}-q8 # Auto-utfylt"), 1)
            self.assertIn(f"          namespace: {namespace}\n          cluster: dev-gcp", second)
            self.assertIn(original, second.replace(
                f"        - application: {app}-q9 {generator.AUTO}\n          namespace: {namespace}\n          cluster: dev-gcp\n", ""
            ).replace(
                f"        - application: {app}-q8 {generator.AUTO}\n          namespace: {namespace}\n          cluster: dev-gcp\n", ""
            ))
            with self.assertRaises(ValueError):
                generator.add_shared_dev_caller(first.replace(
                          f"- application: {app}-q9 {generator.AUTO}\n          namespace: {namespace}",
                          f"- application: {app}-q9 {generator.AUTO}\n          namespace: feil"), app, namespace, "q9")

    def test_shared_dev_manifests_keep_existing_rules_and_prod(self):
        originals = {path: (self.root / path).read_bytes() for path, _, _ in generator.SHARED_DEV_CALLERS}
        prod_paths = (Path(".nais/apps/nom-azure/nom-azure-prod-gcp.yaml"),
                      Path(".nais/apps/proxy/proxy-prod-fss.yaml"))
        prod = {path: (self.root / path).read_bytes() for path in prod_paths}
        generator.generate(self.root, "q9")
        generator.generate(self.root, "q8")
        for path, app, namespace in generator.SHARED_DEV_CALLERS:
            expected = generator.add_shared_dev_caller(originals[path].decode(), app, namespace, "q9")
            expected = generator.add_shared_dev_caller(expected, app, namespace, "q8")
            self.assertEqual((self.root / path).read_text(), expected)
            for suffix in ("q9", "q8"):
                self.assertIn(f"        - application: {app}-{suffix} {generator.AUTO}\n"
                              f"          namespace: {namespace}\n          cluster: dev-gcp\n", expected)
        for path, original in prod.items():
            self.assertEqual((self.root / path).read_bytes(), original)

    def test_rejects_unsafe_suffix_and_existing_targets_without_writing(self):
        for suffix in ("../x", "A", "a_", "a--b", "-a", "a-", "z" * 64):
            with self.subTest(suffix=suffix), self.assertRaises(ValueError):
                generator.generate(self.root, suffix)
        target = self.root / ".nais/topics/topics-additional-dev-q9.yaml"
        target.write_text("keep")
        original = (self.root / ".github/workflows/deploy-api.yaml").read_text()
        with self.assertRaises(FileExistsError):
            generator.generate(self.root, "q9")
        self.assertEqual(target.read_text(), "keep")
        self.assertEqual((self.root / ".github/workflows/deploy-api.yaml").read_text(), original)

    def validator(self, workflow_name, base, variant, suffix="q9"):
        workflow = (self.root / ".github/workflows" / ("deploy-spring-app.yaml" if workflow_name not in ("deploy-ui2.yaml", "deploy-admin-web.yaml") else workflow_name)).read_text()
        match = re.search(r"(?ms)^      - name: Validate variant manifest\n.*?^          python3 - <<'PY'\n(.*?)^          PY$", workflow)
        self.assertIsNotNone(match, workflow_name)
        env = os.environ.copy()
        env.update(BASE_MANIFEST=str(base), VARIANT_MANIFEST=str(variant), SUFFIX=suffix)
        return subprocess.run(["/usr/bin/python3", "-B", "-c", textwrap.dedent(match.group(1))],
                              cwd=self.root, env=env, capture_output=True, text=True)

    def test_validators_accept_quotes_comments_and_reject_wrong_values(self):
        generator.generate(self.root, "q9")
        cases = (
            ("deploy-api.yaml", ".nais/apps/api/api-dev-gcp.yaml", ".nais/apps/api/api-q9-dev-gcp.yaml"),
            ("deploy-streams-intern-ressurs-state.yaml", ".nais/streams/intern-ressurs-state.yaml", ".nais/streams/intern-ressurs-state-q9.yaml"),
            ("deploy-ui2.yaml", ".nais/apps/ui2/ui2-dev.yaml", ".nais/apps/ui2/ui2-q9-dev.yaml"),
            ("deploy-admin-web.yaml", ".nais/apps/admin-web/admin-web-dev.yaml", ".nais/apps/admin-web/admin-web-q9-dev.yaml"),
        )
        for workflow, base, variant in cases:
            with self.subTest(workflow=workflow):
                target = self.root / variant
                content = target.read_text()
                content = re.sub(r"(?m)^kind: (?:['\"]?Application['\"]?)(?:\s+#.*)?$",
                                 "kind: 'Application' # verifiseres", content, count=1)
                content = re.sub(r"(?m)^(  name: )([^\s#]+)(?:\s+#.*)?$",
                                 r'\1"\2" # verifiseres', content, count=1)
                content = re.sub(r"(?m)^    - (https://\S+)(?:\s+#.*)?$",
                                 r"    - '\1' # verifiseres", content)
                if workflow == "deploy-api.yaml":
                    content = re.sub(r"(?m)^(      - name: )([^\s#]+)(?:\s+#.*)?$",
                                     r'\1"\2" # verifiseres', content)
                target.write_text(content)
                accepted = self.validator(workflow, base, variant)
                self.assertEqual(accepted.returncode, 0, accepted.stderr or accepted.stdout)
                target.write_text(content.replace("kind: 'Application'", "kind: 'Other'", 1))
                self.assertNotEqual(self.validator(workflow, base, variant).returncode, 0)
                target.write_text(content)
                if workflow == "deploy-api.yaml":
                    target.write_text(content.replace('"nom-api-q9" # verifiseres', '"another-db" # verifiseres', 1))
                else:
                    target.write_text(content.replace('"' + ("nom-ui2" if workflow == "deploy-ui2.yaml" else "nom-admin-web" if workflow == "deploy-admin-web.yaml" else "nom-intern-ressurs-state") + '-q9" # verifiseres', '"wrong-name" # verifiseres', 1))
                self.assertNotEqual(self.validator(workflow, base, variant).returncode, 0)
                target.write_text(content)
                base_path = self.root / base
                original_base = base_path.read_text()
                base_path.write_text(re.sub(r"(?m)^kind: (?:['\"]?Application['\"]?)(?:\s+#.*)?$",
                                            'kind: "Application" # kommentar', original_base, count=1))
                self.assertEqual(self.validator(workflow, base, variant).returncode, 0)
                base_path.write_text(original_base)
                if workflow == "deploy-api.yaml":
                    target.write_text(content.replace('"nom-api-q9" # verifiseres', 'not-a-variant # verifiseres', 1))
                elif workflow in ("deploy-ui2.yaml", "deploy-admin-web.yaml"):
                    target.write_text(content.replace(".dev.nav.no' # verifiseres", ".example.org' # verifiseres", 1))
                else:
                    continue
                self.assertNotEqual(self.validator(workflow, base, variant).returncode, 0)


if __name__ == "__main__":
    unittest.main()
