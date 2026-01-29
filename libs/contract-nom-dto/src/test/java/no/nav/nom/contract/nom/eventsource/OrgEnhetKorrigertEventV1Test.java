package no.nav.nom.contract.nom.eventsource;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.JacksonJsonSerde;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
public class OrgEnhetKorrigertEventV1Test {

    private static JacksonJsonSerde<Object> objectSerde;

    @BeforeAll
    public static void createSerde() {
        JsonMapper jsonMapper = new JsonMapper().rebuild()
            .disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .build();

        objectSerde = new JacksonJsonSerde<>(Object.class, jsonMapper);
        objectSerde.deserializer().addTrustedPackages(Event.class.getPackageName());
        objectSerde.deserializer().addTrustedPackages(OrgEnhetState.class.getPackageName());
    }


    @Test
    @SneakyThrows
    @DisplayName("Test deserializing of live OrgEnhetKorrigertEventV1")
    public void deserializeWithKafakDeserializerOrgEnhetKorrigertEventV1() {
        byte[] json = jsonResource("OrgEnhetKorrigertEventV1.json");

        final var eventSerde = objectSerde.copyWithType(OrgEnhetEvent.class);

//        JsonMapper jsonMapper = new JsonMapper();
//        jsonMapper.findAndRegisterModules();

        eventSerde.deserializer().deserialize("dummy", json);
//        OrgEnhetEvent orgEnhetEvent = jsonMapper
//            .readerFor(OrgEnhetEvent.class)
//            .readValue(json);

//        assertInstanceOf(OrgEnhetKorrigertEventV1.class, orgEnhetEvent);
    }

    private byte[] jsonResource(String resourceName) {
        try {
//            return new String(getClass().getClassLoader().getResourceAsStream(resourceName).readAllBytes());
            return getClass().getClassLoader().getResourceAsStream(resourceName).readAllBytes();
        } catch (Exception e) {
            log.error("Could not read test resource %s".formatted(resourceName), e);
            throw new RuntimeException(e);
        }
    }
}