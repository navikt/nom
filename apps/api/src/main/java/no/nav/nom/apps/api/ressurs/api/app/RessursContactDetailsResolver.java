package no.nav.nom.apps.api.ressurs.api.app;

import no.nav.nom.apps.api.clients.ad.AdClient;
import no.nav.nom.contract.ad.AdRessurs;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RessursContactDetailsResolver {
    private final AdClient adClient;

    public RessursContactDetailsResolver(AdClient adClient) {
        this.adClient = adClient;
    }

    public Optional<String> fetchEmail(String navident) {
        return adClient.getAdRessurser(Set.of(navident)).getRessurser().stream().findFirst().map(AdRessurs::getEmail);
    }

    public Map<String, Optional<String>> fetchEmails(List<String> navidentList) {
        return adClient.fetchInAdRessursInBatches(navidentList)
            .values()
            .stream()
            .collect(Collectors.toMap(AdRessurs::getNavIdent, adRessurs -> Optional.ofNullable(adRessurs.getEmail())));
    }
}
