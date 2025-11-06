package no.nav.nom.apps.api.graphql.querystats;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GqlQueryRepository extends JpaRepository<GqlQuery, Integer> {
    Optional<GqlQuery> findByHash(String hash);
}
