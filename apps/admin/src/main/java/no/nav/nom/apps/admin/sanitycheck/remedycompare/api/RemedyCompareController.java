package no.nav.nom.apps.admin.sanitycheck.remedycompare.api;

import no.nav.nom.apps.admin.sanitycheck.remedycompare.Comparer;
import no.nav.nom.apps.admin.sanitycheck.remedycompare.ImportedRemedyUser;
import no.nav.nom.apps.admin.sanitycheck.remedycompare.RemedyCompareResult;
import no.nav.nom.apps.admin.sanitycheck.remedycompare.RemedyDataFileRepository;
import no.nav.nom.contract.nom.state.RessursState;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/RemedyCompare")
@PreAuthorize("hasAuthority('ADMIN')")
public class RemedyCompareController {
    private final RemedyDataFileRepository remedyDataFileRepository;
    private final Comparer comparer;

    public RemedyCompareController(RemedyDataFileRepository remedyDataFileRepository, Comparer comparer) {
        this.remedyDataFileRepository = remedyDataFileRepository;
        this.comparer = comparer;
    }

    @GetMapping(value = "/remedyUser")
    public ImportedRemedyUser getRemedyUser(@RequestParam String navident) {
        return remedyDataFileRepository.getRemedyUser(navident);
    }

    @GetMapping(value = "/reloadAndCompare")
    public void reloadAndCompare() {
        remedyDataFileRepository.reload();
        comparer.clearAndCompare();
    }

    @GetMapping(value = "/ressursNotInRemedy")
    public Collection<RemedyCompareResult> ressursNotInRemedy() {
        return comparer.getRessursNotInRemedy().values();
    }

    @GetMapping(value = "/diffPaaNavn")
    public Collection<RemedyCompareResult> diffPaaNavn() {
        return comparer.getDiffPaaNavn().values();
    }

    @GetMapping(value = "/diffPaaSektor")
    public Collection<RemedyCompareResult> diffPaaSektor() {
        return comparer.getDiffPaaSektor().values();
    }

    @GetMapping(value = "/diffPaaSluttdato")
    public Collection<RemedyCompareResult> diffPaaSluttdato() {
        return comparer.getDiffPaaSluttdato().values();
    }

    @GetMapping(value = "/diffPaaUtlopsdato")
    public Collection<RemedyCompareResult> diffPaaUtlopsdato() {
        return comparer.getDiffPaaUtlopsdato().values();
    }

    @GetMapping(value = "/diffPaaAktiv")
    public Collection<RemedyCompareResult> diffPaaAktiv() {
        return comparer.getDiffPaaAktiv().values();
    }

    @GetMapping(value = "/remedyUsersNotInNom")
    public Collection<RemedyCompareResult> remedyUsersNotInNom() {
        return comparer.getRemedyUsersNotInNom().values();
    }
}
