package no.nav.nom.apps.api.graphql.request.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EnhetBestillingResponse {

    UUID nomId;
    Bestilling bestilling;

}
