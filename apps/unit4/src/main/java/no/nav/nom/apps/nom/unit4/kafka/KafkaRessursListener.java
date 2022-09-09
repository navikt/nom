package no.nav.nom.apps.nom.unit4.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.nom.unit4.unit4.Unit4Client;
import no.nav.nom.apps.nom.unit4.unit4.Unit4ConflictException;
import no.nav.nom.apps.nom.unit4.unit4.Unit4Exception;
import no.nav.nom.apps.nom.unit4.unit4.Unit4RequestException;
import no.nav.nom.apps.nom.unit4.unit4.Unit4ServerException;
import no.nav.nom.apps.nom.unit4.unit4.Unit4UnknownException;
import no.nav.nom.apps.nom.unit4.unit4.dto.Unit4RessursDto;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaRessursListener {

    private final Unit4Client unit4Client;

    @KafkaListener(id="test", topics = "nom-ressurser", autoStartup = "${app.kafka.listener.enabled:false}")
    public void listener(NomRessursDto record, @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String uuid) {
        log.info("Unit4 ressurs listener mottok melding for ressurs {} på meldings-key: {}", record.getNavident(), uuid);

        Unit4RessursDto unit4RessursDto;
        try {
            unit4RessursDto = new Unit4RessursDto(record);
        } catch (Exception e) {
            log.error("Feil ved konvertering til Unit4 objekt på meldings-key: " + uuid, e);
            //.... TODO: Lag Null-toleranse alert på disse loggmeldingene
            return;
        }

        try {
            unit4Client.createRessursByProxy(unit4RessursDto);
        } catch (Unit4ConflictException e) {
            log.warn("Ressurs finnes i Unit4. Oppdatere i steden? På meldings-key: " + uuid);
            // TODO: Oppdatere i Unit4 i steden?
        } catch (Unit4ServerException e) {  // Andre Unit4 feil (5xx) ? -> Vent x-sec før behandle på nytt
            log.error("Unit 4 server error på meldings-key: " + uuid, e);
            blokkerOgBailUt();
        } catch (Unit4RequestException e) { // Klient feil (4xx) -> Logge, og baile ut? Hva med endring i Unit4 som gjør at vi starter å feile. På alt...
            log.error("Unit 4 request error på meldings-key: " + uuid, e);
            blokkerOgBailUt();
        } catch (Unit4UnknownException e) { // Andre feil
            log.error("Unit 4 Unknown error på meldings-key: " + uuid, e);
            blokkerOgBailUt();
        } catch (JsonProcessingException e) {
            log.error("NomRessursDto kan ikke serialiseres til Json på meldings-key: " + uuid, e);
            //.... TODO: Lag Null-toleranse alert på disse loggmeldingene
        } catch (Unit4Exception e)  {
            log.error("Ubehandlet Unit4 Exception! På meldings-key: " + uuid, e);
            blokkerOgBailUt();
        }
    }

    private static int BLOCK_SECONDS = 20;
    private void blokkerOgBailUt() {
        try {
            Thread.sleep(BLOCK_SECONDS*1000);
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
        throw new RuntimeException("Bailer ut på behandling av melding");

    }

}
