package no.nav.nom.apps.navident.idgenerator;

import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import java.math.BigInteger;

@Component
@RequiredArgsConstructor
public class NavidRepositoryImpl implements NavidRepository {

    private final EntityManager entityManager;

    public Long getNextval() {
        val query = entityManager.createNativeQuery("SELECT nextval('NAVIDENT_SEQ')");

        return ((BigInteger)query.getSingleResult()).longValue();
    }
}
