package no.nav.nom.apps.admin.dto.ressurs;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import no.nav.nom.contract.nom.state.OrgEnhetLederState;

import jakarta.validation.constraints.Min;
import java.util.function.Predicate;

@Getter
@NoArgsConstructor
@Setter
public final class OrgenhetLederStateSearchParams {
    @Min(0) private int fromOffset = 0;
    private String filterText = "";

    public static Predicate<OrgEnhetLederState> orgenhetLederSearchFilter(OrgenhetLederStateSearchParams orgenhetLederStateSearchParams) {
        return e -> {
            return e.toString().contains(orgenhetLederStateSearchParams.filterText);
        };
    }

}
