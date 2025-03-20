package no.nav.nom.apps.admin.sanitycheck.remedycompare;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import no.nav.nom.libs.state.ressurs.mastered.MasteredRessursStateRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;

@Slf4j
@Service
public class Comparer {
    private boolean hasCompared = false;

    private final RemedyDataFileRepository remedyDataFileRepository;
    private final RessursStateRepository ressursStateRepository;
    private final MasteredRessursStateRepository masteredRessursStateRepository;

    private final HashMap<String, RemedyCompareResult> nomRessurserNotInRemedy = new HashMap<>();
    private final HashMap<String, RemedyCompareResult> diffPaaNavn = new HashMap<>();
    private final HashMap<String, RemedyCompareResult> diffPaaSektor = new HashMap<>();
    private final HashMap<String, RemedyCompareResult> diffPaaSluttdato = new HashMap<>();
    private final HashMap<String, RemedyCompareResult> diffPaaUtlopsdato = new HashMap<>();
    private final HashMap<String, RemedyCompareResult> diffPaaAktiv = new HashMap<>();
    private final HashMap<String, RemedyCompareResult> remedyUsersNotInNom = new HashMap<>();

    public Comparer(RemedyDataFileRepository remedyDataFileRepository, RessursStateRepository ressursStateRepository, MasteredRessursStateRepository masteredRessursStateRepository) {
        this.remedyDataFileRepository = remedyDataFileRepository;
        this.ressursStateRepository = ressursStateRepository;
        this.masteredRessursStateRepository = masteredRessursStateRepository;
    }

    public void compareNomWithRemedy() {
        ressursStateRepository.getAll().forEach(ressursState -> {
            var remedyUser = remedyDataFileRepository.getRemedyUser(ressursState.navident());
            if (remedyUser == null) {
                nomRessurserNotInRemedy.put(ressursState.navident(), new RemedyCompareResult(isMastred(ressursState), ressursState, null));
            } else {
                if (!ressursState.visningsnavn().getFormattedVisningsnavn().equals(remedyUser.getVisningsnavn())) {
                    diffPaaNavn.put(ressursState.navident(), new RemedyCompareResult(isMastred(ressursState), ressursState, remedyUser));
                }
                if (ressursState.sektor() != remedyUser.sektor()) {
                    diffPaaSektor.put(ressursState.navident(), new RemedyCompareResult(isMastred(ressursState), ressursState, remedyUser));
                }
                if (!ressursState.isEkstern() || sluttdatoErPassert(ressursState)) {
                    if (!Objects.equals(ressursState.sluttdato().orElse(null), remedyUser.sluttDato())) {
                        if (!sluttdatoErFremITidOgRemedyErNull(ressursState, remedyUser)) {
                            diffPaaSluttdato.put(ressursState.navident(), new RemedyCompareResult(isMastred(ressursState), ressursState, remedyUser));
                        }
                    }
                } else { // Ekstern hvor sluttdato ikke er passert
                    if (!Objects.equals(ressursState.sluttdato().orElse(null), remedyUser.utlopsDato())) {
                        diffPaaUtlopsdato.put(ressursState.navident(), new RemedyCompareResult(isMastred(ressursState), ressursState, remedyUser));
                    }
                }
                if (erRessursAktiv(ressursState) != remedyUser.aktiv()) {
                    diffPaaAktiv.put(ressursState.navident(), new RemedyCompareResult(isMastred(ressursState), ressursState, remedyUser));
                }
            }
        });
        remedyDataFileRepository.getRemedyUsers().entrySet().forEach( entry -> {
            if (!nomRessurserNotInRemedy.containsKey(entry.getKey())) {
                remedyUsersNotInNom.put(entry.getKey(), new RemedyCompareResult(false, null, entry.getValue()));
            }
        });
        hasCompared = true;
        log.info("Comparison done. Differ på navn: {} Sektor: {} Sluttdato: {} Utlopsdato: {} Aktiv: {} nomRessurserNotInRemedy: {} RemedyUsersNotInNom: {}",
            diffPaaNavn.size(), diffPaaSektor.size(), diffPaaSluttdato.size(), diffPaaUtlopsdato.size(), diffPaaAktiv.size(), nomRessurserNotInRemedy.size(), remedyUsersNotInNom.size());
    }

    public HashMap<String, RemedyCompareResult> getRessursNotInRemedy() {
        if (!hasCompared) { compareNomWithRemedy(); }
        return nomRessurserNotInRemedy;
    }

    public HashMap<String, RemedyCompareResult> getDiffPaaNavn() {
        if (!hasCompared) { compareNomWithRemedy(); }
        return diffPaaNavn;
    }

    public HashMap<String, RemedyCompareResult> getDiffPaaSektor() {
        if (!hasCompared) { compareNomWithRemedy(); }
        return diffPaaSektor;
    }

    public HashMap<String, RemedyCompareResult> getDiffPaaSluttdato() {
        if (!hasCompared) { compareNomWithRemedy(); }
        return diffPaaSluttdato;
    }

    public HashMap<String, RemedyCompareResult> getDiffPaaUtlopsdato() {
        if (!hasCompared) { compareNomWithRemedy(); }
        return diffPaaUtlopsdato;
    }

    public HashMap<String, RemedyCompareResult> getDiffPaaAktiv() {
        if (!hasCompared) { compareNomWithRemedy(); }
        return diffPaaAktiv;
    }

    public HashMap<String, RemedyCompareResult> getRemedyUsersNotInNom() {
        if (!hasCompared) { compareNomWithRemedy(); }
        return remedyUsersNotInNom;
    }

    public void clearAndCompare() {
        nomRessurserNotInRemedy.clear();
        diffPaaNavn.clear();
        diffPaaSektor.clear();
        diffPaaSluttdato.clear();
        diffPaaUtlopsdato.clear();
        diffPaaAktiv.clear();
        remedyUsersNotInNom.clear();
        hasCompared = false;

        compareNomWithRemedy();
    }

    private static boolean erRessursAktiv(RessursState ressursState) {
        return ressursState.startdato().orElse(LocalDate.MIN).isBefore(LocalDate.now().plusDays(1)) && ressursState.sluttdato()
            .orElse(LocalDate.MAX)
            .isAfter(LocalDate.now().minusDays(1));
    }

    private static boolean sluttdatoErFremITidOgRemedyErNull(RessursState ressursState, ImportedRemedyUser remedyUser) {
        return ressursState.sluttdato().isPresent() && ressursState.sluttdato().get().isAfter(LocalDate.now()) && remedyUser.sluttDato() != null;
    }

    private static boolean sluttdatoErPassert(RessursState ressursState) {
        return ressursState.sluttdato().isPresent() && ressursState.sluttdato().get().isBefore(LocalDate.now());
    }

    private boolean isMastred(RessursState ressursState) {
        return masteredRessursStateRepository.isMastered(ressursState.navident());
    }
}
