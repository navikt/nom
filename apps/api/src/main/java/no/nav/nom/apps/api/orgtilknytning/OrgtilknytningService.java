package no.nav.nom.apps.api.orgtilknytning;

import no.nav.nom.apps.api.orgtilknytning.api.exceptions.FysiskBrukerstedNotFoundException;
import no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil;
import no.nav.nom.apps.api.ressurs.FysiskBrukerstedService;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.utils.common.util.DateUtils;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Service
public class OrgtilknytningService {

    private final OrgtilknytningStateRepository orgtilknytningStateRepository;
    private final FysiskBrukerstedService fysiskBrukerstedService;

    public OrgtilknytningService(OrgtilknytningStateRepository orgtilknytningStateRepository, FysiskBrukerstedService fysiskBrukerstedService) {
        this.orgtilknytningStateRepository = orgtilknytningStateRepository;
        this.fysiskBrukerstedService = fysiskBrukerstedService;
    }

    public Collection<OrgtilknytningState> getAll() {
        return orgtilknytningStateRepository.getAll();
    }

    public Collection<OrgtilknytningState> getByNavIdent(String navident) {
        return orgtilknytningStateRepository.findByNavident(navident);
    }

    public List<OrgtilknytningState> getByEnhet(String id) {
        return getAll().stream()
            .filter(ot -> ot.orgEnhetId().equals(id))
            .toList();
    }

    public <T> Map<String, Collection<T>> getByNavidenter(Set<String> navidenter, Function<OrgtilknytningState, T> transformer) {
        return navidenter.stream().collect(Collectors.toMap(navident -> navident, navident -> {
            var value = this.getByNavIdent(navident);
            return value == null ? Collections.emptyList() : value.stream().map(transformer).toList();
        }));
    }

    public Optional<OrgtilknytningState> findByFid(String fid) {
        return orgtilknytningStateRepository.findByFid(fid);
    }

    public OrgtilknytningState getAktivDolOTForRessurs(String navident) {
        return orgtilknytningStateRepository.findByNavident(navident).stream()
            .filter(OrgtilknytningState::erDagligOppfolging)
            .filter(PeriodeUtil::isCurrentPeriod)
            .findFirst()
            .orElse(null);
    }

    public List<OrgtilknytningState> getActiveOrgtilknytningerByFysiskBrukerstedNr(String fysiskBrukerstedNr) {
        if (isNull(fysiskBrukerstedNr) || !fysiskBrukerstedService.fysiskBrukerstedExists(fysiskBrukerstedNr)) {
            throw FysiskBrukerstedNotFoundException.notFound(fysiskBrukerstedNr);
        }

        return orgtilknytningStateRepository.getAll().stream()
            .filter(orgtilknytningState -> DateUtils.isPeriodActive(orgtilknytningState.gyldigFom(), orgtilknytningState.gyldigTom()))
            .filter(orgtilknytningState -> Objects.equals(fysiskBrukerstedNr, orgtilknytningState.primaryFysiskBrukerstedNummer())
                || (nonNull(orgtilknytningState.additionalFysiskeBrukerstederNummer()) && orgtilknytningState.additionalFysiskeBrukerstederNummer().contains(fysiskBrukerstedNr)))
            .toList();
    }
}
