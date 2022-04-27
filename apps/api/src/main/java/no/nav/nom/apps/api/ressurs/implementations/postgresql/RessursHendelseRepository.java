package no.nav.nom.apps.api.ressurs.implementations.postgresql;

import no.nav.nom.apps.api.hendelse.contract.HendelseRepository;
import no.nav.nom.apps.api.postgresql.GenericStorageElement;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RessursHendelseRepository extends HendelseRepository {
    @Query(value = "SELECT * FROM T_HENDELSER WHERE data ->> 'navIdent' like %:seq",
            nativeQuery = true)
    Optional<GenericStorageElement> navIdentSeqIsPresent(String seq);
}
