package no.nav.nom.contract.nom.state;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.LocalDate;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrgEnhetLederState(
    String fid,
    String navident,
    String orgEnhetId,
    LocalDate startdato,
    LocalDate sluttdato,
    String sistEndretAv
) {
}
