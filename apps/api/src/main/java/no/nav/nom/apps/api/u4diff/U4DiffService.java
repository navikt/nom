package no.nav.nom.apps.api.u4diff;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.etter;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.lik;
import static no.nav.nom.apps.api.u4diff.U4DiffKilde.STILLINGER;
import static no.nav.nom.apps.api.u4diff.U4DiffType.EKSTRA_U4_PERIODE;
import static no.nav.nom.apps.api.u4diff.U4DiffType.FORSKJELL_I_U4_PERIODE;
import static no.nav.nom.apps.api.u4diff.U4DiffType.MANGLENDE_U4_PERIODE;

@Service
public class U4DiffService {
    final U4DiffRepository u4DiffRepository;

    public U4DiffService(U4DiffRepository u4DiffRepository) {
        this.u4DiffRepository = u4DiffRepository;
    }

    public List<U4Diff> getStatligeIkkeIU4() {
        return u4DiffRepository.findByKildeAndType(STILLINGER, U4DiffType.MANGLENDE_STATLIG_I_U4);
    }

    public List<U4Diff> getSektorDifferanse() { return u4DiffRepository.findByKildeAndType(U4DiffKilde.RESSURS, U4DiffType.ULIK_SEKTOR_RESSURS); }

    public List<U4Diff> getUlikEnhet() {
        List<U4Diff> ulikEnhetDiffer = new ArrayList<>();
        var dobleRader = grupperteDifferMedKun2Rader();
        dobleRader.forEach((navident, u4Differ) -> {
            var diffTypes = u4Differ.stream().map(U4Diff::getType).toList();
            if (harUlikeEnheter(u4Differ)
                && (erUlikEnhetEndringstyper(diffTypes) || erEnhetsBytteIU4IkkeINOM(diffTypes) || erEnhetsBytteINOMIkkeIU4(diffTypes))) {
                ulikEnhetDiffer.addAll(u4Differ);
            }
        });
        return ulikEnhetDiffer;
    }

    public List<U4Diff> getUlikStartDato() {
        List<U4Diff> ulikStartDatoMerged = new ArrayList<>();

        getUlikStartDatoBasis().stream()
            .collect(Collectors.groupingBy(U4Diff::getNavident))
            .forEach((navident, u4Differ) -> {
                u4Differ.stream()
                    .collect(groupingBy(diff -> diff.getNomOrgenhetId() != null ? diff.getNomOrgenhetId() : diff.getU4OrgenhetId()))
                    .forEach((enhet, differPaEnhet) -> {
                        ulikStartDatoMerged.addAll(mergeIfPossible(differPaEnhet));
                    });
                }
            );
        return ulikStartDatoMerged;
    }

    private List<U4Diff> getUlikStartDatoBasis() {
        List<U4Diff> ulikStartDato = new ArrayList<>();
        var perNavident = u4DiffRepository.findByKilde(STILLINGER).stream()
            .collect(Collectors.groupingBy(U4Diff::getNavident));

        perNavident.forEach((navident, u4Differ) -> {
            u4Differ.stream()
                .collect(groupingBy(diff -> diff.getNomOrgenhetId() != null ? diff.getNomOrgenhetId() : diff.getU4OrgenhetId()))
                .forEach((enhet, differPaEnhet) -> { // per enhet
                    if (erDetEkstraU4PeriodeOgManglendeU4Periode(differPaEnhet) && erStartDatoUlik(differPaEnhet)) {
                        ulikStartDato.addAll(differPaEnhet);
                    }
                });
        });
        return ulikStartDato;
    }

    public List<U4Diff> getUlikSluttDato() {
        List<U4Diff> ulikSluttDato = new ArrayList<>();
        var perNavident = u4DiffRepository.findByKilde(STILLINGER).stream()
            .collect(Collectors.groupingBy(U4Diff::getNavident));

        perNavident.forEach((navident, u4Differ) -> {
            u4Differ.stream()
                .collect(groupingBy(diff -> diff.getNomOrgenhetId() != null ? diff.getNomOrgenhetId() : diff.getU4OrgenhetId()))
                .forEach((enhet, differPaEnhet) -> { // per enhet
                    if (erDetKunForskjellIPeriode(differPaEnhet) && erSluttDatoUlik(differPaEnhet.getFirst())) {
                        ulikSluttDato.addAll(differPaEnhet);
                    }
                });
        });
        return ulikSluttDato;
    }

    public List<U4Diff> getIkkeKlassifiserteDifferanser() {
        List<Integer> klassifiserteIder = new ArrayList<>();
        klassifiserteIder.addAll(getStatligeIkkeIU4().stream().map(U4Diff::getId).toList());
        klassifiserteIder.addAll(getSektorDifferanse().stream().map(U4Diff::getId).toList());
        klassifiserteIder.addAll(getUlikEnhet().stream().map(U4Diff::getId).toList());
        klassifiserteIder.addAll(getUlikStartDatoBasis().stream().map(U4Diff::getId).toList());
        klassifiserteIder.addAll(getUlikSluttDato().stream().map(U4Diff::getId).toList());

        return u4DiffRepository.findAll().stream()
            .filter(u4Diff -> !klassifiserteIder.contains(u4Diff.getId()))
            .toList();
    }

    public List<U4Diff> getAlleDiffRader() { return u4DiffRepository.findAll(); }

    // Grupper U4Diff på navident, og hent alle med bare to rader
    private Map<String, List<U4Diff>> grupperteDifferMedKun2Rader() {
        return u4DiffRepository.findAll().stream()
            .collect(Collectors.groupingBy(U4Diff::getNavident))
            .entrySet().stream()
            .filter(entry -> entry.getValue().size() == 2)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private boolean harUlikeEnheter(List<U4Diff> differ) {
        var nonNullNomOrgenheter = differ.stream().map(U4Diff::getNomOrgenhetId).filter(Objects::nonNull).collect(Collectors.toSet());
        var nonNullU4Orgenheter = differ.stream().map(U4Diff::getU4OrgenhetId).filter(Objects::nonNull).collect(Collectors.toSet());
        return !nonNullNomOrgenheter.containsAll(nonNullU4Orgenheter);
    }

    private boolean erUlikEnhetEndringstyper(List<U4DiffType> diffTyper) {
        if (diffTyper.size() != 2) {
            return false;
        }
        return diffTyper.contains(EKSTRA_U4_PERIODE) && diffTyper.contains(MANGLENDE_U4_PERIODE);
    }

    private boolean erEnhetsBytteIU4IkkeINOM(List<U4DiffType> diffTyper) {
        if (diffTyper.size() != 2) {
            return false;
        }
        return diffTyper.contains(EKSTRA_U4_PERIODE) && diffTyper.contains(FORSKJELL_I_U4_PERIODE);
    }

    private boolean erEnhetsBytteINOMIkkeIU4(List<U4DiffType> diffTyper) {
        if (diffTyper.size() != 2) {
            return false;
        }
        return diffTyper.contains(MANGLENDE_U4_PERIODE) && diffTyper.contains(FORSKJELL_I_U4_PERIODE);
    }

    private boolean erDetEkstraU4PeriodeOgManglendeU4Periode(List<U4Diff> differPaEnhet) {
        if (differPaEnhet.size() != 2) return false;
        var diffTypes = differPaEnhet.stream().map(U4Diff::getType).toList();
        return diffTypes.contains(EKSTRA_U4_PERIODE) && diffTypes.contains(MANGLENDE_U4_PERIODE);
    }

    private boolean erStartDatoUlik(List<U4Diff> differPaEnhet) {
        var nomStartDato = differPaEnhet.stream().filter(u4Diff -> u4Diff.getType().equals(MANGLENDE_U4_PERIODE)).findFirst().get().getNomGyldigFom();
        var u4StartDato = differPaEnhet.stream().filter(u4Diff -> u4Diff.getType().equals(EKSTRA_U4_PERIODE)).findFirst().get().getU4GyldigFom();

        var nestenTreAarSiden = LocalDate.now().minusYears(3).plusMonths(1);
        if (etter(nestenTreAarSiden, nomStartDato)) nomStartDato = nestenTreAarSiden;
        if (etter(nestenTreAarSiden, u4StartDato)) u4StartDato = nestenTreAarSiden;

        return !Objects.equals(nomStartDato, u4StartDato);
    }

    private boolean erDetKunForskjellIPeriode(List<U4Diff> differPaEnhet) {
        if (differPaEnhet.size() != 1) return false;
        return differPaEnhet.getFirst().getType().equals(FORSKJELL_I_U4_PERIODE);
    }

    private boolean erSluttDatoUlik(U4Diff diffPaaEnhet) {
        return !lik(diffPaaEnhet.getNomGyldigTom(), diffPaaEnhet.getU4GyldigTom());
    }

    private Collection<? extends U4Diff> mergeIfPossible(List<U4Diff> differPaEnhet) {
        if (differPaEnhet.size() == 2) {
            U4Diff nomDiffRad = null;
            U4Diff u4DiffRad = null;
            if (differPaEnhet.getFirst().getNomOrgenhetId() != null) {
                nomDiffRad = differPaEnhet.getFirst();
            } else if (differPaEnhet.getLast().getNomOrgenhetId() != null) {
                nomDiffRad = differPaEnhet.getLast();
            }
            if (differPaEnhet.getFirst().getU4OrgenhetId() != null) {
                u4DiffRad = differPaEnhet.getFirst();
            } else if (differPaEnhet.getLast().getU4OrgenhetId() != null) {
                u4DiffRad = differPaEnhet.getLast();
            }
            if (nomDiffRad == null || u4DiffRad == null) {
                return differPaEnhet; // Kan ikke slå sammen
            }
            return List.of(U4Diff.builder()
                .id(nomDiffRad.getId())
                .navident(nomDiffRad.getNavident())
                .nomSektor(nomDiffRad.getNomSektor())
                .kilde(nomDiffRad.getKilde())
                .nomDol(nomDiffRad.getNomDol())
                .nomOrgenhetId(nomDiffRad.getNomOrgenhetId())
                .nomOrgenhetNavn(nomDiffRad.getNomOrgenhetNavn())
                .nomGyldigFom(nomDiffRad.getNomGyldigFom())
                .nomGyldigTom(nomDiffRad.getNomGyldigTom())
                .nomOtSektor(nomDiffRad.getNomOtSektor())
                .nomOrgtilknytning(nomDiffRad.getNomOrgtilknytning())
                .u4SektorPaaRessurs(u4DiffRad.getU4SektorPaaRessurs())
                .u4RessursType(u4DiffRad.getU4RessursType())
                .u4RessursAnsettelsestypeBesk(u4DiffRad.getU4RessursAnsettelsestypeBesk())
                .u4StillingAnsettelsestypeBesk(u4DiffRad.getU4StillingAnsettelsestypeBesk())
                .u4SektorPaaStilling(u4DiffRad.getU4SektorPaaStilling())
                .u4Dol(u4DiffRad.getU4Dol())
                .u4OrgenhetId(u4DiffRad.getU4OrgenhetId())
                .u4OrgenhetNavn(u4DiffRad.getU4OrgenhetNavn())
                .u4GyldigFom(u4DiffRad.getU4GyldigFom())
                .u4GyldigTom(u4DiffRad.getU4GyldigTom())
                .u4StillingPeriode(u4DiffRad.getU4StillingPeriode())
                .build()
            );
        } else {
            return differPaEnhet;
        }

    }

}
