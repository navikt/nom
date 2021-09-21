package no.nav.nom.apps.api.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;

@Service
@RequiredArgsConstructor
public class NavIdentService {

    private final EntityManager entityManager;

    public String generateNavIdent(String navIdentBokstav){
        var seq = entityManager.createNativeQuery("SELECT nextval('NAVIDENT_SEQ')").getSingleResult().toString();
        while (seq.length() < 6){
            seq = "0"+seq;
        }
        return navIdentBokstav.toUpperCase() + seq;
    }

}
