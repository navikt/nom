# NAV Organization Master (NOM)

## How to run locally
1. Spin up docker-containers with `docker-compose up -d` located in project-root.
2. In IntelliJ, go to Run/Debug Configuration and assign `local` as your Active profile.

En midlertidig feil med docker-oppsettet er at kafka topic på en ny container ikke opprettes før første melding på topicet er sendt.