package no.nav.nom.apps.api.orgtilknytning.api.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.orgtilknytning.u4diff.U4Diff;
import no.nav.nom.apps.api.orgtilknytning.u4diff.U4DiffService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Admin for U4 diff mot NOM")
@PreAuthorize("hasAuthority('ADMIN')")
@RequestMapping("/api/admin/u4diff")
@Slf4j
public class U4DiffController {
    private final U4DiffService u4DiffService;

    public U4DiffController(U4DiffService u4DiffService) {
        this.u4DiffService = u4DiffService;
    }

    @Operation(
        description = "List differanser for navidenter uten stillingperioder i U4",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @GetMapping("/identerUtenStillingIU4")
    public ResponseEntity<List<U4Diff>> OrgtilknytningerUtenforHierarki() {
        return ResponseEntity.ok(u4DiffService.getStatligeIkkeIU4());
    }
}
