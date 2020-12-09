package no.nav.nom.apps.api.dvh;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class DvhRepository {

    private final Map<String, DvhOrganisasjonsenhet> enheterByNumber = new ConcurrentHashMap<>(1000);
    private final Map<String, DvhRessurs> ressursByNavIdent = new ConcurrentHashMap<>(1000);
    private final Map<String, DvhLeder> lederByEnhetNr = new ConcurrentHashMap<>(1000);
    private final List<DvhOrganisering> organiseringer = Collections.synchronizedList(new LinkedList<>());

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

    public void saveOrganisering(DvhOrganisering organisering){
        organiseringer.add(organisering);
    }

    public List<DvhOrganisering> getOrganiseringer(){
        return organiseringer;
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

    public void saveOrgLeder(DvhLeder leder){
        lederByEnhetNr.put(leder.getOrgEnhetNr(), leder);
    }

    public Optional<DvhLeder> findLederByEnhetNr(String enhetNr){
        return Optional.ofNullable(lederByEnhetNr.get(enhetNr));
    }

    public void clearRepository(){
        enheterByNumber.clear();
        ressursByNavIdent.clear();
        organiseringer.clear();
    }
}
