package no.nav.nom.skjermede.personer.foresporsel.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.skjermede.personer.foresporsel.ForesporselRepository;
import no.nav.nom.skjermede.personer.foresporsel.mail.GraphService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;

@Slf4j
@Component
@Profile("!local")
@RequiredArgsConstructor
public class ScheduledForesporselSender {

    private final ForesporselRepository foresporselRepository;
    private final GraphService graphService;

    @Value("${ressurser.api}")
    private String ressursAPI;

    @Value("${foresporsel.api}")
    private String foresporselAPI;

    @Scheduled(fixedDelay = 10000, initialDelay = 10000)
    public void scheduledSender() {
        sendForesporsler();
    }

    private void sendForesporsler() {
        foresporselRepository.findAllByEpostSendtNull().forEach(foresporsel -> {
            final String content = new StringBuilder()
                    .append(String.format("Du får denne eposten fordi %s %s har oppgitt deg som nært familiemedlem. ", foresporsel.getFornavn(), foresporsel.getEtternavn()))
                    .append("Hvis dette er riktig kan du bekrefte ved å klikke på knappen under. ")
                    .append(String.format("%s %s vil da bli skjermet i NAV sine fagsystemer. ", foresporsel.getFornavn(), foresporsel.getEtternavn()))
                    .append("Det innebærer at bare saksbehandlere med særlig tilgang kan se informasjon og behandle saker. ")
                    .append("Les mer om skjerming <a href=\"https://navno.sharepoint.com/sites/enhet-oslo/SitePages/Skjerming-av-ansatte.aspx\">her</a>")
                    .append(String.format("<br><a href=\"%s/godkjenn?id=%s\"><button>Godkjenn</button></a>", foresporselAPI, foresporsel.getFunksjonellId()))
                    .toString();

            try {
                graphService.sendMail(content, hentEpost(foresporsel.getTilknyttetRessurs()));
                foresporsel.setEpostSendt(LocalDate.now());
                foresporselRepository.save(foresporsel);
            } catch (Exception e) {
                log.error(String.format("Sending av skjerming forespørsel epost mislyktes: %s", e.getMessage()));
            }
        });
    }

    private String hentEpost(final String navident) {
        return new RestTemplate().getForObject(
                String.format("%s/epost/{navident}", ressursAPI),
                String.class,
                navident);
    }
}
