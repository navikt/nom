package no.nav.nom.apps.api.hendelse;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.api.domain.RessursRepository;
import no.nav.nom.contract.nom.hendelse.HendelseTo;
import no.nav.nom.contract.nom.hendelse.HendelseTo.Endringstype;
import no.nav.nom.contract.nom.hendelse.Hendelsestyper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RessursHendelseService {

    private final ObjectMapper om;
    private final RessursRepository ressursRepository;
    private final HendelseProducer hendelseProducer;

    public void handterRessursIderFraDvh(String navIdent, String personIdent) {

        val navIdentOptional = ressursRepository.findByNavIdent(navIdent);
        val personIdentOptional = ressursRepository.findByPersonIdent(personIdent);

        val bothIdsPresent = navIdentOptional.isPresent() && personIdentOptional.isPresent();
        val nidOnlyPresent = navIdentOptional.isPresent() && !bothIdsPresent;
        val pidOnlyPresent = personIdentOptional.isPresent() && !bothIdsPresent;

        if (bothIdsPresent) {
            val ressursByNid = navIdentOptional.get();
            val ressursByPid = personIdentOptional.get();
            val alreadySaved = ressursByNid == ressursByPid;
            if (!alreadySaved) {
                log.warn("NavIdent {} for personIdent {} er i konflikt med eksisternde ressurs", navIdent, personIdent);
            }
        } else if (nidOnlyPresent) {
            val fid = navIdentOptional.get().getId();
            val hendelse = createEndreTilNyttPersonnummerHendelse(fid, personIdent);
            hendelseProducer.produceHendelse(hendelse);
        } else if (pidOnlyPresent) {
            val ressursByPid = personIdentOptional.get();
            val eksisterendePid = ressursByPid.getPersonIdent();
            val eksisterendeNid = ressursByPid.getNavIdent();
            log.warn("NavIdent {} for personIdent {} i DVH er i konflikt med eksisterende ressurs med navident {} og personIdent {}",
                    navIdent, personIdent, eksisterendeNid, eksisterendePid);
        } else {
            val hendelse = createOpprettetHendelse(navIdent, personIdent);
            hendelseProducer.produceHendelse(hendelse);
        }
    }

    private HendelseTo createEndreTilNyttPersonnummerHendelse(UUID fid, String personIdent) {
        var data = om.createObjectNode();
        data.put("fid", fid.toString());
        data.put("personIdent", personIdent);

        return HendelseTo.builder()
                .id(UUID.randomUUID())
                .reqId(UUID.randomUUID())
                .type(Hendelsestyper.RESSURS_V1.getKey())
                .data(data)
                .endringstype(Endringstype.ENDRET)
                .opprettet(LocalDateTime.now())
                .build();
    }

    private HendelseTo createOpprettetHendelse(String navIdent, String personIdent) {
        var data = om.createObjectNode();
        data.put("fid", UUID.randomUUID().toString());
        data.put("navIdent", navIdent);
        data.put("personIdent", personIdent);

        return HendelseTo.builder()
                .id(UUID.randomUUID())
                .reqId(UUID.randomUUID())
                .type(Hendelsestyper.RESSURS_V1.getKey())
                .data(data)
                .endringstype(Endringstype.OPPRETTET)
                .opprettet(LocalDateTime.now())
                .build();
    }

}
