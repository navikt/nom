package no.nav.nom.apps.api.ressurs.implementations.dvh;

import lombok.RequiredArgsConstructor;
import lombok.val;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.contract.nom.api.graphql.Sektor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DvhRessursInterface {

    public static final String EKSTERN = "Eksternt";
    public static final String STATLIG = "NAV Statlig";
    public static final String KOMMUNAL = "NAV Kommunalt";
    public static final String ANNEN_STATLIG = "Annen Statlig";

    @Value("${SLUTTDATO_OVERRIDE:#{{}}}")
    private List<String> overridedSluttdato;

    private final DvhRepository dvhRepository;

    public Optional<DvhRessurs> getRessurs(String navIdent){
        return Optional.ofNullable((DvhRessurs) dvhRepository.getRessursByNavIdent().get(navIdent));
    }

    public List<Sektor> getRessurstypeGraphQlResponse(String navIdent){
        val ressurstype = getRessurs(navIdent)
                .map(r -> switch (r.getRemedyAnsattType()) {
                            case STATLIG -> Sektor.NAV_STATLIG;
                            case KOMMUNAL -> Sektor.NAV_KOMMUNAL;
                            case EKSTERN, ANNEN_STATLIG -> Sektor.EKSTERN;
                            default -> null;
                        })
                .orElse(null);

        return ressurstype != null ? List.of(ressurstype) : List.of();
    }

    public LocalDate getSluttdatoForPersonident(String personIdent){
        return overridedSluttdato.contains(personIdent) ? LocalDate.parse("2019-01-29") :
                dvhRepository.getSluttdatoByPersonIdent().getOrDefault(personIdent, null);
    }
}