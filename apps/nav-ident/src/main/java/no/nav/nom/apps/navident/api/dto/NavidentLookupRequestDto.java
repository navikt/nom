package no.nav.nom.apps.navident.api.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
@Builder
public class NavidentLookupRequestDto {
    @NonNull
    private String personid;
}
