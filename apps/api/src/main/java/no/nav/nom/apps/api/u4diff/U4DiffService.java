package no.nav.nom.apps.api.u4diff;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
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
        return u4DiffRepository.findByKildeAndType(U4DiffKilde.STILLINGER, U4DiffType.MANGLENDE_STATLIG_I_U4);
    }

    public List<U4Diff> getSektorDifferanse() { return u4DiffRepository.findByKildeAndType(U4DiffKilde.RESSURS, U4DiffType.ULIK_SEKTOR_RESSURS); }

    public List<U4Diff> getUlikEnhet() {
        List<U4Diff> ulikEnhetDiffer = new ArrayList<>();
        var dobleRader = grupperteDifferMedKun2Rader();
        dobleRader.forEach((navident, u4Differ) -> {
            var diffTypes = u4Differ.stream().map(U4Diff::getType).toList();
            if (erUlikEnhet(diffTypes) || erEnhetsBytteIU4IkkeINOM(diffTypes) || erEnhetsBytteINOMIkkeIU4(diffTypes)) {
                ulikEnhetDiffer.addAll(u4Differ);
            }
        });
        return ulikEnhetDiffer;
    }

    public List<U4Diff> getIkkeKlassifiserteDifferanser() {
        List<Integer> klassifiserteIder = new ArrayList<>();
        klassifiserteIder.addAll(getStatligeIkkeIU4().stream().map(U4Diff::getId).toList());
        klassifiserteIder.addAll(getSektorDifferanse().stream().map(U4Diff::getId).toList());
        klassifiserteIder.addAll(getUlikEnhet().stream().map(U4Diff::getId).toList());

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

    private boolean erUlikEnhet(List<U4DiffType> diffTyper) {
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
}
