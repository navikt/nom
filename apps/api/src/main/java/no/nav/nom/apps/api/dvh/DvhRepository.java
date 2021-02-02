package no.nav.nom.apps.api.dvh;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.api.dvh.dto.DvhKobling;
import no.nav.nom.apps.api.dvh.dto.DvhLeder;
import no.nav.nom.apps.api.dvh.dto.DvhOrganisasjonsenhet;
import no.nav.nom.apps.api.dvh.dto.DvhOrganisering;
import no.nav.nom.apps.api.dvh.dto.DvhRessurs;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
@AllArgsConstructor
public class DvhRepository {

    private Map<String, DvhOrganisasjonsenhet> enheterByNumber;
    private final Map<String, DvhRessurs> ressursByNavIdent = new ConcurrentHashMap<>(1000);
    private Map<String, List<DvhLeder>> lederByEnhetNr;
    private Map<String, List<DvhOrganisering>> organiseringByEnhet;
    private List<DvhKobling> koblinger;

    public Optional<DvhOrganisasjonsenhet> findEnhetByNummer(String nummer) {
        return Optional.ofNullable(enheterByNumber.get(nummer));
    }

    public Collection<DvhOrganisasjonsenhet> findAllEnheter() {
        return enheterByNumber.values();
    }

    public List<DvhOrganisering> findOrganiseringerByEnhet(String nummer){
        return organiseringByEnhet.getOrDefault(nummer, Collections.emptyList());
    }

    public void saveRessurs(DvhRessurs ressurs){
        ressursByNavIdent.put(ressurs.getNavIdent(), ressurs);
    }

    public Optional<DvhRessurs> findRessursByNavIdent(String navIdent){
        return Optional.ofNullable(ressursByNavIdent.get(navIdent));
    }

    public Optional<DvhRessurs> findByPersonIdent(String personIdent){
        return ressursByNavIdent.values().stream()
                .filter(r -> r.getPersonIdent().equals(personIdent))
                .findAny();
    }

    public Optional<DvhRessurs> findRessursByRessursId(String ressursId){
        return ressursByNavIdent.keySet().stream()
                .filter(s -> s.contains(ressursId))
                .findAny()
                .map(ressursByNavIdent::get);
    }

    public Collection<DvhRessurs> findAllRessurser(){
        return ressursByNavIdent.values();
    }

    public Collection<DvhKobling> findAllKoblinger() {
        return koblinger;
    }

    public List<DvhKobling> getKoblingerForEnhet(String enhetId){
        return koblinger.stream().filter(x -> x.getAgressoId().equals(enhetId)).collect(Collectors.toUnmodifiableList());
    }

    public List<DvhLeder> findLederByEnhetNr(String enhetNr){
        return Optional.ofNullable(lederByEnhetNr.get(enhetNr))
                .orElse(Collections.emptyList());
    }

    public void saveOrganisasjonsenheter(Map<String, DvhOrganisasjonsenhet> orgenheter){
        enheterByNumber = orgenheter;
    }

    public void saveLedere(Map<String, List<DvhLeder>> lederByEnhetNr){
        this.lederByEnhetNr = lederByEnhetNr.entrySet().stream()
                .filter((entry) ->{
                    val enhet = findEnhetByNummer(entry.getKey());
                    entry.getValue().forEach(l -> {
                        val ressurs = findRessursByRessursId(l.getRessursId());
                        if(enhet.isEmpty() && ressurs.isEmpty()){
                            log.warn("Leder relasjon misslyktes, enhet: "+entry.getKey()+", ressursId: "+l.getRessursId()+", "+"Enhet og ressurs mangler");
                        } else if(enhet.isEmpty()){
                            log.warn("Leder relasjon misslyktes, enhet: "+entry.getKey()+", ressursId: "+l.getRessursId()+", "+"Enhet mangler");
                        } else if(ressurs.isEmpty()){
                            log.warn("Leder relasjon misslyktes, enhet: "+entry.getKey()+", ressursId: "+l.getRessursId()+", "+"Ressurs mangler");
                            entry.getValue().remove(l);
                        }
                    });
                    return enhet.isPresent() && !entry.getValue().isEmpty();
                })
                .collect(Collectors.toConcurrentMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public void saveOrganiseringer(Map<String, List<DvhOrganisering>> organiseringerByEnhet){
        this.organiseringByEnhet = organiseringerByEnhet.entrySet().stream()
                .filter(entry -> {
                    entry.setValue(
                            entry.getValue().stream()
                            .filter(o -> {
                                val overEnhet = findEnhetByNummer(o.getOverenhetNummer());
                                val underEnhet = findEnhetByNummer(o.getUnderenhetNummer());
                                if(overEnhet.isEmpty() && underEnhet.isEmpty()){
                                    log.warn("Organisering relasjon misslyktes, overenhet: "+o.getOverenhetNummer()+", underenhet: "+o.getUnderenhetNummer()+", "+"Over- og underenhet mangler");
                                } else if(overEnhet.isEmpty()){
                                    log.warn("Organisering relasjon misslyktes, overenhet: "+o.getOverenhetNummer()+", underenhet: "+o.getUnderenhetNummer()+", "+"Overenhet mangler");
                                } else if(underEnhet.isEmpty()){
                                    log.warn("Organisering relasjon misslyktes, overenhet: "+o.getOverenhetNummer()+", underenhet: "+o.getUnderenhetNummer()+", "+"Underenhet mangler");
                                }else {
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

    public void saveKoblinger(List<DvhKobling> koblinger){
        this.koblinger = Collections.synchronizedList(
                koblinger.stream()
                .filter(k -> {
                        val organisasjonsenhet = findEnhetByNummer(k.getAgressoId()).orElse(null);
                        val dvhRessurs = findRessursByRessursId(k.getRessursId()).orElse(null);
                        if (organisasjonsenhet == null && dvhRessurs == null) {
                            log.warn("Kobling relasjon misslyktes, enhet: "+k.getAgressoId() + ", ressursId: " + k.getRessursId() + ", " + "Enhet og ressurs mangler");
                        } else if (organisasjonsenhet == null) {
                            log.warn("Kobling relasjon misslyktes, enhet: "+k.getAgressoId() + ", ressursId: " + k.getRessursId() + ", " + "Enhet mangler");
                        } else if (dvhRessurs == null) {
                            log.warn("Kobling relasjon misslyktes, enhet: "+k.getAgressoId() + ", ressursId: " + k.getRessursId() + ", " + "Ressurs mangler");
                        } else {
                            return true;
                        }
                    return false;
                })
                .collect(Collectors.toList())
        );
    }

    public void clearRepository(){
        enheterByNumber.clear();
        ressursByNavIdent.clear();
        organiseringByEnhet.clear();
        koblinger.clear();
    }

    public List<DvhKobling> getKoblingerForRessurs(String resourceId) {
        return findAllKoblinger().stream()
                .filter(s -> s.getRessursId().equals(resourceId))
                .collect(Collectors.toUnmodifiableList());
    }
}
