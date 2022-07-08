package no.nav.nom.apps.api.orgenhet.implementations.dvh.v2;

import no.nav.nom.apps.api.postgresql.GenericStorage;
import no.nav.nom.apps.api.postgresql.GenericStorageElement;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ValidationRepository extends GenericStorage {

    @Query(value =  "SELECT * FROM T_GENERIC_STORAGE a " +
            "WHERE a.document ->> 'validationCode' IN :validationCodes",
            nativeQuery = true)
    List<GenericStorageElement> findAllInValidationCode(List<String> validationCodes);
}
