package no.nav.nom.apps.api.orgenhet.eventsourcing.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.PrefixedUlidCreator;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.AvvikleOrgEnhet;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.EndreNomNivaa;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.EndreOrgEnhetsType;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.KorrigerOrgEnhet;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.OpprettOrgEnhet;
import no.nav.nom.apps.api.orgenhet.implementations.dvh.v2.DvhOrgenhetV2;
import no.nav.nom.contract.nom.eventsource.NomNivaaEndretEventV1;
import no.nav.nom.contract.nom.eventsource.OrgEnhetAvvikletEventV1;
import no.nav.nom.contract.nom.eventsource.OrgEnhetEvent;
import no.nav.nom.contract.nom.eventsource.OrgEnhetKorrigertEventV1;
import no.nav.nom.contract.nom.eventsource.OrgEnhetOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.OrgEnhetTypeEndretEventV1;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class OrgEnhetCommandService {

    private final OrgEnhetEventProducer eventProducer;
    private final ObjectMapper objectMapper;
    public OrgEnhetCommandService(OrgEnhetEventProducer eventProducer, ObjectMapper objectMapper) {
        this.eventProducer = eventProducer;
        this.objectMapper = objectMapper;
    }

    public String opprettOrgEnhet(OpprettOrgEnhet opprettOrgEnhet) {
        String fid = PrefixedUlidCreator.createOrgEnhetUlid();
        opprettOrgEnhet(fid, opprettOrgEnhet);
        return fid;
    }

    private CompletableFuture<SendResult<String, OrgEnhetEvent>> opprettOrgEnhet(String fid, OpprettOrgEnhet opprettOrgEnhet) {
        return eventProducer.sendMessage(getOpprettEvent(fid, opprettOrgEnhet));
    }

    private OrgEnhetOpprettetEventV1 getOpprettEvent(String fid, OpprettOrgEnhet opprettOrgEnhet) {
        OrgEnhetOpprettetEventV1.OrgEnhetOpprettetAdditionalAgressoData additionalAgressoData = null;
        if (opprettOrgEnhet.inputMetadata().inputInterfaceMethodName().equals("Datavarehus")) {
            var input = objectMapper.valueToTree(opprettOrgEnhet.inputMetadata().inputInterfaceMethodArguments());
            try {
                var dvhOrgenhet2 = new DvhOrgenhetV2(input.isArray() ? input.get(0): input);
                additionalAgressoData = new OrgEnhetOpprettetEventV1.OrgEnhetOpprettetAdditionalAgressoData(
                    dvhOrgenhet2.getAgressoId(),
                    dvhOrgenhet2.getNavn(),
                    dvhOrgenhet2.getOrgNiv(),
                    Optional.ofNullable(dvhOrgenhet2.getAgressoOrgenhetType()),
                    dvhOrgenhet2.getUniqueAgressoId()
                );
            } catch (Exception e) {
                log.error("Problem converting orgEnhet info from dvh", e);
                log.info("dvh data: {}", input.toPrettyString());
            }
        }
        return new OrgEnhetOpprettetEventV1(
                fid,
                new OrgEnhetOpprettetEventV1.OrgEnhetOpprettetEventData(
                        opprettOrgEnhet.id(),
                        opprettOrgEnhet.navn(),
                        opprettOrgEnhet.gyldigFom(),
                        opprettOrgEnhet.overordnetOrgEnhetFid(),
                        opprettOrgEnhet.lederRessursFid()
                ),
                opprettOrgEnhet.inputMetadata(),
                additionalAgressoData
        );
    }

    public CompletableFuture<SendResult<String, OrgEnhetEvent>> avikleOrgEnhet(String fid, AvvikleOrgEnhet avvikleOrgEnhet) {
        return eventProducer.sendMessage(new OrgEnhetAvvikletEventV1(
                fid,
                new OrgEnhetAvvikletEventV1.OrgEnhetAvvikletEventData(
                        avvikleOrgEnhet.gyldigTom()
                ),
                avvikleOrgEnhet.inputMetadata()
        ));
    }

    // Alle verdier i KorrigerOrgEnhet objektet erstatter eksisterende verdier. Også tomme.
    public CompletableFuture<SendResult<String, OrgEnhetEvent>> korrigerOrgEnhet(String fid, KorrigerOrgEnhet korrigerOrgEnhet) {
        OrgEnhetKorrigertEventV1.OrgEnhetKorrigertAdditionalAgressoData additionalAgressoData = null;
        if (korrigerOrgEnhet.inputMetadata().inputInterfaceMethodName().equals("Datavarehus")) {
            var input = objectMapper.valueToTree(korrigerOrgEnhet.inputMetadata().inputInterfaceMethodArguments());
            var dvhOrgenhet2 = new DvhOrgenhetV2(input.isArray() ? input.get(0): input);
            additionalAgressoData = new OrgEnhetKorrigertEventV1.OrgEnhetKorrigertAdditionalAgressoData(
                    dvhOrgenhet2.getAgressoId(),
                    dvhOrgenhet2.getNavn(),
                    dvhOrgenhet2.getOrgNiv(),
                    Optional.ofNullable(dvhOrgenhet2.getAgressoOrgenhetType()),
                    dvhOrgenhet2.getUniqueAgressoId()
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
                endreOrgEnhetsType.orgEnhetsType()
            ),
            endreOrgEnhetsType.inputMetadata()
        ));
    }

    public CompletableFuture<SendResult<String, OrgEnhetEvent>> endreNomNivaa(String fid, EndreNomNivaa endreNomNivaa) {
        return eventProducer.sendMessage(new NomNivaaEndretEventV1(
            fid,
            new NomNivaaEndretEventV1.NomNivaaEndretEventData(
                endreNomNivaa.nomNivaa()
            ),
            endreNomNivaa.inputMetadata()
        ));
    }

}
