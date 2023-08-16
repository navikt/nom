## Lokal kjøring

[Hent aad-properties fra dev-gcp](../../README.md#hente-aad-properties)

Kjør ApiStarter med VM options

    -Dspring.profiles.active=local

### Kjøre med Fiddler Web Proxy for WebClient

Sett opp Fiddler med 'capture HTTPS traffic' og 'Ignore server certificate errors (unsafe)'
Under Connections, sett opp port 8866 (f.eks.), og 'Act as system proxy on startup'

I application-local må proxy.host og port settes. F.eks.

    proxy:
      host: 127.0.0.1
      port: 8866

Til slutt, kjør ApiStarter med proxy profil i tillegg

    -Dspring.profiles.active=local,proxy

