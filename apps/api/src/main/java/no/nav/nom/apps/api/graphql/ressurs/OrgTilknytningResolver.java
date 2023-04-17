package no.nav.nom.apps.api.graphql.ressurs;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.graphql.organisasjonsenhet.OrgEnhetResolver;
import no.nav.nom.apps.api.orgtilknytning.contract.Orgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.contract.OrgtilknytningService;
import no.nav.nom.apps.api.orgenhet.IOrgenhetService;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.api.graphql.RessursOrgTilknytning;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

@Slf4j
@Component

public class OrgTilknytningResolver {

    private final OrgtilknytningService orgtilknytningService;
    private final IOrgenhetService orgenhetService;

    @Autowired
    public OrgTilknytningResolver(OrgtilknytningService orgtilknytningService, IOrgenhetService orgenhetService) {
        this.orgtilknytningService = orgtilknytningService;
        this.orgenhetService = orgenhetService;
    }

    public Map<Ressurs, List<RessursOrgTilknytning>> getOrgTilknytning(Collection<Ressurs> ressurser) {
        return ressurser.stream()
                .collect(toMap(
                        Function.identity(),
                        ressurs -> orgtilknytningService.getKoblingerByNavIdent(ressurs.getNavIdent())
                                .stream()
                                .map(kobling -> this.fromDvhKobling(kobling))
                                .toList()
                ));
    }

    private RessursOrgTilknytning fromDvhKobling(Orgtilknytning orgtilknytning) {
        final var orgenhet = orgenhetService.getById(orgtilknytning.getOrgEnhetId()).get();
        final var orgEnhetGqlResponse = OrgEnhetResolver.convertToOrgEnhet(orgenhet);

        return RessursOrgTilknytning.builder()
                .organisasjonsenhet(orgEnhetGqlResponse)
                .orgEnhet(orgEnhetGqlResponse)
                .gyldigFom(orgtilknytning.getGyldigFom())
                .gyldigTom(orgtilknytning.getGyldigTom().orElse(null))
                .build();
    }
}
