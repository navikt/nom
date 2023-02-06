package no.nav.nom.contract.nom.eventsource;

//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
//import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.JsonSerde;

@Slf4j
public class OrgEnhetKorrigertEventV1Test {

    private static JsonSerde<Object> objectSerde;

    @BeforeAll
    public static void createSerde() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        objectSerde = new JsonSerde<>(Object.class, objectMapper);
        objectSerde.deserializer().addTrustedPackages(Event.class.getPackageName());
        objectSerde.deserializer().addTrustedPackages(OrgEnhetState.class.getPackageName());
    }


    @Test
    @SneakyThrows
    @DisplayName("Test deserializing of live OrgEnhetKorrigertEventV1")
    public void deserializeWithKafakDeserializerOrgEnhetKorrigertEventV1() {
        byte[] json = jsonResource("OrgEnhetKorrigertEventV1.json");

        final var eventSerde = objectSerde.copyWithType(OrgEnhetEvent.class);

//        ObjectMapper objectMapper = new ObjectMapper();
//        objectMapper.findAndRegisterModules();

        eventSerde.deserializer().deserialize("dummy", json);
//        OrgEnhetEvent orgEnhetEvent = objectMapper
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