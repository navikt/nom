package no.nav.nom.apps.api.dvh;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.api.domain.RessursRepository;
import no.nav.nom.apps.api.dvh.dto.DvhKobling;
import no.nav.nom.apps.api.dvh.dto.DvhLeder;
import no.nav.nom.apps.api.dvh.dto.DvhOrganisasjonsenhet;
import no.nav.nom.apps.api.dvh.dto.DvhOrganisering;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@AllArgsConstructor
public class DvhRepository {

    private Map<String, DvhOrganisasjonsenhet> enheterByNumber;
    private Map<String, List<DvhLeder>> lederByEnhetNr;
    private Map<String, List<DvhOrganisering>> organiseringerByAgressoId;
    private List<DvhKobling> koblinger;
    private final RessursRepository ressursRepository;

    public Optional<DvhOrganisasjonsenhet> getEnhetByAgressoId(String agressoId) {
        return Optional.ofNullable(enheterByNumber.get(agressoId));
    }

    public Collection<DvhOrganisasjonsenhet> getAllEnheter() {
        return enheterByNumber.values();
    }

    public List<DvhOrganisering> getOrganiseringerByAgressoId(String agressoId) {
        return organiseringerByAgressoId.getOrDefault(agressoId, Collections.emptyList());
    }

    public Collection<DvhKobling> getAllKoblinger() {
        return koblinger;
    }

    public List<DvhKobling> getKoblingerByAgressoId(String enhetId) {
        return koblinger.stream().filter(x -> x.getAgressoId().equals(enhetId)).collect(Collectors.toUnmodifiableList());
    }

    public List<DvhLeder> getLederByAgressoId(String enhetNr) {
        return Optional.ofNullable(lederByEnhetNr.get(enhetNr))
                .orElse(Collections.emptyList());
    }

    public List<DvhOrganisasjonsenhet> getEnheterByLeder(String lederNavIdent) {
        return lederByEnhetNr.values().stream().flatMap(Collection::stream)
                .filter(l -> l.getNavIdent().equals(lederNavIdent))
                .map(DvhLeder::getOrgEnhetNr)
                .map(enheterByNumber::get)
                .distinct()
                .collect(Collectors.toList());
    }

    public void saveOrganisasjonsenheter(Map<String, DvhOrganisasjonsenhet> orgenheter) {
        enheterByNumber = orgenheter;
    }

    public void saveLedere(Map<String, List<DvhLeder>> lederByEnhetNr) {
        this.lederByEnhetNr = lederByEnhetNr.entrySet().stream()
                .filter((entry) -> {
                    val enhet = getEnhetByAgressoId(entry.getKey());
                    entry.getValue().forEach(l -> {
                        val ressurs = ressursRepository.findByNavIdent(l.getNavIdent());
                        if(enhet.isEmpty() && ressurs.isEmpty()){
                            log.warn("Leder relasjon mislyktes, enhet: "+entry.getKey()+", navIdent: "+l.getNavIdent()+", "+"Enhet og ressurs mangler");
                        } else if(enhet.isEmpty()){
                            log.warn("Leder relasjon mislyktes, enhet: "+entry.getKey()+", navIdent: "+l.getNavIdent()+", "+"Enhet mangler");
                        } else if(ressurs.isEmpty()){
                            log.warn("Leder relasjon mislyktes, enhet: "+entry.getKey()+", navIdent: "+l.getNavIdent()+", "+"Ressurs mangler");
                            entry.getValue().remove(l);
                        }
                    });
                    return enhet.isPresent() && !entry.getValue().isEmpty();
                })
                .collect(Collectors.toConcurrentMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public void saveOrganiseringer(Map<String, List<DvhOrganisering>> organiseringerByEnhet) {
        this.organiseringerByAgressoId = organiseringerByEnhet.entrySet().stream()
                .filter(entry -> {
                    entry.setValue(
                            entry.getValue().stream()
                                    .filter(o -> {
                                        val overEnhet = getEnhetByAgressoId(o.getOverenhetNummer());
                                        val underEnhet = getEnhetByAgressoId(o.getUnderenhetNummer());
                                        if (overEnhet.isEmpty() && underEnhet.isEmpty()) {
                                            log.warn("Organisering relasjon mislyktes, overenhet: " + o.getOverenhetNummer() + ", underenhet: " + o.getUnderenhetNummer() + ", " + "Over- og underenhet mangler");
                                        } else if (overEnhet.isEmpty()) {
                                            log.warn("Organisering relasjon mislyktes, overenhet: " + o.getOverenhetNummer() + ", underenhet: " + o.getUnderenhetNummer() + ", " + "Overenhet mangler");
                                        } else if (underEnhet.isEmpty()) {
                                            log.warn("Organisering relasjon mislyktes, overenhet: " + o.getOverenhetNummer() + ", underenhet: " + o.getUnderenhetNummer() + ", " + "Underenhet mangler");
                                        } else {
                                            return true;
                                        }
                                        return false;
                                    })
                                    .collect(Collectors.toList())
                    );
                    return !entry.getValue().isEmpty();
                })
                .collect(Collectors.toConcurrentMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public void saveKoblinger(List<DvhKobling> koblinger) {
        this.koblinger = Collections.synchronizedList(
                koblinger.stream()
                .filter(k -> {
                        val organisasjonsenhet = getEnhetByAgressoId(k.getAgressoId()).orElse(null);
                        val dvhRessurs = ressursRepository.findByNavIdent(k.getNavIdent()).orElse(null);
                        if (organisasjonsenhet == null && dvhRessurs == null) {
                            log.warn("Kobling relasjon mislyktes, enhet: "+k.getAgressoId() + ", navIdent: " + k.getNavIdent() + ", " + "Enhet og ressurs mangler");
                        } else if (organisasjonsenhet == null) {
                            log.warn("Kobling relasjon mislyktes, enhet: "+k.getAgressoId() + ", navIdent: " + k.getNavIdent() + ", " + "Enhet mangler");
                        } else if (dvhRessurs == null) {
                            log.warn("Kobling relasjon mislyktes, enhet: "+k.getAgressoId() + ", navIdent: " + k.getNavIdent() + ", " + "Ressurs mangler");
                        } else {
                            return true;
                        }
                    return false;
                })
                .collect(Collectors.toList())
        );
    }

    public void clearRepository() {
        enheterByNumber.clear();
        organiseringerByAgressoId.clear();
        koblinger.clear();
    }

    public List<DvhKobling> getKoblingerByNavIdent(String navIdent) {
        return getAllKoblinger().stream()
                .filter(s -> s.getNavIdent().equals(navIdent))
                .collect(Collectors.toUnmodifiableList());
    }
}
