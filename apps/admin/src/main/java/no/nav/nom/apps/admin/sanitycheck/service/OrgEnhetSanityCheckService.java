package no.nav.nom.apps.admin.sanitycheck.service;

import no.nav.nom.apps.admin.sanitycheck.dto.orgenhet.EnheterOgOrganisering;
import no.nav.nom.apps.admin.sanitycheck.dto.orgenhet.OrgEnhetStartOgSluttdato;
import no.nav.nom.apps.admin.sanitycheck.dto.orgenhet.OrgEnhetOrganisering;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Comparator.comparing;

@Service
public class OrgEnhetSanityCheckService {

    private final Logger log = LoggerFactory.getLogger(OrgEnhetSanityCheckService.class);
    private final OrgEnhetStateRepository orgEnhetStateRepository;
    private final OrganiseringStateRepository organiseringStateRepository;

    public OrgEnhetSanityCheckService(OrgEnhetStateRepository orgEnhetStateRepository,
                                      OrganiseringStateRepository organiseringStateRepository) {
        this.orgEnhetStateRepository = orgEnhetStateRepository;
        this.organiseringStateRepository = organiseringStateRepository;
    }

    public OrgEnhetStartOgSluttdato sjekkOrgEnhetStartSluttDato() {
        List<OrgEnhetState> alleEnheter = orgEnhetStateRepository.getAll().stream().toList();

        return new OrgEnhetStartOgSluttdato(
            alleEnheter.size(),
            enheterMedStartdato(alleEnheter).size(),
            enheterMedSluttdato(alleEnheter).size(),
            enheterMedSluttdatoFoerStartdato(enheterMedSluttdato(alleEnheter)).size()
        );
    }

    public EnheterOgOrganisering sjekkAktiveEnheterOgOrganisering() {
        List<OrgEnhetState> alleEnheter = orgEnhetStateRepository.getAll().stream().toList();
        List<OrgEnhetState> aktiveEnheter = aktiveEnheter(alleEnheter);

        int numberOfAktiveOrganiseringer;
        int numberOfAktiveOverEnheter;
        int numberOfAktiveUnderEnheter;
        List<OrgEnhetState> aktiveEnheterUtenAktivOverEnhet;
        List<OrgEnhetState> aktiveEnheterUtenAktivUnderEnhet;
        List<OrgEnhetState> aktiveEnheterMedAktivUnderEnhetUtenAktivOverEnhet;
        List<OrgEnhetState> aktiveEnheterUtenOrganisering;
        log.info("Beregner aktive enheter og organisering med ny implementasjon");

        List<OrganiseringState> aktiveOrganiseringer = organiseringStateRepository.getOrganiseringer();
        numberOfAktiveOrganiseringer = aktiveOrganiseringer.size();

        List<String> aktiveOverEnheterUniqueIder = uniqueIdForOverEnhet(aktiveOrganiseringer);
        numberOfAktiveOverEnheter = aktiveOverEnheterUniqueIder.size();

        List<String> aktiveUnderEnheterUniqueIder = uniqueIdForUnderEnhet(aktiveOrganiseringer);
        numberOfAktiveUnderEnheter = aktiveUnderEnheterUniqueIder.size();


        List<String> aktiveOrganiseringerUniqueIder = new ArrayList<>(aktiveOverEnheterUniqueIder);
        aktiveOrganiseringerUniqueIder.addAll(aktiveUnderEnheterUniqueIder);


        aktiveEnheterUtenAktivOverEnhet = enheterIkkeBlantEnhetIder(aktiveEnheter, aktiveUnderEnheterUniqueIder);
        aktiveEnheterUtenAktivUnderEnhet = enheterIkkeBlantEnhetIder(aktiveEnheter, aktiveOverEnheterUniqueIder);


        aktiveEnheterMedAktivUnderEnhetUtenAktivOverEnhet = enheterBlantEnhetIder(aktiveEnheterUtenAktivOverEnhet, aktiveUnderEnheterUniqueIder);
        aktiveEnheterUtenOrganisering = enheterIkkeBlantEnhetIder(aktiveEnheter, aktiveOrganiseringerUniqueIder);

        return new EnheterOgOrganisering(
            aktiveEnheter.size(),
            numberOfAktiveOrganiseringer,
            numberOfAktiveOverEnheter,
            numberOfAktiveUnderEnheter,
            aktiveEnheterUtenAktivOverEnhet.size(),
            aktiveEnheterUtenAktivUnderEnhet.size(),
            aktiveEnheterMedAktivUnderEnhetUtenAktivOverEnhet.size(),
            aktiveEnheterUtenOrganisering.size()
        );
    }

    public List<OrgEnhetOrganisering> enhetUtenOverEnhet() {
        List<OrgEnhetState> alleEnheter =  orgEnhetStateRepository.getAll().stream().toList();
        List<OrgEnhetState> aktiveEnheter = aktiveEnheter(alleEnheter);

        List<OrgEnhetState> aktiveEnheterUtenAktivOverEnhet;
        log.info("Lister enheter uten overenheter med ny implementasjon");

        List<OrganiseringState> aktiveOrganiseringer = organiseringStateRepository.getOrganiseringer();

        List<String> aktiveUnderEnheterUniqueIder = uniqueIdForUnderEnhet(aktiveOrganiseringer);
        aktiveEnheterUtenAktivOverEnhet = enheterIkkeBlantEnhetIder(aktiveEnheter, aktiveUnderEnheterUniqueIder);

        return aktiveEnheterUtenAktivOverEnhet.stream()
                .map(oe ->
                        new OrgEnhetOrganisering(
                                oe.id(),
                                oe.uniqueAgressoId().orElse(""),
                                oe.gyldigFom(),
                                oe.gyldigTom().orElse(null)))
                .toList();
    }

    public List<OrgEnhetOrganisering> enhetUtenOverEnhetMedUnderEnhet() {
        List<OrgEnhetState> alleEnheter = orgEnhetStateRepository.getAll().stream().toList();
        List<OrgEnhetState> aktiveEnheter = aktiveEnheter(alleEnheter);

        List<OrgEnhetState> aktiveEnheterMedAktivUnderEnhetUtenAktivOverEnhet;
        log.info("Lister enheter uten overenheter med ny implementasjon");

        List<OrganiseringState> aktiveOrganiseringer = organiseringStateRepository.getOrganiseringer();

        List<String> aktiveUnderEnheterUniqueIder = uniqueIdForUnderEnhet(aktiveOrganiseringer);
        List<OrgEnhetState> aktiveEnheterUtenAktivOverEnhet = enheterIkkeBlantEnhetIder(aktiveEnheter, aktiveUnderEnheterUniqueIder);

        aktiveEnheterMedAktivUnderEnhetUtenAktivOverEnhet = enheterBlantEnhetIder(aktiveEnheterUtenAktivOverEnhet, aktiveUnderEnheterUniqueIder);

        return aktiveEnheterMedAktivUnderEnhetUtenAktivOverEnhet.stream()
                .map(oe ->
                        new OrgEnhetOrganisering(
                                oe.id(),
                                oe.uniqueAgressoId().orElse(""),
                                oe.gyldigFom(),
                                oe.gyldigTom().orElse(null)))
                .toList();
    }

    public List<OrgEnhetState> finnDuplikateEnheter() {
        List<OrgEnhetState> alleEnheter = orgEnhetStateRepository.getAll().stream().toList();
        List<OrgEnhetState> aktiveEnheter = aktiveEnheter(alleEnheter);
        return duplikatPaaSammeUniqueAgresssoId(aktiveEnheter).values().stream().flatMap(List::stream).toList();
    }

    private Map<String, List<OrgEnhetState>> duplikatPaaSammeUniqueAgresssoId(List<OrgEnhetState> aktiveEnheter) {
        var duplicateListMap = new HashMap<String, List<OrgEnhetState>>();
        aktiveEnheter.forEach(e -> {
            OrgEnhetState enhetPaaUniqueAgressoId = getEnhetByAgressoIdNivaAndName(e.agressoId().get(), e.agressoOrgNivaa().get(), e.navn()).get();
                if (!enhetPaaUniqueAgressoId.id().equals(e.id())) {
                    List<OrgEnhetState> duplicates = duplicateListMap.computeIfAbsent(enhetPaaUniqueAgressoId.id(), k -> new ArrayList<>());
                    duplicates.add(e);
                }
            }
        );
        return duplicateListMap;
    }

    public Optional<OrgEnhetState> getEnhetByAgressoIdNivaAndName(String agressoId, String orgNiv, String name) {
        if (name != null) {
            return orgEnhetStateRepository.findAllByUniqueAgressoId(agressoId + "-" + name + "-" + orgNiv)
                    .stream()
                    .filter(o -> !o.endedOnStartDate()) // Ikke returner enheter som er opprettet og avsluttet samme dag
                    .max(comparing(OrgEnhetState::gyldigFom));

        }
        return getEnhetByAgressoIdAndOrgniv(agressoId, orgNiv);
    }


    public Optional<OrgEnhetState> getEnhetByAgressoIdAndOrgniv(String agressoId, String orgNiv) {
        return orgEnhetStateRepository.findAllByAgressoIdAndNiv(agressoId + "--" + orgNiv)
            .stream()
            .filter(o -> !o.endedOnStartDate()) // Ikke returner enheter som er opprettet og avsluttet samme dag
            .max(comparing(OrgEnhetState::gyldigFom));
    }

    private List<OrgEnhetState> enheterMedStartdato(List<OrgEnhetState> enheter) {
        return enheter.stream().filter(oe -> oe.gyldigFom() != null).toList();
    }

    private List<OrgEnhetState> enheterMedSluttdato(List<OrgEnhetState> enheter) {
        return enheter.stream().filter(oe -> oe.gyldigTom().isPresent()).toList();
    }

    private List<OrgEnhetState> enheterMedSluttdatoFoerStartdato(List<OrgEnhetState> enheter) {
        return enheter.stream().filter(oe -> oe.gyldigTom().isPresent()).filter(oe -> oe.gyldigTom().get().isBefore(oe.gyldigFom())).toList();
    }

    private List<OrgEnhetState> aktiveEnheter(List<OrgEnhetState> enheter) {
        // FOM er før eller lik dagens dat, samt at TOM er null eller fremtidig
        return enheter.stream().filter(oe ->
                oe.gyldigFom().isBefore(LocalDate.now().plusDays(1)) &&
                        (oe.gyldigTom().isEmpty() || oe.gyldigTom().get().isAfter(LocalDate.now()))
        ).toList();
    }

    public List<String> uniqueIdForOverEnhet(List<OrganiseringState> organiseringer) {
        return organiseringer.stream().map(OrganiseringState::idOver).distinct().toList();
    }

    public List<String> uniqueIdForUnderEnhet(List<OrganiseringState> organiseringer) {
        return organiseringer.stream().map(OrganiseringState::id).distinct().toList();
    }

    private List<OrgEnhetState> enheterBlantEnhetIder(List<OrgEnhetState> enheter, List<String> enhetIder) {
        return enheter.stream().filter(orgenhet -> enhetIder.contains(orgenhet.id())).toList();
    }

    private static List<OrgEnhetState> enheterIkkeBlantEnhetIder(List<OrgEnhetState> enheter, List<String> enehtIder) {
        return enheter.stream().filter(e -> !enehtIder.contains(e.id())).toList();
    }
}
