package no.nav.nom.apps.admin.sanitycheck.remedycompare.api;

import no.nav.nom.apps.admin.sanitycheck.remedycompare.ImportedRemedyUser;
import no.nav.nom.apps.admin.sanitycheck.remedycompare.RemedyDataFileRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/RemedyCompare")
@PreAuthorize("hasAuthority('ADMIN')")
public class RemedyCompareController {
    private final RemedyDataFileRepository remedyDataFileRepository;

    public RemedyCompareController(RemedyDataFileRepository remedyDataFileRepository) {
        this.remedyDataFileRepository = remedyDataFileRepository;
    }

    @GetMapping(value = "/remedyUser")
    public ImportedRemedyUser getRemedyUser(@RequestParam String navident) {
        return remedyDataFileRepository.getRemedyUser(navident);
    }
}
