package no.nav.nom.apps.sfintegrasjon.outbox;

import io.getunleash.Unleash;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.sfintegrasjon.orgenhet.domain.SfOrgEnhet;
import no.nav.nom.apps.sfintegrasjon.outbox.domain.SfOutbox;
import no.nav.nom.apps.sfintegrasjon.outbox.domain.SfStatus;
import no.nav.nom.apps.sfintegrasjon.outbox.repository.SfOutboxRepository;
import no.nav.nom.apps.sfintegrasjon.sfclient.SfCpiClient;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

@Slf4j
@Service
public class SfOutboxService {

    // Fast prefiks, slik at radene kan finnes med SQL og sendes på nytt etter en retting
    private static final String LESEFEIL_PREFIKS = "Kunne ikke lese orgenheten i outboxen";

    private final SfOutboxRepository sfOutboxRepository;
    private final SfCpiClient sfCpiClient;
    private final LeaderElection leaderElection;
    private final ObjectMapper objectMapper;
    private final Unleash unleash;

    public SfOutboxService(SfOutboxRepository sfOutboxRepository, SfCpiClient sfCpiClient, LeaderElection leaderElection, ObjectMapper objectMapper,
                           Unleash unleash
    ) {
        this.sfOutboxRepository = sfOutboxRepository;
        this.sfCpiClient = sfCpiClient;
        this.leaderElection = leaderElection;
        this.objectMapper = objectMapper;
        this.unleash = unleash;
    }

    // Ved :40, etter at NomSuccessFactorsOrgEnhetOppdateringService har bygget radene (:20)
    @Scheduled(cron = "40 * * * * *")
    public void sendPayloadToSfCpi() {
        if (!leaderElection.isLeader()) {
            return;
        }

        if (!unleash.isEnabled("nom-sf-integrasjon.outbox-service")) {
            return;
        }

        var allIkkeSendtEnheter = sfOutboxRepository.getAllBySfStatusIn(List.of(SfStatus.IKKE_SENDT, SfStatus.FEILET))
            .stream()
            .collect(Collectors.groupingBy(SfOutbox::getOrgEnhetId));

        allIkkeSendtEnheter.forEach((_, enheter) -> {
            var nyesteSfOutboxEnhet = enheter.stream().max(Comparator.comparing(SfOutbox::getSporingsId)).orElseThrow();
            var sfOutboxDTO = SfOutboxDTO.builder()
                .sporingsId(nyesteSfOutboxEnhet.getSporingsId().toString())
                .endretAv(nyesteSfOutboxEnhet.getEndretAv())
                .endringshendelser(nyesteSfOutboxEnhet.getEndringshendelser())
                .build();
            var nyesteSporingsId = nyesteSfOutboxEnhet.getSporingsId();
            enheter.stream().filter(enhet -> !Objects.equals(nyesteSporingsId, enhet.getSporingsId())).forEach(enhet -> sfOutboxRepository.save(enhet.toBuilder().sfStatus(SfStatus.ERSTATTET).build()));

            try {
                var sfOrgEnhet = objectMapper.readValue(nyesteSfOutboxEnhet.getOrgenhet(), SfOrgEnhet.class);
                SfOutboxDTO response;
                if (unleash.isEnabled("nom-sf-integrasjon.outbox-service-sende-til-sf")) {
                    response = sfCpiClient.sendOrgenhetPayload(sfOutboxDTO.toBuilder().sfOrgEnhet(sfOrgEnhet).build()).getBody();
                } else {
                    response = SfOutboxDTO.builder()
                        .sporingsId(nyesteSfOutboxEnhet.getSporingsId().toString())
                        .endretAv(nyesteSfOutboxEnhet.getEndretAv())
                        .endringshendelser(nyesteSfOutboxEnhet.getEndringshendelser())
                        .sfOrgEnhet(sfOrgEnhet)
                        .build();
                    log.info("UNLEASH TOGGLE ER AV, OUTBOX RAD BLIR SATT TIL SENDT. SporingsId: {}", response.sporingsId());
                }
                if (isNull(response) || !Objects.equals(response.sporingsId(), sfOutboxDTO.sporingsId())) {
                    var feilmelding = String.format("Tomt eller uventet svar fra SF for sporingsId %s", sfOutboxDTO.sporingsId());
                    nyesteSfOutboxEnhet = nyesteSfOutboxEnhet.toBuilder().sfStatus(SfStatus.AVVIST).feilmelding(feilmelding).build();
                    log.error(feilmelding);
                } else {
                    nyesteSfOutboxEnhet = nyesteSfOutboxEnhet.toBuilder().feilmelding(null).sfStatus(SfStatus.SENDT).build();
                }
            } catch (RestClientResponseException rcre) {
                var status = rcre.getStatusCode();
                var midlertidig = status.is5xxServerError() || status.value() == 408 || status.value() == 429;
                var nyStatus = midlertidig ? SfStatus.FEILET : SfStatus.AVVIST;
                String feilmelding = String.format(
                    "Sendingen til SF med sporingsId %s feilet med en Http Status: %s og exception melding: %s. Responsen fra SF: %s",
                    sfOutboxDTO.sporingsId(),
                    rcre.getStatusCode(),
                    rcre.getMessage(),
                    rcre.getResponseBodyAsString()
                );
                nyesteSfOutboxEnhet = nyesteSfOutboxEnhet.toBuilder().sfStatus(nyStatus).feilmelding(feilmelding).build();
                log.error(feilmelding, rcre);
            } catch (ResourceAccessException rae) {
                String feilmelding = String.format(
                    "Feilet på grunn IO, nettverk eller generell timeout for sporingsId %s og melding %s",
                    sfOutboxDTO.sporingsId(),
                    rae.getMessage()
                );
                nyesteSfOutboxEnhet = nyesteSfOutboxEnhet.toBuilder().sfStatus(SfStatus.FEILET).feilmelding(feilmelding).build();
                log.error(feilmelding, rae);
            } catch (JacksonException je) {
                String feilmelding = String.format(
                    "%s for sporingsId %s: %s",
                    LESEFEIL_PREFIKS,
                    sfOutboxDTO.sporingsId(),
                    je.getMessage()
                );
                nyesteSfOutboxEnhet = nyesteSfOutboxEnhet.toBuilder().sfStatus(SfStatus.AVVIST).feilmelding(feilmelding).build();
                log.error(feilmelding, je);
            } catch (RestClientException | NullPointerException e) {
                String feilmelding = String.format(
                    "En uventet feil skjedde med sporingsId %s som har feilmelding %s",
                    sfOutboxDTO.sporingsId(),
                    e.getMessage()
                );
                nyesteSfOutboxEnhet = nyesteSfOutboxEnhet.toBuilder().sfStatus(SfStatus.FEILET).feilmelding(feilmelding).build();
                log.error(feilmelding, e);
            } finally {
                sfOutboxRepository.save(nyesteSfOutboxEnhet);
            }
        });
    }

    @Transactional
    @Scheduled(cron = "0 30 8 * * MON-FRI")
    public void logRejectedErrorStates() {
        if (leaderElection.isLeader()) {
            var failedAndRejectedStates = sfOutboxRepository.getAllBySfStatusIn(List.of(SfStatus.AVVIST, SfStatus.FEILET));
            if (failedAndRejectedStates.isEmpty()) {
                return;
            }

            var rejectedSize = failedAndRejectedStates.stream().filter(sfOutbox -> sfOutbox.getSfStatus().equals(SfStatus.AVVIST)).count();
            if (rejectedSize > 0) {
                log.info(
                    "ALERT: En sjekk har blitt gjort på nom-sf-integrasjonen og det er {} avviste rader i Outbox tabellen, det kan sjekkes i nom-admin",
                    rejectedSize
                );
            }
            var failedSize = failedAndRejectedStates.stream().filter(sfOutbox -> sfOutbox.getSfStatus().equals(SfStatus.FEILET)).count();
            if (failedSize > 0) {
                log.info(
                    "ALERT: En sjekk har blitt gjort på nom-sf-integrasjonen og det er {} feilede rader i Outbox tabellen, det kan sjekkes i nom-admin",
                    failedSize
                );
            }
        }
    }
}
