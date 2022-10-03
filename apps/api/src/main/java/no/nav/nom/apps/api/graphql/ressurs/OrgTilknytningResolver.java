package no.nav.nom.apps.api.graphql.ressurs;

import no.nav.nom.apps.api.graphql.common.ConverterUtils;
import no.nav.nom.apps.api.graphql.organisasjonsenhet.OrgEnhetResolver;
import no.nav.nom.apps.api.kobling.contract.Kobling;
import no.nav.nom.apps.api.kobling.contract.KoblingService;
import no.nav.nom.apps.api.orgenhet.contract.IOrgenhetService;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.api.graphql.RessursOrgTilknytning;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

@Component
public class OrgTilknytningResolver {

    private final KoblingService koblingService;
    private final IOrgenhetService orgenhetService;

    @Autowired
    public OrgTilknytningResolver(KoblingService koblingService, IOrgenhetService orgenhetService) {
        this.koblingService = koblingService;
        this.orgenhetService = orgenhetService;
    }

    public Map<Ressurs, List<RessursOrgTilknytning>> getOrgTilknytning(Collection<Ressurs> ressurser) {
        return ressurser.stream()
                .collect(
                        toMap(
                                Function.identity(),
                                this::getOrgTilknytning
                        )
                );
    }

    public List<RessursOrgTilknytning> getOrgTilknytning(Ressurs ressurs) {
        return koblingService.getKoblingerByNavIdent(ressurs.getNavIdent()).stream()
                .map(this::fromDvhKobling)
                .toList();
    }

    private RessursOrgTilknytning fromDvhKobling(Kobling kobling) {
        final var orgenhet = orgenhetService.getEnhetByAgressoIdNivaAndName(kobling.getAgressoId(), kobling.getOrgNiv(), null).get();
        final var orgEnhetGqlResponse = OrgEnhetResolver.convertToOrgEnhet(orgenhet);

        return RessursOrgTilknytning.builder()
                .organisasjonsenhet(orgEnhetGqlResponse)
                .orgEnhet(orgEnhetGqlResponse)
                .gyldigFom(kobling.getGyldigFom())
                .gyldigTom(kobling.getGyldigTom().orElse(null))
                .build();
    }
}
