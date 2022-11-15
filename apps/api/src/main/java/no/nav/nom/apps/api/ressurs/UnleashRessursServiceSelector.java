package no.nav.nom.apps.api.ressurs;

import com.github.f4b6a3.ulid.Ulid;
import io.getunleash.Unleash;
import lombok.AllArgsConstructor;
import no.nav.nom.apps.api.eventsourcing.state.RessursStateService;
import no.nav.nom.apps.api.ressurs.contract.IRessurs;
import no.nav.nom.apps.api.ressurs.contract.IRessursService;
import no.nav.nom.apps.api.ressurs.implementations.RessursService;
import no.nav.nom.contract.nom.state.RessursState;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
public class UnleashRessursServiceSelector implements IRessursService<IRessurs> {

    private static final String UNLEASH_TOGGLE = "nom.ressurs.eventsourcing_toggle";
    private Unleash unleash;
    private final RessursService ressursService;
    private final RessursStateService ressursStateService;

    @Override
    public Optional<IRessurs> findByFid(UUID fid) {
        if (eventSourceToggle()) {
            return ressursStateService.findByFid(fid).map(Ressurs::new);
        } else {
            return ressursService.findByFid(fid);
        }
    }

    @Override
    public Optional<IRessurs> findByPersonident(String personident) {
        if (eventSourceToggle()) {
            return ressursStateService.findByPersonident(personident).map(Ressurs::new);
        } else {
            return ressursService.findByPersonident(personident);
        }
    }

    @Override
    public <T> Optional<T> findByNavident(String navident, Function<IRessurs, T> converter) {
        if (eventSourceToggle()) {
            return ressursStateService.findByNavident(navident).map(Ressurs::new).map(converter);
        } else {
            return ressursService.findByNavident(navident).map(converter);
        }
    }

    @Override
    public Collection<IRessurs> getAll() {
        if (eventSourceToggle()) {
            return ressursStateService.getAll().stream().map(Ressurs::new).collect(Collectors.toSet());
        } else {
            return ressursService.getAll();
        }
    }

    @Override
    public List<IRessurs> matchByNavident(String navident) {
        if (eventSourceToggle()) {
            return ressursStateService.matchByNavident(navident).stream().map(Ressurs::new).collect(Collectors.toList());
        } else {
            return ressursService.matchByNavident(navident);
        }
    }

    @Override
    public Map<String, IRessurs> findByPersonidentBulk(List<String> personIdenter) {
        if (eventSourceToggle()) {
            return ressursStateService.findByPersonidentBulk(personIdenter).entrySet().stream()
                    .map(entry -> Map.entry(entry.getKey(), new Ressurs(entry.getValue())))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        } else {
            return ressursService.findByPersonidentBulk(personIdenter);
        }
    }

    @Override
    public Map<String, IRessurs> findByNavidentBulk(List<String> navIdenter) {
        if (eventSourceToggle()) {
            return ressursStateService.findByNavidentBulk(navIdenter).entrySet().stream()
                    .map(entry -> Map.entry(entry.getKey(), new Ressurs(entry.getValue())))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        } else {
            return ressursService.findByNavidentBulk(navIdenter);
        }
    }

    private boolean eventSourceToggle() {
        return unleash.isEnabled(UNLEASH_TOGGLE);
    }

    class Ressurs implements IRessurs {
        RessursState ressursState;

        Ressurs(RessursState ressursState) {
            this.ressursState = ressursState;
        }

        @Override
        public UUID getId() {
            return Ulid.from(ressursState.fid().split("-")[1]).toUuid();
        }

        @Override
        public String getPersonIdent() {
            return ressursState.personident();
        }

        @Override
        public String getNavIdent() {
            return ressursState.navident();
        }

        @Override
        public Optional<String> getVisningsfornavn() {
            return Optional.of(ressursState.visningsnavn().fornavn());
        }

        @Override
        public Optional<String> getVisningsmellomnavn() {
            return ressursState.visningsnavn().mellomnavn();
        }

        @Override
        public  Optional<String> getVisningsetternavn() {
            return Optional.of(ressursState.visningsnavn().etternavn());
        }

        @Override
        public  String getSektor() {
            return ressursState.sektor().toString();
        }

        @Override
        public LocalDate getSluttdato() {
            return ressursState.sluttdato().orElse(null);
        }
    }
}
