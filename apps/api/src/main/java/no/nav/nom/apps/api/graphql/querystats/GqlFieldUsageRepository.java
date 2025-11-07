package no.nav.nom.apps.api.graphql.querystats;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GqlFieldUsageRepository extends JpaRepository<GqlFieldUsage, Integer> {
}
