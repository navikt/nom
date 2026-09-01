package no.nav.nom.apps.sfintegrasjon.organisasjon.diff;

import no.nav.nom.apps.sfintegrasjon.sfclient.odata.SfOrgEnhet;
import no.nav.nom.contract.nom.state.OrgEnhetState;

import java.time.LocalDateTime;
import java.util.List;

public record OrgenhetDiff(
    String nomId,
    OrgEnhetState nomEnhet,
    List<SfOrgEnhet> sfEnheter,
    List<DiffDetail> diffDetails,
    LocalDateTime diffCreated
) {

    OrgenhetDiff(String nomId, OrgEnhetState nomEnhet, List<SfOrgEnhet> sfEnheter, List<DiffDetail> diffDetails) {
        this(nomId, nomEnhet, sfEnheter, diffDetails, LocalDateTime.now());
    }

    public OrgenhetDiff {
        if (diffCreated == null) {
            diffCreated = LocalDateTime.now();
        }
    }

    public boolean harDifferanser() {
        return !diffDetails.isEmpty();
    }

    public boolean erLike() {
        return diffDetails.isEmpty();
    }


    public String report() {
        StringBuilder sb = new StringBuilder();
        sb.append("Diff for NOM enhet %s:\n".formatted(nomId));
        if (erLike()) {
            sb.append("Ingen differanser funnet.\n");
        } else {
            sb.append("Differanser     funnet:\n");
            for (DiffDetail detail : diffDetails) {
                sb.append("- %s: %s\n".formatted(detail.typeOfDiff(), detail.description()));
            }
        }
        return sb.toString();
    }
}
