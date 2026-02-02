package no.nav.nom.libs.gql.hierarchygeneration.initals;

import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public interface InitHierarkiUtils {

    static void writeStateObjectsToTmpFile(List<Object> stateObjects){
        JsonMapper mapper = new JsonMapper().rebuild().disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY).build();

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
