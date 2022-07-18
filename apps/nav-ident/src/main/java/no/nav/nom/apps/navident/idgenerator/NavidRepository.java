package no.nav.nom.apps.navident.idgenerator;

import org.springframework.stereotype.Repository;

@Repository
public interface NavidRepository {

    Long getNextval();
}
