package no.nav.nom.apps.api.dvh;

import lombok.RequiredArgsConstructor;
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

@Component
@RequiredArgsConstructor
public class DvhRepository {

    private final Map<String, DvhOrganisasjonsenhet> enheterByNumber = new ConcurrentHashMap<>(1000);
    private final Map<String, DvhRessurs> ressursByNavIdent = new ConcurrentHashMap<>(1000);
    private final Map<String, List<DvhLeder>> lederByEnhetNr = new ConcurrentHashMap<>(1000);
    private final List<DvhOrganisering> organiseringer = Collections.synchronizedList(new LinkedList<>());
    private final List<DvhKobling> koblinger = Collections.synchronizedList(new LinkedList<>());

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

    public Collection<DvhKobling> findAllKoblinger() {
        return koblinger;
    }

    public void saveOrgLeder(DvhLeder leder){
        val lederMapping = lederByEnhetNr.get(leder.getOrgEnhetNr());

        if(lederMapping == null) {
            lederByEnhetNr.put(leder.getOrgEnhetNr(), new LinkedList<>(List.of(leder)));
        }else {
            lederMapping.add(leder);
        }
    }

    public void saveKobling(DvhKobling kobling){
        val organisasjonsenhet = findEnhetByNummer(kobling.getAgressoId()).orElse(null);
        val dvhRessurs = findRessursByRessursId(kobling.getRessursId()).orElse(null);
        if (organisasjonsenhet == null && dvhRessurs == null){
            System.out.println(kobling.getAgressoId()+"\t"+kobling.getRessursId()+"\t"+"Enhet og ressurs mangler");
        }
        else if (organisasjonsenhet == null){
            System.out.println(kobling.getAgressoId()+"\t"+kobling.getRessursId()+"\t"+"Enhet mangler");

        }
        else if (dvhRessurs == null){
            System.out.println(kobling.getAgressoId()+"\t"+kobling.getRessursId()+"\t"+"Ressurs mangler");
        }
        else {
            koblinger.add(kobling);
        }
    }

    public List<DvhKobling> getKoblingerForEnhet(String enhetId){
        return koblinger.stream().filter(x -> x.getAgressoId().equals(enhetId)).collect(Collectors.toUnmodifiableList());
    }

    public List<DvhLeder> findLederByEnhetNr(String enhetNr){
        return Optional.ofNullable(lederByEnhetNr.get(enhetNr))
                .orElse(Collections.emptyList());
    }


    public void clearKoblinger(){
        koblinger.clear();
    }

    public void clearRepository(){
        enheterByNumber.clear();
        ressursByNavIdent.clear();
        organiseringer.clear();
        koblinger.clear();
    }

    public List<DvhKobling> getKoblingerForRessurs(String resourceId) {
        return findAllKoblinger().stream()
                .filter(s -> s.getRessursId().equals(resourceId))
                .collect(Collectors.toUnmodifiableList());
    }
}
