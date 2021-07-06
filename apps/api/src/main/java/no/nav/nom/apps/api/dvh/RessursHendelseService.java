package no.nav.nom.apps.api.dvh;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.api.domain.RessursService;
import no.nav.nom.apps.api.dvh.dto.DvhRessurs;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RessursHendelseService {

    private final RessursService ressursService;

    public void handterRessursIderFraDvh(List<DvhRessurs> dvhRessurs) {

        val bulkSize = 10000;
        for(int i = 0; i<dvhRessurs.size(); i+=bulkSize){
            val selection = dvhRessurs.subList(i, Math.min(i + bulkSize, dvhRessurs.size()));
            val ressursByNavIdent = ressursService.findByNavIdentBulk(selection.stream().map(DvhRessurs::getNavIdent).toList());
            val ressursByPersonIdent = ressursService.findByPersonIdentBulk(selection.stream().map(DvhRessurs::getPersonIdent).toList());

            val opprettInserts = new HashMap<String, String>();
            val endreinserts = new HashMap<UUID, String>();

            selection.forEach( ressurs -> {
                val bothIdsPresent = ressursByNavIdent.containsKey(ressurs.getNavIdent()) && ressursByPersonIdent.containsKey(ressurs.getPersonIdent());
                val nidOnlyPresent = ressursByNavIdent.containsKey(ressurs.getNavIdent()) && !bothIdsPresent;
                val pidOnlyPresent = ressursByPersonIdent.containsKey(ressurs.getPersonIdent())  && !bothIdsPresent;

                if (bothIdsPresent) {
                    val ressursByNid = ressursByNavIdent.get(ressurs.getNavIdent());
                    val ressursByPid = ressursByPersonIdent.get(ressurs.getPersonIdent());
                    val alreadySaved = (ressursByNid.getNavIdent().equals(ressursByPid.getNavIdent()) && ressursByNid.getPersonIdent().equals(ressursByPid.getPersonIdent()));
                    if (!alreadySaved) {
                        log.warn("NavIdent {} er i konflikt med eksisterende ressurs", ressurs.getNavIdent());
                    }
                } else if (nidOnlyPresent) {
                    val fid = ressursByNavIdent.get(ressurs.getNavIdent()).getId();
                    endreinserts.put(fid, ressurs.getPersonIdent());
                } else if (pidOnlyPresent) {
                    val ressursByPid = ressursByPersonIdent.get(ressurs.getPersonIdent());
                    val eksisterendeNid = ressursByPid.getNavIdent();
                    log.warn("NavIdent {} i DVH er i konflikt med eksisterende ressurs med navident {}",
                            ressurs.getNavIdent(), eksisterendeNid);
                } else {
                    opprettInserts.put(ressurs.getNavIdent(), ressurs.getPersonIdent());
                }
                    }
            );

            ressursService.createOpprettetHendelser(opprettInserts);
            ressursService.createEndreTilNyttPersonnummerHendelser(endreinserts);
        }
    }
}
