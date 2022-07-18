package no.nav.nom.apps.navident.api.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import no.nav.nom.apps.navident.domain.IdentType;

@Data
@Builder
public class NavidentRequestDTO {

    @NonNull
    private String personid;
    @NonNull
    private IdentType identType;
    @NonNull
    private String identPrefix;
}
