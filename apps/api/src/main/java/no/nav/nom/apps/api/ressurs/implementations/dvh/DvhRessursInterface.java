package no.nav.nom.apps.api.ressurs.implementations.dvh;

import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.contract.nom.api.graphql.Sektor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class DvhRessursInterface {

    public static final String EKSTERN = "Eksternt";
    public static final String STATLIG = "NAV Statlig";
    public static final String KOMMUNAL = "NAV Kommunalt";
    public static final String ANNEN_STATLIG = "Annen Statlig";
    private final DvhRepository dvhRepository;

    public DvhRessursInterface(DvhRepository dvhRepository) {
        this.dvhRepository = dvhRepository;
    }

    public Optional<DvhRessurs> getRessurs(String navIdent){
        return Optional.ofNullable(dvhRepository.getRessursByNavIdent().get(navIdent));
    }

    public Optional<String> getRessurstypeGraphQlResponse(String navIdent) {
        return getRessurs(navIdent)
                .map(r -> switch (r.getRemedyAnsattType()) {
                    case STATLIG -> Sektor.NAV_STATLIG;
                    case KOMMUNAL -> Sektor.NAV_KOMMUNAL;
                    case EKSTERN, ANNEN_STATLIG -> Sektor.EKSTERN;
                    default -> null;
                })
                .map(Sektor::name);
    }

    public LocalDate getSluttdatoForPersonident(String personIdent) {
        return Optional.ofNullable(dvhRepository.getRessursByPersonident().get(personIdent))
                .map(DvhRessurs::getSluttdato)
                .orElse(null);
    }
}