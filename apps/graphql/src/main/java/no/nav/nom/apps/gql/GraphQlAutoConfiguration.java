package no.nav.nom.apps.gql;

import no.nav.nom.apps.gql.lucenesearch.LuceneService;
import no.nav.nom.apps.gql.lucenesearch.OrgEnhetStateVisitor;
import no.nav.nom.apps.gql.lucenesearch.RessursStateVisitor;
import no.nav.nom.apps.gql.resolvers.LederResolver;
import no.nav.nom.apps.gql.resolvers.OrgEnhetResolver;
import no.nav.nom.apps.gql.resolvers.OrganiseringResolver;
import no.nav.nom.apps.gql.resolvers.OrgtilknytningResolver;
import no.nav.nom.apps.gql.resolvers.RessursResolver;
import no.nav.nom.apps.gql.resolvers.SearchResolver;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import no.nav.nom.libs.state.orgenhet.NomStateOrgEnhetListener;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.state.orgenhetleder.OrgEnhetLederStateRepository;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.ressurs.NomStateRessursListener;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@AutoConfiguration
public class GraphQlAutoConfiguration {

    @Bean
    OrgEnhetResolver orgEnhetResolver(OrgEnhetStateRepository orgEnhetStateRepository) {
        return new OrgEnhetResolver(orgEnhetStateRepository);
    }

    @Bean
    OrgtilknytningResolver orgtilknytningResolver(
        OrgtilknytningStateRepository orgtilknytningStateRepository,
        RessursStateRepository ressursStateRepository,
        OrgEnhetStateRepository orgEnhetStateRepository
    ) {
        return new OrgtilknytningResolver(orgtilknytningStateRepository, ressursStateRepository, orgEnhetStateRepository);
    }

    @Bean
    OrganiseringResolver organiseringResolver(OrganiseringStateRepository organiseringStateRepository) {
        return new OrganiseringResolver(organiseringStateRepository);
    }

    @Bean
    LederResolver lederResolver(
        OrgEnhetLederStateRepository orgEnhetLederStateRepository,
        RessursStateRepository ressursStateRepository,
        OrgEnhetStateRepository orgEnhetStateRepository,
        OrgtilknytningStateRepository orgtilknytningStateRepository,
        OrganiseringStateRepository organiseringStateRepository
    ) {
        return new LederResolver(
            orgEnhetLederStateRepository,
            ressursStateRepository,
            orgEnhetStateRepository,
            orgtilknytningStateRepository,
            organiseringStateRepository
        );
    }

    @Bean
    RessursResolver ressursResolver(RessursStateRepository ressursStateRepository) {
        return new RessursResolver(ressursStateRepository);
    }

    @Bean
    SearchResolver searchResolver(LuceneService luceneService, RessursStateRepository ressursStateRepository, OrgEnhetStateRepository orgEnhetStateRepository) {
        return new SearchResolver(luceneService, ressursStateRepository, orgEnhetStateRepository);
    }



    @Bean
    public static LuceneService getLuceneService(RessursStateRepository ressursStateRepository, Environment environment,
                                          OrgEnhetStateRepository orgEnhetStateRepository,
                                          GraphqlSupportInterface graphqlSupportInterface,
                                          @Autowired(required = false) NomStateOrgEnhetListener nomStateOrgEnhetListener,
                                          @Autowired(required = false) NomStateRessursListener nomStateRessursListener
    ) {
        var out = new LuceneService(ressursStateRepository, environment, orgEnhetStateRepository, graphqlSupportInterface);
        // todo, only allow non-init if the appropriate profile is set
        if(nomStateOrgEnhetListener != null) {
            var ignored1 = new OrgEnhetStateVisitor(out, nomStateOrgEnhetListener);
        }
        // todo, only allow non-init if the appropriate profile is set
        if(nomStateRessursListener != null) {
            var ignored2 = new RessursStateVisitor(out, nomStateRessursListener);
        }
        return out;
    }

    @Bean
    public static GraphQLConfig getGraphQLConfig() {
        return new GraphQLConfig();
    }
}
