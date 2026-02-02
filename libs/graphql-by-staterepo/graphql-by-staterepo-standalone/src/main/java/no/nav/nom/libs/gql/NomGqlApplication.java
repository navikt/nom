package no.nav.nom.libs.gql;

import no.nav.nom.contract.nom.state.OrgEnhetLederState;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.gql.resolvers.LederResolver;
import no.nav.nom.libs.gql.resolvers.OrgEnhetResolver;
import no.nav.nom.libs.gql.resolvers.OrganiseringResolver;
import no.nav.nom.libs.gql.resolvers.OrgtilknytningResolver;
import no.nav.nom.libs.gql.resolvers.RessursResolver;
import no.nav.nom.libs.gql.resolvers.SearchResolver;
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@SpringBootApplication
public class NomGqlApplication {

    private final static String STATE_OBJECTS_PATH = "state-objects-path";

    static private File getFileByDialog(){
        class Contain<T>{
            public T value;
            Contain(T value){
                this.value = value;
            }
        }
        final var file = new Contain<File>(null);
        {
            var dialog = new JDialog();
            var inputBox = new JPanel(new BorderLayout());

            {
                dialog.setModal(true);
                dialog.setSize(800, 200);
                dialog.setLayout(new GridLayout(4,2));
            }

            var jLabel1 = new JLabel("No JSON file specified as first program arg.");
            var jLabel2 = new JLabel("Provide JSON file containing state objects to be loaded");
            var fileDialogButton = new JButton("open file dialog");
            var selectStringButton = new JButton("use input path");
            var inputField = new JTextField();
            {
                inputField.setText("... provide path");
                fileDialogButton.addActionListener(ae -> {
                    var x = new JFileChooser();
                    x.addChoosableFileFilter(new FileNameExtensionFilter("json files", "json"));
                    var result = x.showOpenDialog(null);
                    if (result == JFileChooser.APPROVE_OPTION) {
                        file.value = x.getSelectedFile();
                        dialog.setVisible(false);
                    }
                });
                selectStringButton.addActionListener(ae -> {
                    var txt = inputField.getText();
                    var p = Path.of(txt);
                    var okPath = txt.endsWith(".json") && Files.exists(p);
                    if (!okPath) {
                        throw new RuntimeException("Bad path to JSON");
                    } else {
                        file.value = p.toFile();
                    }
                    dialog.setVisible(false);
                });
                dialog.add(jLabel1);
                dialog.add(jLabel2);
                dialog.add(fileDialogButton);

                inputBox.add(inputField, BorderLayout.CENTER);
                inputBox.add(selectStringButton, BorderLayout.EAST);
                dialog.add(inputBox);
                dialog.getRootPane().setSize(700,125);
            }

            dialog.setVisible(true);
            if (file.value == null) {
                throw new RuntimeException("Bad path to JSON");
            }
            return file.value;
        }
    }

	static void main(String[] args) {

        var app = new SpringApplication(NomGqlApplication.class, FileSourcedRepositories.class, ConcreteGraphqlController.class, GraphQlExceptionHandler.class, NomGraphQlAutoConfiguration.class);

        var pathProvided = args.length > 0 && args[0] != null;

        app.setDefaultProperties(Map.of(STATE_OBJECTS_PATH, pathProvided ? args[0] : getFileByDialog().getAbsolutePath()));
        app.setAdditionalProfiles("localgql");

        app.run();
	}

    private static class FileSourcedRepositories {
        private final RessursStateRepositoryImpl ressursStateRepository;
        private final OrgEnhetStateRepositoryImpl orgEnhetStateRepository;
        private final OrgEnhetLederStateRepositoryImpl orgEnhetLederStateRepository;
        private final OrgtilknytningStateRepositoryImpl orgtilknytningStateRepository;
        private final OrganiseringStateRepositoryImpl organiseringStateRepository;

        public FileSourcedRepositories(JsonMapper jsonMapper, Environment env) {
            this.ressursStateRepository = new RessursStateRepositoryImpl();
            this.orgEnhetStateRepository = new OrgEnhetStateRepositoryImpl();
            this.orgEnhetLederStateRepository = new OrgEnhetLederStateRepositoryImpl();
            this.orgtilknytningStateRepository = new OrgtilknytningStateRepositoryImpl();
            this.organiseringStateRepository = new OrganiseringStateRepositoryImpl();

            var stateObjects = this.readStateObjectsFromFile(env.getProperty(STATE_OBJECTS_PATH), jsonMapper);

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
        public NomGraphqlSupportInterface graphqlSupport() {
            return NomGraphqlSupportInterface.dummyImplementation();
        }

        private List<Object> readStateObjectsFromFile(String pathProperty, JsonMapper jsonMapper){
            try {
                var s = Files.readString(Path.of(pathProperty));
                var tree = jsonMapper.readTree(s);
                if(tree.isArray()){
                    var arr = (ArrayNode) tree;
                    return arr.valueStream().map(it -> stateObjectFromJson(it, jsonMapper)).toList();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            throw new RuntimeException("Bad state");
        }


        private Object stateObjectFromJson(JsonNode objectNode, JsonMapper jsonMapper){
            var stateClassName = objectNode.get("stateClassName").asText();
            var stateData = objectNode.get("stateData");

            var possibleStateClasses = List.of(RessursState.class, OrgEnhetState.class, OrgtilknytningState.class, OrgEnhetLederState.class, OrganiseringState.class);
            for(var c : possibleStateClasses){
                if(c.getName().equals(stateClassName)){
                    return jsonMapper.convertValue(stateData, c);
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
            NomGraphqlSupportInterface graphqlSupportInterface
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
