package no.nav.nom.apps.api.orgtilknytning.api.admin;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.orgtilknytning.job.ImportMissingOrgtilknytninger;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasAuthority('ADMIN')")
@RequestMapping("/api/admin")
@Slf4j
public class ImportMissingOrgtilknytningerController {

    private final ImportMissingOrgtilknytninger importMissingOrgtilknytninger;

    public ImportMissingOrgtilknytningerController(ImportMissingOrgtilknytninger importMissingOrgtilknytninger) {
        this.importMissingOrgtilknytninger = importMissingOrgtilknytninger;
    }


    @GetMapping("/importMissingOrgtilknytninger")
    public String importMissingOrgTilknytninger(
            @RequestParam String bucketObjectName,
            @RequestParam(required = false) boolean kunLes
    ) {
        log.info("Importerer manglende orgtilknytninger fra {} kunLes={}", bucketObjectName, kunLes);
        return importMissingOrgtilknytninger.importfromBucket(bucketObjectName, kunLes);
    }
    
}
