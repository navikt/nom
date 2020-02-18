# NAV Organization Master (NOM)

## How to run locally
1. Spin up a Docker container with PostgreSQL: `docker run --name postgres -p 5432:5432 -e POSTGRES_PASSWORD=password -d postgres:alpine`
2. Spin up network for kafka: `docker network create kafka-net --driver bridge`
3. Spin up zookeeper: `docker run --name zookeeper-server -p 2181:2181 --network kafka-net -e ALLOW_ANONYMOUS_LOGIN=yes bitnami/zookeeper:latest`
4. Spin up kafka: `docker run --name kafka-server1 --network kafka-net -e ALLOW_PLAINTEXT_LISTENER=yes -e KAFKA_CFG_ZOOKEEPER_CONNECT=zookeeper-server:2181 -e KAFKA_CFG_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 -p 9092:9092 bitnami/kafka:latest`
5. In IntelliJ, go to Run/Debug Configuration and assign `local` as your Active profile.