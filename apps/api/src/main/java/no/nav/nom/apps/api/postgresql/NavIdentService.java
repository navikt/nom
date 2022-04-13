package no.nav.nom.apps.api.postgresql;

import lombok.RequiredArgsConstructor;
import no.nav.nom.apps.api.ressurs.implementations.postgresql.RessursHendelseRepository;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;

/*
    FORELØPIG IKKE I BRUK.
*/
@Service
@RequiredArgsConstructor
public class NavIdentService {

    private final EntityManager entityManager;
    private final RessursHendelseRepository ressursHendelseRepository;

    public String generateNavIdent(String navIdentBokstav){
        var seq = "";
        do {
            seq = entityManager.createNativeQuery("SELECT nextval('NAVIDENT_SEQ')").getSingleResult().toString();
            while (seq.length() < 6) {
                seq = "0" + seq;
            }
        }while(ressursHendelseRepository.navIdentSeqIsPresent(seq).isPresent());
        return navIdentBokstav.toUpperCase() + seq;
    }

}
