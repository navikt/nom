package no.nav.nom.apps.gql
;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import no.nav.nom.apps.gql.resolvers.LederResolver;
import no.nav.nom.apps.gql.resolvers.OrgEnhetResolver;
import no.nav.nom.apps.gql.resolvers.OrganiseringResolver;
import no.nav.nom.apps.gql.resolvers.OrgtilknytningResolver;
import no.nav.nom.apps.gql.resolvers.RessursResolver;
import no.nav.nom.apps.gql.resolvers.SearchResolver;
import no.nav.nom.contract.nom.state.OrgEnhetLederState;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepositoryImpl;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepositoryImpl;
import no.nav.nom.libs.state.orgenhetleder.OrgEnhetLederStateRepository;
import no.nav.nom.libs.state.orgenhetleder.OrgEnhetLederStateRepositoryImpl;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepositoryImpl;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepositoryImpl;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;

import javax.swing.JOptionPane;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@SpringBootApplication
public class NomGqlApplication {

    private final static String STATE_OBJECTS_PATH = "state-objects-path";

    static private Path getPathByDialog(){
        var input = JOptionPane.showInputDialog("Provide path for json file containing state objects to be loaded").trim();
        var p = Path.of(input);
        var okPath = input.endsWith(".json") && Files.exists(p);
        if(!okPath){
            throw new RuntimeException("Bad path to JSON");
        }
        return p;
    }

	static void main(String[] args) {
        var app = new SpringApplication(NomGqlApplication.class, FileSourcedRepositories.class, ConcreteGraphqlController.class, GraphQlExceptionHandler.class);

        var pathProvided = args.length > 0 && args[0] != null;

        app.setDefaultProperties(Map.of(STATE_OBJECTS_PATH, pathProvided ? args[0] : getPathByDialog().toString()));

        app.run();
	}

    private static class FileSourcedRepositories {
        private final RessursStateRepositoryImpl ressursStateRepository;
        private final OrgEnhetStateRepositoryImpl orgEnhetStateRepository;
        private final OrgEnhetLederStateRepositoryImpl orgEnhetLederStateRepository;
        private final OrgtilknytningStateRepositoryImpl orgtilknytningStateRepository;
        private final OrganiseringStateRepositoryImpl organiseringStateRepository;

        public FileSourcedRepositories(ObjectMapper objectMapper, Environment env) {
            this.ressursStateRepository = new RessursStateRepositoryImpl();
            this.orgEnhetStateRepository = new OrgEnhetStateRepositoryImpl();
            this.orgEnhetLederStateRepository = new OrgEnhetLederStateRepositoryImpl();
            this.orgtilknytningStateRepository = new OrgtilknytningStateRepositoryImpl();
            this.organiseringStateRepository = new OrganiseringStateRepositoryImpl();

            var stateObjects = this.readStateObjectsFromFile(env.getProperty(STATE_OBJECTS_PATH), objectMapper );

            for(var so : stateObjects){
                switch(so){
                    case RessursState rs -> {
                        ressursStateRepository.update(rs);
                    }
                    case OrgEnhetLederState oels -> {
                        orgEnhetLederStateRepository.update(oels.fid(), oels);
                    }
                    case OrgEnhetState oes -> {
                        orgEnhetStateRepository.update(oes);
                    }
                    case OrganiseringState os -> {
                        organiseringStateRepository.update(os);
                    }
                    case OrgtilknytningState oets -> {
                        orgtilknytningStateRepository.update(oets.fid(), oets);
                    }
                    default -> {
                        throw new  RuntimeException("Bad state object");
                    }
                }
            }
        }

        @Bean
        public OrganiseringStateRepository organiseringStateRepository() {
            return organiseringStateRepository;
        }

        @Bean
        public OrgtilknytningStateRepository orgtilknytningStateRepository() {
            return orgtilknytningStateRepository;
        }


        @Bean
        public RessursStateRepository ressursStateRepository(){
            return this.ressursStateRepository;
        }

        @Bean
        public OrgEnhetLederStateRepository orgEnhetLederStateRepository(){
            return this.orgEnhetLederStateRepository;
        }

        @Bean
        public OrgEnhetStateRepository orgEnhetStateRepository(){
            return this.orgEnhetStateRepository;
        }


        @Bean
        public GraphqlSupportInterface graphqlSupport() {
            return GraphqlSupportInterface.dummyImplementation();
        }

        private List<Object> readStateObjectsFromFile(String pathProperty, ObjectMapper objectMapper){
            try {
                var s = Files.readString(Path.of(pathProperty));
                var tree = objectMapper.readTree(s);
                if(tree.isArray()){
                    var arr = (ArrayNode) tree;
                    return arr.valueStream().map(it -> stateObjectFromJson(it, objectMapper)).toList();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            throw new RuntimeException("Bad state");
        }


        private Object stateObjectFromJson(JsonNode objectNode, ObjectMapper objectMapper){
            var stateClassName = objectNode.get("stateClassName").asText();
            var stateData = objectNode.get("stateData");

            var possibleStateClasses = List.of(RessursState.class, OrgEnhetState.class, OrgtilknytningState.class, OrgEnhetLederState.class, OrganiseringState.class);
            for(var c : possibleStateClasses){
                if(c.getName().equals(stateClassName)){
                    return objectMapper.convertValue(stateData, c);
                }
            }
            throw new RuntimeException("Bad state object");
        }
    }

    @Controller
    private static class ConcreteGraphqlController extends AbstractNomGraphQlController {

        public ConcreteGraphqlController(
            OrgEnhetResolver orgEnhetResolver,
            OrgtilknytningResolver orgtilknytningResolver,
            OrganiseringResolver organiseringResolver,
            LederResolver lederResolver,
            RessursResolver ressursResolver,
            SearchResolver searchResolver,
            GraphqlSupportInterface graphqlSupportInterface
        ) {
            super(
                orgEnhetResolver,
                orgtilknytningResolver,
                organiseringResolver,
                lederResolver,
                ressursResolver,
                searchResolver,
                graphqlSupportInterface
            );
        }
    }

}
