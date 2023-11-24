import subprocess as sp
from dataclasses import dataclass
import argparse

kafka_bootstrap_server = "localhost:39092"


@dataclass
class EntityTopics:
    event_source_topic: str
    state_topic: str
    streams_application_id: str


def run_command(command: str):
    return sp.run(command.split(" "), capture_output=True).stdout.decode("utf-8")


def delete_topic(topic: str):
    print(f"Deleting local topic {topic}")
    run_command(f"kafka-topics --bootstrap-server {kafka_bootstrap_server} --delete --topic {topic}")


def reset_streams_application(streams_application_id):
    print(f"Resetting local streams application {streams_application_id}")
    run_command(
        f"kafka-streams-application-reset --bootstrap-server {kafka_bootstrap_server} --application-id {streams_application_id} --to-earliest"
    )


def create_topic(topic: str):
    print(f"Creating local topic {topic}")
    run_command(
        f"kafka-topics --bootstrap-server {kafka_bootstrap_server} --create --topic {topic}  --partitions 1 --replication-factor 1"
    )


def create_topics(entity_topics: EntityTopics, recreate: bool):

    if recreate:
        delete_topic(entity_topics.event_source_topic)
        delete_topic(entity_topics.state_topic)
        reset_streams_application(entity_topics.streams_application_id)

    create_topic(entity_topics.event_source_topic)
    create_topic(entity_topics.state_topic)


def create_topics_for_nom_entities(nom_entity_filter: list, recreate: bool):
    entity_topics_map = {
        "orgenhet": EntityTopics(
            event_source_topic="org.nom.orgenhet-eventsource",
            state_topic="org.nom.intern-orgenhet-state",
            streams_application_id="orgenhet-state"
        ),
        "orgtilknytning": EntityTopics(
            event_source_topic="org.nom.orgtilknytning-eventsource",
            state_topic="org.nom.intern-orgtilknytning-state",
            streams_application_id="orgtilknytning-state"
        ),
        "ressurs": EntityTopics(
            event_source_topic="org.nom.ressurs-eventsource",
            state_topic="org.nom.intern-ressurs-state",
            streams_application_id="ressurs-state"
        ),
        "organisering": EntityTopics(
            event_source_topic="org.nom.organisering-eventsource",
            state_topic="org.nom.intern-organisering-state",
            streams_application_id="organisering-state"
        )
    }

    for entity, entity_topics in entity_topics_map.items():

        if entity in nom_entity_filter or len(nom_entity_filter) == 0:
            create_topics(entity_topics, recreate)


if __name__ == "__main__":
    argument_parser = argparse.ArgumentParser()
    argument_parser.add_argument("--entities", type=str, required=False)
    argument_parser.add_argument("--recreate", action=argparse.BooleanOptionalAction)
    arguments = argument_parser.parse_args()

    nom_entity_filter = []
    if arguments.entities:
        nom_entity_filter = arguments.entities.split(",")

    create_topics_for_nom_entities(nom_entity_filter, arguments.recreate)
