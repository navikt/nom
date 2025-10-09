# Frackend (BFF) for NOM-ui

Slik utvikler du mot lokal frontend

Naviger til `apps/ui2/frackend`-folderen

### Først må du sette lokale miljøvariabler fra rot:

```bash
gcloud auth login
python3 setup_local_azure_secrets.py
```

### Autentiser mot GCP for å ta i bruk Chainguard docker image
```
gcloud auth configure-docker europe-north1-docker.pkg.dev
gcloud auth login
```

### Deretter starte Express+Wonderwall

```bash
docker-compose up # Alternativt legg til -d flag for detached mode.
```

Dersom du har gjort endringer i frackend og vil kun bygge Express-server på nytt

```bash
docker-compose up -d --build frontend
```

### Til slutt starte devserver for frontend

Fra `apps/ui2/webapp` folderen kjører du følgende for å starte vite dev-server med hot-reloading:

```bash
npm install
npm run dev # Starter vite devserver på port 5173 som standard
```

Gå så til `localhost:8080` og logg på med trygdeetaten brukeren din. Etter innlogging får du en feil, men gå nå istedenfor til porten for dev-server
`localhost:8080/vite-on`.

### Troubleshooting

#### setup_local_azure_secrets.py scriptet feiler

Sannsynligvis skyldes det:

1. Kubectl skriver en client-throttling melding til console når det er lenge siden man har kjørt en kubectl kommando. Kjør scriptet på nytt, sannsynligvis
   kommer det ikke andre gangen.
2. Slett eksisterende verdier i .localenv pg kjør deretter scriptet på ny
3. gke-gcloud-auth-plugin mangler og du får ikke logget inn. Husk å legge til path/home

   
