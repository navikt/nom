# Frackend (BFF) for NOM-ui

Slik utvikler du mot lokal frontend

Naviger til `apps/ui2/frackend`-folderen

### Først må du sette lokale miljøvariabler:
```bash
# Logg inn i gcloud
gcloud auth login
# Oppdater miljøvariabler
python3 localenv_setup.py 
# Verdien for AZURE_APP_JWK må quotes. Putt ' før og etter {}-parantesene. {} -> K'{}'
```

### Deretter starte express-serveren
```bash
npm install
npm run start # Starter express server på port 8080 som standard
```

### Til slutt starte devserver for frontend
Fra `apps/ui2/webapp` folderen kjører du følgende for å starte vite dev-server med hot-reloading:
```bash
npm install
npm run dev # Starter vite devserver på port 5173 som standard
```

Gå så til localhost:8080 og logg på med trygdeetaten brukeren din.
Velg 'uidev' og 'upload' for å laste opp sesjonen. Følg lenken 'default' for å gå til lokalt frontend.

### Troubleshooting
#### localenv_setup.py scriptet feiler
Sannsynligvis skyldes det:
1. Kubectl skriver en client-throttling melding til console når det er lenge siden man har kjørt en kubectl kommando. Kjør scriptet på nytt, sannsynligvis kommer det ikke andre gangen.
2. Slett eksisterende verdier i .localenv pg kjør deretter scriptet på ny

   
