package no.nav.nom.apps.gql.hierarchygeneration.initals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public interface InitHierarkiInterface {
//    void upsertInitHierarki(HierarkiService.Upserts upserts, Long seed);
//
//    default void upsertInitHierarki(HierarkiService.Upserts upserts) {
//        upsertInitHierarki(upserts, System.currentTimeMillis());
//    }
//
//    default void upsertItemsInRandomOrder(List<Record> upsertStateObjectList, HierarkiService.Upserts upserts, long randomSeed) {
//
//        RandomGenerator random = new Random(randomSeed);
//        System.out.println("Random seed for upsert item shuffle is " + randomSeed);
//
//        var upsertStateObjectListCopy = new ArrayList<Record>(upsertStateObjectList);
//
//        Collections.shuffle(upsertStateObjectListCopy, random);
//
//        for(var upsertObjectState : upsertStateObjectListCopy){
//            switch(upsertObjectState){
//                case OrgEnhetState oe -> upserts.upsertEnhet(oe);
//                case RessursState rs -> upserts.upsertRessurs(rs);
//                case OrgtilknytningState ot -> upserts.upsertOrgtilknytningState(ot);
//                case LederTilgangState lts -> upserts.upsertLederTilgang(lts);
//                case OrganiseringState os -> upserts.upsertOrganisering(os);
//                case OrgEnhetLederState oels -> upserts.upsertLeder(oels);
//                default -> {
//                    throw new IllegalStateException("Not a valid class to upsert: " + upsertObjectState.getClass().getName());
//                }
//            }
//        }
//    }

    static void writeStateObjectsToTmpFile(List<Object> stateObjects){
        ObjectMapper mapper = new ObjectMapper();
        { // fix up mapper
            mapper.registerModule(new Jdk8Module());
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        }

        record StateContext(String stateClassName, Object stateData) {}

        var stateObjectsFromHierarchyWithContext = stateObjects
            .stream()
            .map(it -> new StateContext(it.getClass().getName(), it))
            .toList();

        var seed = System.currentTimeMillis();
        System.out.println("Current seed: " + seed);

        var ar = new ArrayList<>(stateObjectsFromHierarchyWithContext);
        Collections.shuffle(ar,new Random(seed));

        try {
            var jsonHierarchyData = mapper.writeValueAsString(ar);
            var tmpDir =  System.getProperty("java.io.tmpdir");
            var randomUuid = UUID.randomUUID().toString();
            var path = Path.of(tmpDir,"hierarchy-dump-"+ randomUuid + ".json");
            Files.writeString(path, jsonHierarchyData,  StandardOpenOption.CREATE);
            System.out.println("NOM hierarchy as state objects written to `" + path + "`");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
