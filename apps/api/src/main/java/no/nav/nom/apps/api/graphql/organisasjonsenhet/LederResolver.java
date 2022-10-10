package no.nav.nom.apps.api.graphql.organisasjonsenhet;

import graphql.GraphQLError;
import graphql.schema.DataFetchingEnvironment;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.graphql.common.ErrorCode;
import no.nav.nom.apps.api.graphql.common.exception.LederNotFoundException;
import no.nav.nom.apps.api.ressurs.contract.IRessursService;
import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import no.nav.nom.contract.nom.api.graphql.OrgEnhetsLeder;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
public class LederResolver {

    private final DvhRepository dvhRepository;
    private final IRessursService ressursService;

    public LederResolver(DvhRepository dvhRepository, IRessursService ressursService) {
        this.dvhRepository = dvhRepository;
        this.ressursService = ressursService;
    }

    public List<OrgEnhetsLeder> leder(OrgEnhet orgEnhet) {
        return dvhRepository.getLederByAgressoIdAndNiv(orgEnhet.getAgressoId(), orgEnhet.getOrgNiv()).stream()
                .map(leder -> tilOrgEnhetsLeder(
                        ressursService.findByNavIdent(leder.getNavIdent(), ressursService::convertToGraphQlResponse)
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
