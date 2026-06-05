package no.nav.nom.apps.api.ressurs.api.v1.navident;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Slf4j
public class NavidentResponse {

    private String navident;

}
