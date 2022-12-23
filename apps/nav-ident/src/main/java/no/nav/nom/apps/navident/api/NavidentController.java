package no.nav.nom.apps.navident.api;

import lombok.val;
import no.nav.nom.apps.navident.IdentService;
import no.nav.nom.apps.navident.api.dto.NavidentRequestDTO;
import no.nav.nom.apps.navident.api.dto.NavidentResponseDTO;
import no.nav.nom.apps.navident.common.LogConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping(NavidentController.BASE_URL)
public class NavidentController {

    public final static String BASE_URL = "/navident";
    private final static Logger logger = LoggerFactory.getLogger(NavidentController.class);

    private final IdentService identService;

    public NavidentController(IdentService identService) {
        this.identService = identService;
    }

    @PostMapping
    public ResponseEntity<NavidentResponseDTO> createNavident(@RequestBody NavidentRequestDTO request) {
        logger.info(LogConfig.SECURE_LOGS, "Navident bestilt for: {}", request);
        val bestilling = identService.bestillIdent(request.getPersonid(), request.getIdentType(), request.getIdentPrefix().charAt(0));
        val response = NavidentResponseDTO.builder()
                .navident(bestilling.getBody().getNavident())
                .feilmelding(bestilling.getBody().getFeilmelding())
                .build();

        return ResponseEntity.status(bestilling.getHttpStatus()).body(response);
    }
}