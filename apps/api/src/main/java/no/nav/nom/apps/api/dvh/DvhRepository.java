package no.nav.nom.apps.api.dvh;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.nav.nom.contract.dvh.DvhEvent;
import no.nav.nom.contract.dvh.Enhet;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DvhRepository {
    private final List<DvhEvent> events = new LinkedList<>();
    private final ObjectMapper mapper;

    void save(DvhEvent event) {
        events.add(event);
    }

    public Optional<Enhet> findEnhetByNummer(String nummer) {
        return events.stream()
                .filter(e -> Enhet.TYPE.equals(e.getType()))
                .map(e -> toType(e.getData(), Enhet.class))
                .filter(e -> e.getEnhetKode().equals(nummer))
                .findFirst();
    }

    public int size() {
        return events.size();
    }

    @SneakyThrows
    private <T> T toType(JsonNode node, Class<T> type) {
        return mapper.treeToValue(node, type);
    }
}
