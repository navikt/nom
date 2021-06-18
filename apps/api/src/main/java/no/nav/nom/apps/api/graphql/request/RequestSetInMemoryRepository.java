package no.nav.nom.apps.api.graphql.request;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static no.nav.nom.libs.utils.common.util.StreamUtils.find;

@Component
public class RequestSetInMemoryRepository {

    private final List<RequestSet> requestSets = new CopyOnWriteArrayList<>();

    public RequestSet get(UUID requestSetId) {
        return find(requestSets, r -> r.getRequestSetId().equals(requestSetId));
    }

    public List<RequestSet> get(Collection<UUID> requestSetIds) {
        return requestSets.stream().filter(r -> requestSetIds.contains(r.getRequestSetId())).toList();
    }

    public List<RequestSet> getReady() {
        return requestSets.stream().filter(r -> r.getActivateTime() != null && r.getActivateTime().isBefore(LocalDateTime.now())).toList();
    }

    public void add(RequestSet requestSet) {
        requestSets.add(requestSet);
    }
}
