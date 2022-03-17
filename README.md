# NAV Organization Master (NOM)

## How to run locally 
1. Spin up docker-containers with `docker-compose up -d` located in project-root.
2. In IntelliJ, go to Run/Debug Configuration and assign `local` as your Active profile.

En midlertidig feil med docker-oppsettet er at kafka topic på en ny container ikke opprettes før første melding på topicet er sendt.

## Hvordan benytte pgAdmin lokalt
pgAdmin er satt opp lokalt på adressen http://localhost:8089/ med bruker/passord = `user@domain.com/password`.
For å nå en lokal nom database velg 'Create server' og gi den ett navn på 'General' tabben.
På 'Connection' tabben sett opp følgende:
```
Host name:  enhet-db/ressurs-db/skjermet-db (velg en)
port: 5432/5433/5434 (henholdsvis)
Maintenance database: postgres-enheter/postgres-ressurs/postgres-skjermet (henholdsvis)
Username: postgres
Password: password
```
Kryss gjerne av for lagre passord.

På 'Advanced' tabben må 'Host Address' være ip adressen til maskinen din. Eksempelvis 192.168.39.139. Altså ip adressen til docker host. Dette kan ikke være localhost referanse, for det vil bare peke på ip adressen som pgAdmin docker container har fått. 

pgAdmin har fått en docker volume for config oppsett slik at denne konfigurasjonen persisteres mellom hvar gang men kjører opp lokalmiljøet.
