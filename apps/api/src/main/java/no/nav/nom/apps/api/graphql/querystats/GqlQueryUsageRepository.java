package no.nav.nom.apps.api.graphql.querystats;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GqlQueryUsageRepository extends JpaRepository<GqlQueryUsage, Integer> {
    List<GqlQueryUsage> findByHash(String hash);
    List<GqlQueryUsage> findByConsumer(String consumer);
}
