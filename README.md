# NAV Organization Master (NOM)

## How to run locally
1. Spin up a Docker container with PostgreSQL: `docker run --name postgres -p 5432:5432 -e POSTGRES_PASSWORD=password -d postgres:alpine`
2. In IntelliJ, go to Run/Debug Configuration and assign `local` as your Active profile.