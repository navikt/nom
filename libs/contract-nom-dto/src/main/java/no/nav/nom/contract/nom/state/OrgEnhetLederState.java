package no.nav.nom.contract.nom.state;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
    @JsonIgnore
    public boolean isActive() {
        return (startdato == null || !startdato.isAfter(LocalDate.now())) &&
            (sluttdato == null || !sluttdato.isBefore(LocalDate.now()));
    }
}
