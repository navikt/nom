package no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OrgtilknytningCommandService {

    private final no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.OrgtilknytningEventProducer eventProducer;
    private final ObjectMapper objectMapper;
    public OrgtilknytningCommandService(no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.OrgtilknytningEventProducer eventProducer, ObjectMapper objectMapper) {
        this.eventProducer = eventProducer;
        this.objectMapper = objectMapper;
    }

    /* TODO: Implement commands */

    /*
    public String opprettOrgEnhet(OpprettOrgEnhet opprettOrgEnhet) {
        String fid = PrefixedUlidCreator.createOrgEnhetUlid();
        opprettOrgEnhet(fid, opprettOrgEnhet);
        return fid;
    }

    private ListenableFuture<SendResult<String, OrgtilknytningEvent>> opprettOrgEnhet(String fid, OpprettOrgEnhet opprettOrgEnhet) {
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
                        opprettOrgEnhet.lederRessursFid(),
                        opprettOrgEnhet.nomNivaa()
                ),
                opprettOrgEnhet.inputMetadata(),
                additionalAgressoData
        );
    }

    public ListenableFuture<SendResult<String, OrgEnhetEvent>> avikleOrgEnhet(String fid, AvvikleOrgEnhet avvikleOrgEnhet) {
        return eventProducer.sendMessage(new OrgEnhetAvvikletEventV1(
                fid,
                new OrgEnhetAvvikletEventV1.OrgEnhetAvvikletEventData(
                        avvikleOrgEnhet.gyldigTom()
                ),
                avvikleOrgEnhet.inputMetadata()
        ));
    }

    // Alle verdier i KorrigerOrgEnhet objektet erstatter eksisterende verdier. Også tomme.
    public ListenableFuture<SendResult<String, OrgEnhetEvent>> korrigerOrgEnhet(String fid, KorrigerOrgEnhet korrigerOrgEnhet) {
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
                        korrigerOrgEnhet.lederRessursFid(),
                        korrigerOrgEnhet.nomNivaa()
                ),
                korrigerOrgEnhet.inputMetadata(),
                additionalAgressoData
                ));
    }
    */
}
