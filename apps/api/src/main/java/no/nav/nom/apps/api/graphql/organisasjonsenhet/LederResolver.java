package no.nav.nom.apps.api.graphql.organisasjonsenhet;

import graphql.GraphQLError;
import graphql.schema.DataFetchingEnvironment;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.graphql.MappingUtils;
import no.nav.nom.apps.api.graphql.common.ErrorCode;
import no.nav.nom.apps.api.graphql.common.exception.LederNotFoundException;
import no.nav.nom.apps.api.ressurs.eventsourcing.state.RessursStateRepository;
import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import no.nav.nom.contract.nom.api.graphql.OrgEnhetsLeder;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import org.springframework.stereotype.Component;

import java.util.List;


@Component("OrgEnhetsLeder")
public class LederResolver {

    private final DvhRepository dvhRepository;
    private final RessursStateRepository ressursStateRepository;

    public LederResolver(DvhRepository dvhRepository, RessursStateRepository ressursStateRepository) {
        this.dvhRepository = dvhRepository;
        this.ressursStateRepository = ressursStateRepository;
    }

    public List<OrgEnhetsLeder> leder(OrgEnhet orgEnhet) {
        return dvhRepository.getLederByAgressoIdAndNiv(orgEnhet.getAgressoId(), orgEnhet.getOrgNiv())
                .stream()
                .map(leder -> tilOrgEnhetsLeder(ressursStateRepository.findByNavident(leder.getNavIdent(), MappingUtils::toRessurs)
                                                        .orElseThrow(LederNotFoundException::new)))
                .toList();
    }

    private OrgEnhetsLeder tilOrgEnhetsLeder(Ressurs leder) {
        return OrgEnhetsLeder.builder().ressurs(leder).build();
    }

    public static GraphQLError handleLederNotFoundException(Throwable throwable, DataFetchingEnvironment environment) {
        if (throwable instanceof LederNotFoundException) {
            return ErrorCode.NOT_FOUND.construct(environment, "Kunne ikke slå opp leder!");
        }
        return null;
    }
}
