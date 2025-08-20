package no.nav.nom.apps.admin.sanitycheck.remedycompare;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.admin.PeriodeUtil;
import no.nav.nom.apps.admin.webclient.nomapiclient.NomApiClient;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import no.nav.nom.libs.state.ressurs.mastered.MasteredRessursStateRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Objects;

@Slf4j
@Service
public class Comparer {
    private boolean hasCompared = false;

    private final RemedyDataFileRepository remedyDataFileRepository;
    private final RessursStateRepository ressursStateRepository;
    private final MasteredRessursStateRepository masteredRessursStateRepository;
    private final NomApiClient nomApiClient;

    private final HashMap<String, RemedyCompareResult> nomRessurserNotInRemedy = new HashMap<>();
    private final HashMap<String, RemedyCompareResult> diffPaaNavn = new HashMap<>();
    private final HashMap<String, RemedyCompareResult> diffPaaSektor = new HashMap<>();
    private final HashMap<String, RemedyCompareResult> diffPaaSluttdato = new HashMap<>();
    private final HashMap<String, RemedyCompareResult> diffPaaUtlopsdato = new HashMap<>();
    private final HashMap<String, RemedyCompareResult> diffPaaAktiv = new HashMap<>();
    private final HashMap<String, RemedyCompareResult> remedyUsersNotInNom = new HashMap<>();
    private final HashMap<String, RemedyCompareResult> diffPaaRemedyEnhet = new HashMap<>();

    private final HashMap<String, String> nomRemedyEnhet = new HashMap<>();
    private final HashMap<String, String> epost = new HashMap<>();

    public Comparer(RemedyDataFileRepository remedyDataFileRepository, RessursStateRepository ressursStateRepository, MasteredRessursStateRepository masteredRessursStateRepository,
                    NomApiClient nomApiClient
    ) {
        this.remedyDataFileRepository = remedyDataFileRepository;
        this.ressursStateRepository = ressursStateRepository;
        this.masteredRessursStateRepository = masteredRessursStateRepository;
        this.nomApiClient = nomApiClient;
    }

    public void compareNomWithRemedy() {
        log.info("Starting comparison between NOM and Remedy");
        ressursStateRepository.getAll().forEach(ressursState -> {
            var remedyUser = remedyDataFileRepository.getRemedyUser(ressursState.navident());
            if (remedyUser == null) {
                nomRessurserNotInRemedy.put(ressursState.navident(), new RemedyCompareResult(isMastred(ressursState), ressursState, remedyEnhet(ressursState), null, null));
            } else {
                if (nameDiffers(ressursState, remedyUser) && isRessursActive(ressursState)) {
                    diffPaaNavn.put(ressursState.navident(), new RemedyCompareResult(isMastred(ressursState), ressursState, remedyEnhet(ressursState), epost(ressursState), remedyUser));
                }
                if (ressursState.sektor() != remedyUser.sektor() && isRessursActive(ressursState)) {
                    diffPaaSektor.put(ressursState.navident(), new RemedyCompareResult(isMastred(ressursState), ressursState, remedyEnhet(ressursState), null, remedyUser));
                }
                if (!ressursState.isEkstern() || sluttdatoErPassert(ressursState)) {
                    if (sluttdatoDiffers(ressursState, remedyUser) && !sluttdatoInFuture(ressursState)  && isRessursActive(ressursState)) {
                        if (!sluttdatoErFremITidOgRemedyErNull(ressursState, remedyUser)) {
                            diffPaaSluttdato.put(ressursState.navident(), new RemedyCompareResult(isMastred(ressursState), ressursState, remedyEnhet(ressursState), null, remedyUser));
                        }
                    }
                } else { // Ekstern hvor sluttdato ikke er passert
                    if (!Objects.equals(ressursState.sluttdato(), remedyUser.utlopsDato())) {
                        diffPaaUtlopsdato.put(ressursState.navident(), new RemedyCompareResult(isMastred(ressursState), ressursState, remedyEnhet(ressursState), null, remedyUser));
                    }
                }
                if (erRessursAktiv(ressursState) != remedyUser.aktiv() && !isNomHistorical(ressursState)) {
                    diffPaaAktiv.put(ressursState.navident(), new RemedyCompareResult(isMastred(ressursState), ressursState, remedyEnhet(ressursState), null, remedyUser));
                }
                if (erRessursAktiv(ressursState) && remedyEnhetDiffers(ressursState, remedyUser)) {
                    diffPaaRemedyEnhet.put(ressursState.navident(), new RemedyCompareResult(isMastred(ressursState), ressursState, remedyEnhet(ressursState), null, remedyUser));
                }
            }
        });
        remedyDataFileRepository.getRemedyUsers().forEach((key, value) -> {
            if (ressursStateRepository.findByNavident(key).isEmpty()) {
                remedyUsersNotInNom.put(key, new RemedyCompareResult(false, null, null, null, value));
            }
        });

        hasCompared = true;
        log.info("Comparison done. Differ på navn: {} Sektor: {} Sluttdato: {} Utlopsdato: {} Aktiv: {}  RemedyEnhet {} nomRessurserNotInRemedy: {} RemedyUsersNotInNom: {}",
            diffPaaNavn.size(), diffPaaSektor.size(), diffPaaSluttdato.size(), diffPaaUtlopsdato.size(), diffPaaAktiv.size(), diffPaaRemedyEnhet.size(), nomRessurserNotInRemedy.size(), remedyUsersNotInNom.size());
    }

    private boolean remedyEnhetDiffers(RessursState ressursState, ImportedRemedyUser remedyUser) {
        return !Objects.equals(remedyEnhet(ressursState), remedyUser.enhetsId());
    }

    private String remedyEnhet(RessursState ressursState) {
        var navident = ressursState.navident();
        if (!nomRemedyEnhet.containsKey(navident)) {
            nomRemedyEnhet.put(navident, nomApiClient.getRessursRemedyEnhet(navident));
        }
        return nomRemedyEnhet.get(navident);
    }

    private String epost(RessursState ressursState) {
        var navident = ressursState.navident();
        if (!epost.containsKey(navident)) {
            epost.put(navident, nomApiClient.getEpost(navident));
        }
        return epost.get(navident);
    }

    private boolean sluttdatoInFuture(RessursState ressursState) {
        return ressursState.sluttdato() != null ? ressursState.sluttdato().isAfter(LocalDate.now()) : LocalDate.MIN.isAfter(LocalDate.now());
    }

    private static boolean sluttdatoDiffers(RessursState ressursState, ImportedRemedyUser remedyUser) {
        return !Objects.equals(ressursState.sluttdato(), remedyUser.sluttDato());
    }

    private static boolean nameDiffers(RessursState ressursState, ImportedRemedyUser remedyUser) {
        var nomName = ressursState.visningsnavn().getFormattedVisningsnavn().replaceAll("\\s+", " "); // replace multiples spaces with one
        var nomNameRemedyCapitalized = capitalizeNavn(ressursState.visningsnavn().getFormattedVisningsnavn()).replaceAll("\\s+", " ");
        var remedyVisningsnavn = remedyUser.getVisningsnavn().replaceAll("\\s+", " ");
        return !nomName.equals(remedyVisningsnavn) && !nomNameRemedyCapitalized.equals(remedyVisningsnavn);
    }


    private boolean isNomHistorical(RessursState ressursState) {
        return ressursState.sluttdato() != null ? ressursState.sluttdato().isBefore(LocalDate.of(2024, 1, 1)) : LocalDate.MAX.isBefore(LocalDate.of(2024,1,1));
    }

    private boolean isRessursActive(RessursState ressursState) {
        return ressursState.sluttdato() == null || !ressursState.sluttdato().isBefore(LocalDate.now());
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

    public HashMap<String, RemedyCompareResult> getDiffPaaRemedyEnhet() {
        if (!hasCompared) { compareNomWithRemedy(); }
        return diffPaaRemedyEnhet;
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
        diffPaaRemedyEnhet.clear();
        remedyUsersNotInNom.clear();
        nomRemedyEnhet.clear();

        hasCompared = false;
        compareNomWithRemedy();
    }

    private static boolean erRessursAktiv(RessursState ressursState) {
        return PeriodeUtil.isCurrentPeriod(ressursState);
    }

    private static boolean sluttdatoErFremITidOgRemedyErNull(RessursState ressursState, ImportedRemedyUser remedyUser) {
        return ressursState.sluttdato() != null && ressursState.sluttdato().isAfter(LocalDate.now()) && remedyUser.sluttDato() != null;
    }

    private static boolean sluttdatoErPassert(RessursState ressursState) {
        return ressursState.sluttdato() != null && ressursState.sluttdato().isBefore(LocalDate.now());
    }

    private boolean isMastred(RessursState ressursState) {
        return masteredRessursStateRepository.isMastered(ressursState.navident());
    }

    public static String capitalizeNavn(String navn){
        StringBuilder stringBuilder = new StringBuilder(navn.substring(0, 1).toUpperCase() + navn.substring(1).toLowerCase());
        for(int i = 0; i < navn.length(); i++) {
            char characterAtIndex = navn.charAt(i);
            if(characterAtIndex == '-' || Character.isWhitespace(characterAtIndex)) {
                if(i + 1 < navn.length()) {
                    stringBuilder.replace(i + 1, i + 2, String.valueOf(Character.toUpperCase(navn.charAt(i + 1))));
                }
            }
        }
        return stringBuilder.toString();
    }
}
