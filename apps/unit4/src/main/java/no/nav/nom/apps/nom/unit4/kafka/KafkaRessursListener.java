package no.nav.nom.apps.nom.unit4.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.getunleash.Unleash;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.nom.unit4.AppProperties;
import no.nav.nom.apps.nom.unit4.unit4.Unit4Client;
import no.nav.nom.apps.nom.unit4.unit4.Unit4ConflictException;
import no.nav.nom.apps.nom.unit4.unit4.Unit4Exception;
import no.nav.nom.apps.nom.unit4.unit4.Unit4RequestException;
import no.nav.nom.apps.nom.unit4.unit4.Unit4ServerException;
import no.nav.nom.apps.nom.unit4.unit4.Unit4UnknownException;
import no.nav.nom.apps.nom.unit4.unit4.dto.Unit4RessursDto;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.time.Duration;

import static no.nav.nom.apps.nom.unit4.AppConfig.SECURE_LOGS;

@Component
@Slf4j
public class KafkaRessursListener {

    public static final String UNLEASH_SKRU_AV_EKSTRAAGA_TOGGLE = "nom.unit4.ikke_legg_til_ekstra_arbeidsgiveravgift_relasjon";
    private final Unit4Client unit4Client;
    private final Duration blockSeconds;

    private final Unleash unleash;
    @Value("${app.unit4.felles-kobling-value:000000}")
    private String fellesKoblingValue;

    public KafkaRessursListener(Unit4Client unit4Client, AppProperties appProperties, Unleash unleash) {
        this.unit4Client = unit4Client;
        this.blockSeconds = appProperties.kafka().listener().blockSecondsOnBail();
        this.unleash = unleash;
    }

    @KafkaListener(topics = "${app.ressurs.topic}")
    public void listener(RessursEvent record, @Header(KafkaHeaders.RECEIVED_KEY) String uuid) {
        log.info("Unit4 ressurs listener mottok melding for ressurs {} på meldings-key: {}", record.getFid(), uuid);
        if(record instanceof RessursOpprettetEventV1 ressursOpprettetEventV1) {
            if(ressursOpprettetEventV1.getMetadata().inputMetadata().origination().initiatedFrom().contains("nom-ui")) {
                Unit4RessursDto unit4RessursDto;
                try {
                    unit4RessursDto = new Unit4RessursDto(ressursOpprettetEventV1, fellesKoblingValue, !unleash.isEnabled(UNLEASH_SKRU_AV_EKSTRAAGA_TOGGLE, true));
                } catch (Exception e) {
                    log.error("Feil ved konvertering til Unit4 objekt på meldings-key: {}", uuid);
                    log.error(SECURE_LOGS, "Feil ved konvertering til Unit4 objekt på meldings-key: {}, event: {}", uuid, ressursOpprettetEventV1, e); // Send more detailed information to secure-logs logger.
                    return;
                }

                try {
                    unit4Client.createRessurs(unit4RessursDto);
                } catch (Unit4ConflictException e) {
                    log.warn("Ressurs finnes i Unit4. Oppdatere i stedet? På meldings-key: {}", uuid);
                } catch (Unit4ServerException e) {  // Andre Unit4 feil (5xx) ? -> Vent x-sec før behandle på nytt
                    log.warn("Unit 4 server error på meldings-key: {}", uuid, e);
                    blokkerOgBailUt();
                } catch (Unit4RequestException e) { // Klient feil (4xx) -> Logge, og baile ut? Hva med endring i Unit4 som gjør at vi starter å feile. På alt...
                    log.warn("Unit 4 request error på meldings-key: {}", uuid, e);
                    blokkerOgBailUt();
                } catch (Unit4UnknownException e) { // Andre feil
                    log.warn("Unit 4 Unknown error på meldings-key: {}", uuid, e);
                    blokkerOgBailUt();
                } catch (JsonProcessingException e) {
                    log.error("NomRessursDto kan ikke serialiseres til Json på meldings-key: {}", uuid, e);
                } catch (Unit4Exception e) {
                    log.warn("Ubehandlet Unit4 Exception! På meldings-key: {}", uuid, e);
                    blokkerOgBailUt();
                }
            }
        }
    }

    private void blokkerOgBailUt() {
        try {
            Thread.sleep(blockSeconds.toMillis());
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
        throw new RuntimeException("Bailer ut på behandling av melding");
    }

}
