package no.nav.nom.apps.navident.api;

import lombok.RequiredArgsConstructor;
import lombok.val;
import no.nav.nom.apps.navident.IdentService;
import no.nav.nom.apps.navident.api.dto.NavidentRequestDTO;
import no.nav.nom.apps.navident.api.dto.NavidentResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
@RequestMapping(NavidentController.BASE_URL)
public class NavidentController {

    private final IdentService identService;
    public static final String BASE_URL = "/navident";

    @PostMapping
    public ResponseEntity<NavidentResponseDTO> createNavident(@RequestBody NavidentRequestDTO request) {
        val bestilling = identService.bestillIdent(request.getPersonid(), request.getIdentType(), request.getIdentPrefix().charAt(0));
        val response = NavidentResponseDTO.builder()
                .navident(bestilling.getBody().getNavident())
                .feilmelding(bestilling.getBody().getFeilmelding())
                .build();

        return ResponseEntity.status(bestilling.getHttpStatus()).body(response);
    }
}