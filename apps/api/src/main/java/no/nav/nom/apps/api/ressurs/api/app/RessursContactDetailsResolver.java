package no.nav.nom.apps.api.ressurs.api.app;

import no.nav.nom.apps.api.clients.ad.AdClient;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.dvh.dto.DvhTelefon;
import no.nav.nom.apps.api.ressurs.api.dto.Ressurs;

import no.nav.nom.contract.ad.AdRessurs;
import no.nav.nom.contract.nom.TelefonType;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Component
public class RessursContactDetailsResolver {

    private final DvhRepository dvhRepository;
    private final AdClient adClient;

    public RessursContactDetailsResolver(DvhRepository dvhRepository, AdClient adClient) {
        this.dvhRepository = dvhRepository;
        this.adClient = adClient;
    }

    public Optional<String> fetchEmail(String navident){
        return adClient.getAdRessurser(Set.of(navident)).getRessurser().stream().findFirst().map(AdRessurs::getEmail);
    }

    public List<Ressurs.Telefon> fetchTelefoner(String navident) {
        return dvhRepository.getTelefonNrListByNavIdent(navident)
            .stream()
            .filter(Objects::nonNull)
            .map(tlf -> new Ressurs.Telefon(tlf.getNummer(), mapType(tlf.getType())))
            .toList();
    }

    private TelefonType mapType(DvhTelefon.DvhTelefonType dvhTelefonType) {
        return switch (dvhTelefonType) {
            case TJENESTETELEFON -> TelefonType.NAV_TJENESTE_TELEFON;
            case KONTORTELEFON -> TelefonType.NAV_KONTOR_TELEFON;
            default -> TelefonType.PRIVAT_TELEFON;
        };
    }
}
