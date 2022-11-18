package no.nav.nom.apps.api.graphql.organisasjonsenhet;

import graphql.GraphQLError;
import graphql.schema.DataFetchingEnvironment;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.graphql.MappingUtils;
import no.nav.nom.apps.api.graphql.common.ErrorCode;
import no.nav.nom.apps.api.graphql.common.exception.LederNotFoundException;
import no.nav.nom.apps.api.ressurs.UnleashRessursServiceSelector;
import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import no.nav.nom.contract.nom.api.graphql.OrgEnhetsLeder;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import org.springframework.stereotype.Component;

import java.util.List;


@Component("OrgEnhetsLeder")
public class LederResolver {

    private final DvhRepository dvhRepository;
    private final UnleashRessursServiceSelector ressursService;

    public LederResolver(DvhRepository dvhRepository, UnleashRessursServiceSelector ressursService) {
        this.dvhRepository = dvhRepository;
        this.ressursService = ressursService;
    }

    public List<OrgEnhetsLeder> leder(OrgEnhet orgEnhet) {
        return dvhRepository.getLederByAgressoIdAndNiv(orgEnhet.getAgressoId(), orgEnhet.getOrgNiv()).stream()
                .map(leder -> tilOrgEnhetsLeder(
                        ressursService.findByNavident(leder.getNavIdent(), MappingUtils::toRessurs)
                                .orElseThrow(LederNotFoundException::new))
                )
                .toList();
    }

    private OrgEnhetsLeder tilOrgEnhetsLeder(Ressurs leder) {
        return OrgEnhetsLeder.builder()
                .ressurs(leder)
                .build();
    }

    public static GraphQLError handleLederNotFoundException(Throwable throwable, DataFetchingEnvironment environment) {
        if (throwable instanceof LederNotFoundException) {
            return ErrorCode.NOT_FOUND.construct(environment, "Kunne ikke slå opp leder!");
        }
        return null;
    }
}
