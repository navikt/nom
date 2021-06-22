package no.nav.nom.apps.api.graphql.request;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static no.nav.nom.libs.utils.common.util.StreamUtils.find;

@Component
public class BestillingSetInMemoryRepository {

    private final List<BestillingSet> bestillingSets = new CopyOnWriteArrayList<>();

    public BestillingSet get(UUID bestillingSetId) {
        return find(bestillingSets, r -> r.getBestillingSetId().equals(bestillingSetId));
    }

    public List<BestillingSet> all() {
        return bestillingSets;
    }

    public List<BestillingSet> get(Collection<UUID> bestillingSetIds) {
        return bestillingSets.stream().filter(r -> bestillingSetIds.contains(r.getBestillingSetId())).toList();
    }

    public List<BestillingSet> getReady() {
        return bestillingSets.stream().filter(r -> r.getActivateTime() != null && r.getActivateTime().isBefore(LocalDateTime.now())).toList();
    }

    public void add(BestillingSet bestillingSet) {
        bestillingSets.add(bestillingSet);
    }
}
