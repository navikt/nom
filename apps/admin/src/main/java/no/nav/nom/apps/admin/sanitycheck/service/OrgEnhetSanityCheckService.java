package no.nav.nom.apps.admin.sanitycheck.service;

import no.nav.nom.apps.admin.sanitycheck.dto.orgenhet.EnheterOgOrganisering;
import no.nav.nom.apps.admin.sanitycheck.dto.orgenhet.OrgEnhetStartOgSluttdato;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
