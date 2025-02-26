package no.nav.nom.apps.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableJpaRepositories
@EnableAsync
@ConfigurationPropertiesScan
public class ApiStarter {

// NOM-UNIT4 appen er skrudd av
//  Denne er i dag kodet til å lese alle RessursOpprettet eventer.
//  * Den sjekker om opprett event kommer fra NOM-UI. (metadata info)
//  * Og at den ikke er ekstern
//  Den vil dermed forsøke å opprette Kommunale ressurser i U4.
//  Det ønsker vi ikke. Det er også uklart om vi skal (noensinne) opprette i U4 i det hele tatt.
//
//  Når jeg så på dette så tror jeg det umiddelbart er noen problemer med løsningen.
//  * Akkurat nå feiler koden i test med at den ikke får forventet info i opprett respons mot U4, (altså dagens funksjonalitet har ant en bug as-is)
//  * Den er laget til å lese Ressurs eventer fra start (tidenes morgen), på consumer gruppen. Dette kan potensielt skape en situasjon med dobbelt opprettelse
//    av ressurser i U4. Dette må evt hensyntas hvis denne skal skrus på igjen.
//    (En consumer gruppe i kafka slettes automatisk etter default 7 dager hvis det ikke har funnets en consumer på den)
//
// Ref: https://nav-it.slack.com/archives/CGJDVTF6H/p1714465869648509

    public static void main(String[] args) {
        SpringApplication.run(ApiStarter.class, args);

    }
}