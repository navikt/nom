package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class OrgEnhetKorrigertEventV1Test {


    @Test
    @SneakyThrows
    @DisplayName("Test deserializing of live OrgEnhetKorrigertEventV1")
    public void deserializeOrgEnhetKorrigertEventV1() {
        String json = jsonResource("OrgEnhetKorrigertEventV1.json");
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        Object orgEnhetEvent = objectMapper
            .readerFor(OrgEnhetEvent.class)
            .readValue(json);

        assertInstanceOf(OrgEnhetKorrigertEventV1.class, orgEnhetEvent);
    }

    private String jsonResource(String resourceName) {
        try {
            return new String(getClass().getClassLoader().getResourceAsStream(resourceName).readAllBytes());
        } catch (Exception e) {
            log.error("Could not read test resource %s".formatted(resourceName), e);
            throw new RuntimeException(e);
        }
    }
}