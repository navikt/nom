package no.nav.nom.contract.nom.state;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.Jobbtelefon;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;

import java.time.LocalDate;
import java.util.List;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RessursState(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String fid,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String personident,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String navident,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Sektor sektor,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Visningsnavn visningsnavn,
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) Jobbtelefon jobbtelefon,
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) String primaryTelefonnummer,
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) LocalDate sluttdato,
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) LocalDate startdato,
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) String primaryFysiskBrukersted,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<String> fysiskeBrukersteder,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, defaultValue = "STANDARD") IdentType identType,
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) String sistEndretAv
) {
    @JsonIgnore
    public boolean isEkstern() { return Sektor.EKSTERN.equals(this.sektor()); }
    @JsonIgnore
    public boolean isStatlig() { return Sektor.NAV_STATLIG.equals(this.sektor()); }
    @JsonIgnore
    public boolean isKommunal() { return Sektor.NAV_KOMMUNAL.equals(this.sektor()); }

    @JsonIgnore
    @Override
    public String toString() {
        return "RessursState{" +
            "navident='" + navident + '\'' +
            ", sektor=" + sektor +
            ", visningsnavn=" + visningsnavn +
            ", jobbtelefon=" + jobbtelefon +
            ", primaryTelefonnummer=" + primaryTelefonnummer +
            ", sluttdato=" + sluttdato +
            ", startdato=" + startdato +
            ", primaryFysiskBrukersted=" + primaryFysiskBrukersted +
            ", fysiskeBrukersteder=" + fysiskeBrukersteder +
            ", identType=" + identType +
            ", fid='" + fid + '\'' +
            '}';
    }
}
