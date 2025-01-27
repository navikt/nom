package no.nav.nom.apps.api.clients.msgraph;

import lombok.Builder;

@Builder
public record ODataError(
        ODataErrorBody error
) {
    
    @Builder
    public record ODataErrorBody (
            String code,
            String message
    ){}
}
