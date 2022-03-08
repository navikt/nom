package no.nav.nom.apps.api.ressurs.implementations.dvh;

import lombok.RequiredArgsConstructor;
import lombok.val;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.contract.nom.api.graphql.RessursType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DvhRessursInterface {

    private final DvhRepository dvhRepository;

    public Optional<DvhRessurs> getRessurs(String navIdent){
        return Optional.ofNullable((DvhRessurs) dvhRepository.getRessursByNavIdent().get(navIdent));
    }

    public List<RessursType> getRessursTypeGraphQlResponse(String navIdent){
        val ressursType = getRessurs(navIdent)
                .map(r -> switch (r.getRemedyAnsattType().toUpperCase()) {
                            case "NAV_STATLIG" -> RessursType.NAV_STATLIG;
                            case "NAV_KOMMUNAL" -> RessursType.NAV_KOMMUNAL;
                            case "EKSTERN", "ANNEN_STAT" -> RessursType.EKSTERN;
                            default -> null;
                        })
                .orElse(null);

        return ressursType != null ? List.of(ressursType) : List.of();
    }
}