package no.nav.nom.apps.nom.ressurser.dvh;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.nom.ressurser.ressurser.Ressurs;
import no.nav.nom.apps.nom.ressurser.ressurser.RessursRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;

@Profile("local")
@Component
@RequiredArgsConstructor
@Slf4j
public class DvhConsumer {

    @Value("${dvh.endpoint}")
    private String dvhEndpoint;
    private String uriVariable = "in_sekvensnummer=";

    private RestTemplate restTemplate;
    private final RessursRepository ressursRepository;
    private final OffsetRepository offsetRepository;

    @Scheduled(cron = "${app.scheduledTasks.dvhConsumer}")
    @Transactional(value = "transactionManager")
    public void hentBulkOppdatering(){

    }

    private static Ressurs converted(DvhRessursDTO dvhRessursDTO){
        return Ressurs.builder()
                .navIdent(dvhRessursDTO.getNav_ident())
                .personident(dvhRessursDTO.getPersonIdent())
                .ressurstype(null)
                .startdato(dvhRessursDTO.getStart_dato())
                .sluttdato(null)
                .opprettetDato(LocalDate.now())
                .endretDato(LocalDate.now())
                .build();
    }

    @Bean
    private RestTemplate restTemplate(){
        return new RestTemplate();
    }
}
