package no.nav.nom.apps.api.ressurs.api.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import no.nav.nom.contract.nom.AnsettelsesType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.Tilknytningtype;

import java.time.LocalDate;
import java.util.Optional;

@Builder(toBuilder = true)
public record OpprettRessursRequest(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String personident,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String visningsfornavn,
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) String visningsmellomnavn,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String visningsetternavn,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Sektor sektor,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String orgEnhetId,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate startDato,
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) Optional<LocalDate> sluttDato,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) AnsettelsesType ansettelsesType,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String fysiskBrukerstedNr,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Tilknytningtype tilknytningtype
) { }
