package no.nav.nom.apps.api.dvh;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class DvhRepository {

    private final Map<String, DvhOrganisasjonsenhet> enheterByNumber = new ConcurrentHashMap<>(1000);

    public void saveEnhet(DvhOrganisasjonsenhet enhet) {
        enheterByNumber.put(enhet.getNummer(), enhet);
    }

    public Optional<DvhOrganisasjonsenhet> findEnhetByNummer(String nummer) {
        return Optional.ofNullable(enheterByNumber.get(nummer));
    }

    public Collection<DvhOrganisasjonsenhet> findAllEnheter() {
        return enheterByNumber.values();
    }

    public int enhetCount() {
        return enheterByNumber.size();
    }
}
