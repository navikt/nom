package no.nav.nom.apps.api.orgenhet.eventsourcing.producer;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.PrefixedUlidCreator;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.AvvikleOrgEnhet;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.EndreNomNivaa;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.EndreOrgEnhetsType;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.EndreTilgangsenhet;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.KorrigerGyldigFom;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.KorrigerOrgEnhet;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.KorrigerOrgEnhetNavn;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.OpprettOrgEnhet;
import no.nav.nom.apps.api.orgenhet.implementations.dvh.v2.DvhOrgenhetV2;
import no.nav.nom.contract.nom.eventsource.KorrigerGyldigFomEventV1;
import no.nav.nom.contract.nom.eventsource.NomNivaaEndretEventV1;
import no.nav.nom.contract.nom.eventsource.OrgEnhetAvvikletEventV1;
import no.nav.nom.contract.nom.eventsource.OrgEnhetEndretNavnEventV1;
import no.nav.nom.contract.nom.eventsource.OrgEnhetEvent;
import no.nav.nom.contract.nom.eventsource.OrgEnhetKorrigertEventV1;
import no.nav.nom.contract.nom.eventsource.OrgEnhetOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.OrgEnhetTypeEndretEventV1;
import no.nav.nom.contract.nom.eventsource.TilgangsenhetEndretEventV1;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.libs.state.orgenhet.NomStateOrgEnhetListener;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
public class OrgEnhetCommandService {

    private final OrgEnhetEventProducer eventProducer;
    private final JsonMapper jsonMapper;
    private final NomStateOrgEnhetListener nomStateOrgEnhetListener;

    public OrgEnhetCommandService(OrgEnhetEventProducer eventProducer, JsonMapper jsonMapper, NomStateOrgEnhetListener nomStateOrgEnhetListener) {
        this.eventProducer = eventProducer;
        this.jsonMapper = jsonMapper;
        this.nomStateOrgEnhetListener = nomStateOrgEnhetListener;
    }

    public String opprettOrgEnhet(OpprettOrgEnhet opprettOrgEnhet) {
        String fid = PrefixedUlidCreator.createOrgEnhetUlid();
        opprettOrgEnhet(fid, opprettOrgEnhet);
        return fid;
    }

    public OrgEnhetState opprettOrgEnhetSync(OpprettOrgEnhet opprettOrgEnhet) throws ExecutionException, InterruptedException, TimeoutException {
        boolean exceptionThrown = false;
        final var fid = PrefixedUlidCreator.createOrgEnhetUlid();
        final var response = nomStateOrgEnhetListener.waitForStateAfterEventCreation(fid);
        opprettOrgEnhet(fid, opprettOrgEnhet);

        try {
            return response.get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            exceptionThrown = true;
            nomStateOrgEnhetListener.removeWait(fid);
            log.error("Error waiting for orgenhet to be created", e);
            throw e;
        } finally {
            if (exceptionThrown) {
                avvikleOrgEnhet(fid, new AvvikleOrgEnhet(
                    opprettOrgEnhet.id(),
                    opprettOrgEnhet.gyldigFom(),
                    opprettOrgEnhet.inputMetadata()
                ));
            }
        }
    }

    private CompletableFuture<SendResult<String, OrgEnhetEvent>> opprettOrgEnhet(String fid, OpprettOrgEnhet opprettOrgEnhet) {
        return eventProducer.sendMessage(getOpprettEvent(fid, opprettOrgEnhet));
    }

    private OrgEnhetOpprettetEventV1 getOpprettEvent(String fid, OpprettOrgEnhet opprettOrgEnhet) {
        OrgEnhetOpprettetEventV1.OrgEnhetOpprettetAdditionalAgressoData additionalAgressoData = null;
        if (opprettOrgEnhet.inputMetadata().inputInterfaceMethodName().equals("Datavarehus")) {
            var input = jsonMapper.valueToTree(opprettOrgEnhet.inputMetadata().inputInterfaceMethodArguments());
            try {
                var raw = input.isArray() ? input.get(0): input;
                DvhOrgenhetV2 dvhOrgenhet2 = DvhOrgenhetV2.fromOrds(raw);
                additionalAgressoData = new OrgEnhetOpprettetEventV1.OrgEnhetOpprettetAdditionalAgressoData(
                    dvhOrgenhet2.getAgressoId(),
                    dvhOrgenhet2.getNavn(),
                    dvhOrgenhet2.getOrgNiv(),
                    dvhOrgenhet2.getAgressoOrgenhetType(),
                    dvhOrgenhet2.getUniqueAgressoId()
                );
            } catch (Exception e) {
                log.error("Problem converting orgEnhet info from dvh", e);
                log.info("dvh data: {}", input.toPrettyString());
            }
        } else {
            additionalAgressoData = new OrgEnhetOpprettetEventV1.OrgEnhetOpprettetAdditionalAgressoData(
                "00",
                opprettOrgEnhet.navn(),
                "ORGNIVX",
                "",
                "00-" + opprettOrgEnhet.navn() + "ORGNIVX"
            );
        }
        return new OrgEnhetOpprettetEventV1(
                fid,
                new OrgEnhetOpprettetEventV1.OrgEnhetOpprettetEventData(
                        opprettOrgEnhet.id(),
                        opprettOrgEnhet.navn(),
                        opprettOrgEnhet.gyldigFom(),
                        opprettOrgEnhet.overordnetOrgEnhetFid(),
                        opprettOrgEnhet.lederRessursFid(),
                        opprettOrgEnhet.tilgangsenhetId()
                ),
                opprettOrgEnhet.inputMetadata(),
                additionalAgressoData
        );
    }

    public CompletableFuture<SendResult<String, OrgEnhetEvent>> korrigerNavnOrgEnhet(String fid, KorrigerOrgEnhetNavn oppdaterOrgEnhet) {
        return eventProducer.sendMessage(new OrgEnhetEndretNavnEventV1(
            fid,
            new OrgEnhetEndretNavnEventV1.OrgEnhetNavnEndretEventData(
                oppdaterOrgEnhet.id(),
                oppdaterOrgEnhet.navn()
            ),
            oppdaterOrgEnhet.inputMetadata()
        ));
    }

    public CompletableFuture<SendResult<String, OrgEnhetEvent>> avvikleOrgEnhet(String fid, AvvikleOrgEnhet avvikleOrgEnhet) {
        return eventProducer.sendMessage(new OrgEnhetAvvikletEventV1(
                fid,
                new OrgEnhetAvvikletEventV1.OrgEnhetAvvikletEventData(
                    avvikleOrgEnhet.id(),
                    avvikleOrgEnhet.gyldigTom()
                ),
                avvikleOrgEnhet.inputMetadata()
        ));
    }

    // Alle verdier i KorrigerOrgEnhet objektet erstatter eksisterende verdier. Også tomme.
    public CompletableFuture<SendResult<String, OrgEnhetEvent>> korrigerOrgEnhet(String fid, KorrigerOrgEnhet korrigerOrgEnhet) {
        OrgEnhetKorrigertEventV1.OrgEnhetKorrigertAdditionalAgressoData additionalAgressoData;
        if (korrigerOrgEnhet.inputMetadata().inputInterfaceMethodName().equals("Datavarehus")) {
            var input = jsonMapper.valueToTree(korrigerOrgEnhet.inputMetadata().inputInterfaceMethodArguments());
            var raw = input.isArray() ? input.get(0): input;
            DvhOrgenhetV2 dvhOrgenhet2 = DvhOrgenhetV2.fromOrds(raw);
            additionalAgressoData = new OrgEnhetKorrigertEventV1.OrgEnhetKorrigertAdditionalAgressoData(
                    dvhOrgenhet2.getAgressoId(),
                    dvhOrgenhet2.getNavn(),
                    dvhOrgenhet2.getOrgNiv(),
                    dvhOrgenhet2.getAgressoOrgenhetType(),
                    dvhOrgenhet2.getUniqueAgressoId()
            );
        } else {
            additionalAgressoData = new OrgEnhetKorrigertEventV1.OrgEnhetKorrigertAdditionalAgressoData(
                "00",
                korrigerOrgEnhet.navn(),
                "ORGNIVX",
                "",
                "00-" + korrigerOrgEnhet.navn() + "ORGNIVX"
            );
        }

        return eventProducer.sendMessage(new OrgEnhetKorrigertEventV1(
                fid,
                new OrgEnhetKorrigertEventV1.OrgEnhetKorrigertEventData(
                        korrigerOrgEnhet.id(),
                        korrigerOrgEnhet.navn(),
                        korrigerOrgEnhet.gyldigFom(),
                        korrigerOrgEnhet.gyldigTom(),
                        korrigerOrgEnhet.overordnetOrgEnhetFid(),
                        korrigerOrgEnhet.lederRessursFid()
                ),
                korrigerOrgEnhet.inputMetadata(),
                additionalAgressoData
                ));
    }

    public CompletableFuture<SendResult<String, OrgEnhetEvent>> endreOrgEnhetsType(String fid, EndreOrgEnhetsType endreOrgEnhetsType) {
        return eventProducer.sendMessage(new OrgEnhetTypeEndretEventV1(
            fid,
            new OrgEnhetTypeEndretEventV1.OrgEnhetTypeEndretEventData(
                endreOrgEnhetsType.id(),
                endreOrgEnhetsType.orgEnhetsType()
            ),
            endreOrgEnhetsType.inputMetadata()
        ));
    }

    public CompletableFuture<SendResult<String, OrgEnhetEvent>> endreNomNivaa(String fid, EndreNomNivaa endreNomNivaa) {
        return eventProducer.sendMessage(new NomNivaaEndretEventV1(
            fid,
            new NomNivaaEndretEventV1.NomNivaaEndretEventData(
                endreNomNivaa.id(),
                endreNomNivaa.nomNivaa()
            ),
            endreNomNivaa.inputMetadata()
        ));
    }

    public CompletableFuture<SendResult<String, OrgEnhetEvent>> korrigerGyldigFom(String fid, KorrigerGyldigFom korrigerGyldigFom) {
        return eventProducer.sendMessage(new KorrigerGyldigFomEventV1(
            fid,
            new KorrigerGyldigFomEventV1.KorrigerGyldigFomEventData(
                korrigerGyldigFom.id(),
                korrigerGyldigFom.gyldigFom()
            ),
            korrigerGyldigFom.inputMetadata()
        ));
    }

    public CompletableFuture<SendResult<String, OrgEnhetEvent>> endreTilgangsenhet(String fid, EndreTilgangsenhet endreTilgangsenhet) {
        return eventProducer.sendMessage(new TilgangsenhetEndretEventV1(
            fid,
            new TilgangsenhetEndretEventV1.TilgangsenhetEndretEventData(
                endreTilgangsenhet.id(),
                endreTilgangsenhet.tilgangsenhetId()
            ),
            endreTilgangsenhet.inputMetadata()
        ));
    }

}
