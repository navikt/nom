package no.nav.nom.apps.api.repository;

import lombok.RequiredArgsConstructor;
import no.nav.nom.apps.api.repository.hendelse.HendelseRepository;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;

@Service
@RequiredArgsConstructor
public class NavIdentService {

    private final EntityManager entityManager;
    private final HendelseRepository hendelseRepository;

    public String generateNavIdent(String navIdentBokstav){
        var seq = "";
        do {
            seq = entityManager.createNativeQuery("SELECT nextval('NAVIDENT_SEQ')").getSingleResult().toString();
            while (seq.length() < 6) {
                seq = "0" + seq;
            }
        }while(hendelseRepository.navIdentSeqIsPresent(seq).isPresent());
        return navIdentBokstav.toUpperCase() + seq;
    }

}
