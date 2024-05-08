package no.nav.nom.contract.nom.state;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import io.swagger.v3.oas.annotations.media.Schema;
import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RessursState(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String fid,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String personident,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String navident,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Sektor sektor,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Visningsnavn visningsnavn,
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) Optional<String> primaryTelefonnummer,
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) Optional<LocalDate> sluttdato,
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) Optional<LocalDate> startdato,
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) Optional<String> primaryFysiskBrukersted,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<String> fysiskeBrukersteder,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, defaultValue = "STANDARD") IdentType identType
) {
    @JsonIgnore
    public boolean isEkstern() {
        return this.sektor().equals(Sektor.EKSTERN);
    }
}
