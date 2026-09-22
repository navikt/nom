package no.nav.nom.apps.sfintegrasjon.sfhendelse.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.springframework.boot.configurationprocessor.json.JSONObject;

import java.util.List;

public record RessursHendelseDto(
    String sporingsId,
    List<String> endretAv,
    List<String> endringshendelser,
    ObjectNode ressurs
) {
}
