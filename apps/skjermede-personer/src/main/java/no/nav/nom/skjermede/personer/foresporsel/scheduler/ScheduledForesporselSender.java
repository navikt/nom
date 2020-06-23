package no.nav.nom.skjermede.personer.foresporsel.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.skjermede.personer.common.LeaderElector;
import no.nav.nom.skjermede.personer.common.NomRessurserService;
import no.nav.nom.skjermede.personer.foresporsel.ForesporselRepository;
import no.nav.nom.skjermede.personer.common.GraphService;
import no.nav.nom.skjermede.personer.foresporsel.azure.TokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Slf4j
@Component
@Profile("!local & !test")
@RequiredArgsConstructor
public class ScheduledForesporselSender {

    private final ForesporselRepository foresporselRepository;
    private final NomRessurserService nomRessurserService;
    private final GraphService graphService;
    private final LeaderElector leaderElector;
    private final TokenService tokenService;

    @Value("${foresporsel-api}")
    private String foresporselAPI;

    @Scheduled(fixedDelay = 10000, initialDelay = 10000)
    public void scheduledSender() {
        if (leaderElector.isLeader()) {
            sendForesporsler();
        }
    }

    private void sendForesporsler() {
        foresporselRepository.findAllByEpostSendtNull().forEach(foresporsel -> {
            val content = new StringBuilder()
                    .append(String.format("Du får denne eposten fordi %s %s har oppgitt deg som nært familiemedlem. ", foresporsel.getFornavn(), foresporsel.getEtternavn()))
                    .append("Hvis dette er riktig kan du bekrefte ved å klikke på knappen under. ")
                    .append(String.format("%s %s vil da bli skjermet i NAV sine fagsystemer. ", foresporsel.getFornavn(), foresporsel.getEtternavn()))
                    .append("Det innebærer at bare saksbehandlere med særlig tilgang kan se informasjon og behandle saker. ")
                    .append("Les mer om skjerming <a href=\"https://navno.sharepoint.com/sites/intranett-personvern/SitePages/Alle-ansatte-skjermes-fra-01.01.2020.aspx\">her</a>.")
                    .append(String.format("<br><br><a href=\"%s/bekreft?id=%s\"><button>Bekreft Forespørsel</button></a>", foresporselAPI, foresporsel.getFunksjonellId()))
                    .toString();

            try {
                graphService.sendMail(content, hentEpost(foresporsel.getTilknyttetRessurs()), tokenService.getAccessToken());
                foresporsel.setEpostSendt(LocalDateTime.now());
                foresporselRepository.save(foresporsel);
            } catch (Exception e) {
                log.error(String.format("Sending av skjerming-forespørsel epost mislyktes: %s", e.getMessage()));
            }
        });
    }

    private String hentEpost(final String navident) {
        val ressurs = nomRessurserService.getInternRessursByNavident(navident, Boolean.TRUE);

        if (ressurs == null || ressurs.getEpost() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("Ressurs med navident %s ikke funnet", navident));
        }

        return ressurs.getEpost();
    }
}
