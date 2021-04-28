package no.nav.nom.apps.api.hendelse;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.domain.OrgenhetRepository;
import no.nav.nom.apps.api.dvh.dto.DvhOrganisasjonsenhet;
import no.nav.nom.contract.nom.hendelse.HendelseTo;
import no.nav.nom.contract.nom.hendelse.HendelseTo.Endringstype;
import no.nav.nom.contract.nom.hendelse.Hendelsestyper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrgenhetHendelseService {

    private final ObjectMapper om;
    private final OrgenhetRepository repository;
    private final HendelseProducer hendelseProducer;

    public void handterOrgenhetFraDvh(DvhOrganisasjonsenhet o) {
        var orgenhetOptional = repository.findByAgressoId(o.getAgressoId());

        if (orgenhetOptional.isPresent()) {
            var orgenhet = orgenhetOptional.get();
            var endringer = new HashMap<String, String>();

            if (!orgenhet.getNavn().equals(o.getNavn())) {
                endringer.put("navn", o.getNavn());
            }

            if (!endringer.isEmpty()) {
                var hendelse = createEndreHendelse(orgenhet.getId(), endringer);
                hendelseProducer.produceHendelse(hendelse);
            }
        } else {
            var hendelse = createOpprettHendelse(o.getAgressoId(), o.getNavn());
            hendelseProducer.produceHendelse(hendelse);
        }
    }

    private HendelseTo createEndreHendelse(UUID fid, Map<String, String> endringer) {
        var data = om.createObjectNode();
        data.put("fid", fid.toString());
        endringer.forEach(data::put);

        return HendelseTo.builder()
                .id(UUID.randomUUID())
                .reqId(UUID.randomUUID())
                .type(Hendelsestyper.ORGENHET_V1.getKey())
                .data(data)
                .endringstype(Endringstype.ENDRET)
                .opprettet(LocalDateTime.now())
                .build();
    }

    private HendelseTo createOpprettHendelse(String agressoId, String navn) {
        var data = om.createObjectNode();
        data.put("fid", UUID.randomUUID().toString());
        data.put("agressoId", agressoId);
        data.put("navn", navn);

        return HendelseTo.builder()
                .id(UUID.randomUUID())
                .reqId(UUID.randomUUID())
                .type(Hendelsestyper.ORGENHET_V1.getKey())
                .data(data)
                .endringstype(Endringstype.OPPRETTET)
                .opprettet(LocalDateTime.now())
                .build();
    }
}
