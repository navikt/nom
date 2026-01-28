package no.nav.nom.libs.gql;

import no.nav.nom.libs.gql.lucenesearch.LuceneService;
import no.nav.nom.libs.gql.lucenesearch.OrgEnhetStateVisitor;
import no.nav.nom.libs.gql.lucenesearch.RessursStateVisitor;
import no.nav.nom.libs.gql.resolvers.LederResolver;
import no.nav.nom.libs.gql.resolvers.OrgEnhetResolver;
import no.nav.nom.libs.gql.resolvers.OrganiseringResolver;
import no.nav.nom.libs.gql.resolvers.OrgtilknytningResolver;
import no.nav.nom.libs.gql.resolvers.RessursResolver;
import no.nav.nom.libs.gql.resolvers.SearchResolver;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import no.nav.nom.libs.state.orgenhet.NomStateOrgEnhetListener;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.state.orgenhetleder.OrgEnhetLederStateRepository;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.ressurs.NomStateRessursListener;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.graphql.autoconfigure.GraphQlSourceBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AutoConfiguration
public class NomGraphQlAutoConfiguration {

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
                                          NomGraphqlSupportInterface graphqlSupportInterface,
                                          @Autowired(required = false ) NomStateOrgEnhetListener nomStateOrgEnhetListener,
                                          @Autowired(required = false) NomStateRessursListener nomStateRessursListener
    ) {

        var skipVisitorSetup = Arrays.asList(environment.getActiveProfiles()).contains("localgql");

        if(!skipVisitorSetup && (nomStateOrgEnhetListener == null || nomStateRessursListener == null)){
            var clazzNames = Stream.of(NomStateOrgEnhetListener.class, NomStateRessursListener.class).map(Class::getName).collect(Collectors.joining(", "));
            throw new RuntimeException("Missing state listeners %s. Set profile `localgql` if intentional".formatted(clazzNames));
        }

        var out = new LuceneService(ressursStateRepository, environment, orgEnhetStateRepository, graphqlSupportInterface);

        if(nomStateOrgEnhetListener != null) {
            var ignored1 = new OrgEnhetStateVisitor(out, nomStateOrgEnhetListener);
        }
        if(nomStateRessursListener != null) {
            var ignored2 = new RessursStateVisitor(out, nomStateRessursListener);
        }
        return out;
    }

    @Bean
    GraphQlSourceBuilderCustomizer inspectionCustomizer() {
        return source -> source.inspectSchemaMappings(report -> {
            var noUnmappedRegistrations = report.unmappedRegistrations().isEmpty();
            var noUnmappedArgs = report.unmappedArguments().isEmpty();
            var noUnmappedFields = report.unmappedFields().isEmpty();
            if (!(noUnmappedArgs && noUnmappedFields && noUnmappedRegistrations)) {
                throw new RuntimeException(report.toString());
            }
        });
    }

    @Bean
    public static GraphQLConfig getGraphQLConfig() {
        return new GraphQLConfig();
    }
}
