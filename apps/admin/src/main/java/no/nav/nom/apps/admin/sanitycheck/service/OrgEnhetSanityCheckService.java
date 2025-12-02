package no.nav.nom.apps.admin.sanitycheck.service;

import no.nav.nom.apps.admin.sanitycheck.dto.orgenhet.EnheterOgOrganisering;
import no.nav.nom.apps.admin.sanitycheck.dto.orgenhet.OrgEnhetArbeidsomraadeInfo;
import no.nav.nom.apps.admin.sanitycheck.dto.orgenhet.OrgEnhetOrganisering;
import no.nav.nom.apps.admin.sanitycheck.dto.orgenhet.OrgEnhetStartOgSluttdato;
import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.state.LederTilgangState;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.libs.state.authorize.DelegertLederStateRepository;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.state.orgenhetleder.OrgEnhetLederStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Comparator.comparing;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNullElse;

@Service
public class OrgEnhetSanityCheckService {

    private final Logger log = LoggerFactory.getLogger(OrgEnhetSanityCheckService.class);
    private final OrgEnhetStateRepository orgEnhetStateRepository;
    private final OrganiseringStateRepository organiseringStateRepository;
    private final DelegertLederStateRepository delegertLederStateRepository;
    private final OrgEnhetLederStateRepository orgEnhetLederStateRepository;

    public OrgEnhetSanityCheckService(OrgEnhetStateRepository orgEnhetStateRepository,
                                      OrganiseringStateRepository organiseringStateRepository,
                                      DelegertLederStateRepository delegertLederStateRepository,
                                      OrgEnhetLederStateRepository orgEnhetLederStateRepository
    ) {
        this.orgEnhetStateRepository = orgEnhetStateRepository;
        this.organiseringStateRepository = organiseringStateRepository;
        this.delegertLederStateRepository = delegertLederStateRepository;
        this.orgEnhetLederStateRepository = orgEnhetLederStateRepository;
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
                                Objects.requireNonNullElse(oe.uniqueAgressoId(), ""),
                                oe.gyldigFom(),
                                oe.gyldigTom()))
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
                                Objects.requireNonNullElse(oe.uniqueAgressoId(), ""),
                                oe.gyldigFom(),
                                oe.gyldigTom()))
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
            getEnhetByAgressoIdNivaAndName(e.agressoId(), e.agressoOrgNivaa(), e.navn()).ifPresent(enhet -> {
                if (!enhet.id().equals(e.id())) {
                    List<OrgEnhetState> duplicates = duplicateListMap.computeIfAbsent(enhet.id(), k -> new ArrayList<>());
                    duplicates.add(e);
                }
            });
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
        return enheter.stream().filter(oe -> nonNull(oe.gyldigTom())).toList();
    }

    private List<OrgEnhetState> enheterMedSluttdatoFoerStartdato(List<OrgEnhetState> enheter) {
        return enheter.stream().filter(oe -> nonNull(oe.gyldigTom())).filter(oe -> oe.gyldigTom().isBefore(oe.gyldigFom())).toList();
    }

    public static List<OrgEnhetState> aktiveEnheter(List<OrgEnhetState> enheter) {
        // FOM er før eller lik dagens dat, samt at TOM er null eller fremtidig
        return enheter.stream().filter(oe ->
                oe.gyldigFom().isBefore(LocalDate.now().plusDays(1)) &&
                        (isNull(oe.gyldigTom()) || oe.gyldigTom().isAfter(LocalDate.now()))
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

    public List<OrgEnhetOrganisering> bunnEnheterSomIkkeHarEksakt1ArbeidsomradeIOrghierarkietStier() {
        List<OrgEnhetState> alleEnheter =  orgEnhetStateRepository.getAll().stream().toList();
        List<OrgEnhetState> aktiveEnheter = aktiveEnheter(alleEnheter);
        List<OrganiseringState> aktiveOrganiseringer = organiseringStateRepository.getOrganiseringer();

        List<String> bunnEnheter = aktiveOrganiseringer
            .stream()
            .map(OrganiseringState::id)
            .filter(id -> !givenOrgenhetIsOverenhetToSome(id, aktiveOrganiseringer)) //rules out those who has underenheter
            .filter(this::givenOrgenhetHasOverenhet) // also rules out single orphan orgenheter outside the hierarchy
            .toList();
        //log.info("Lister bunn enheter: {}", bunnEnheter);

        List<OrgEnhetState> bunnEnheterUtenEksakt1arbeidsomradeIHierarkiet = bunnEnheter
            .stream()
            .filter(bunnEnhet -> validHierarchyAndInvalidArbeidsomradeConfig(bunnEnhet, aktiveEnheter, aktiveOrganiseringer))
            .map(bunnEnhet -> getOrgEnhetState(bunnEnhet, aktiveEnheter))
            .filter(Objects::nonNull)
            .toList();

        log.info("Lister bunn enheter som ikke har kksakt 1 arbeidsomrade i orghierarkiet: {}", bunnEnheterUtenEksakt1arbeidsomradeIHierarkiet);

        return bunnEnheterUtenEksakt1arbeidsomradeIHierarkiet.stream()
            .map(oe ->
                new OrgEnhetOrganisering(
                    oe.id(),
                    Objects.requireNonNullElse(oe.uniqueAgressoId(), ""),
                    oe.gyldigFom(),
                    oe.gyldigTom()))
            .toList();
    }

    public OrgEnhetArbeidsomraadeInfo validerArbeidsomraade() {
        var alleEnheter =  orgEnhetStateRepository.getAll().stream().toList(); // Aktive enheter i organisasjonstreet
        var alleAktiveEnheter  = aktiveEnheter(alleEnheter);

        var alleIdOver = organiseringStateRepository.getActiveOrganisering().values().stream()
                .map(OrganiseringState::idOver).toList();
        var alleLeafNoder = alleAktiveEnheter.stream().filter(o -> !alleIdOver.contains(o.id())).toList();
        var alleIdUnder = organiseringStateRepository.getActiveOrganisering().values().stream()
            .map(OrganiseringState::id).toList();

        List<OrgEnhetArbeidsomraadeInfo.LoevnodeMedFeilAntallArbOmr> loevnoderMedFeilAntallArbOmr = new ArrayList<>();
        alleLeafNoder.forEach(orgenhet -> {
            var count = findArbeidsomraadeCountInHierarkiAbove(orgenhet);
            if (count != 1 && !manglerOvernode(orgenhet, alleIdUnder)) {
                loevnoderMedFeilAntallArbOmr.add(
                        OrgEnhetArbeidsomraadeInfo.LoevnodeMedFeilAntallArbOmr.builder()
                                .orgenhetId(orgenhet.id())
                                .orgenhetNavn(orgenhet.navn())
                                .antallArbOmr(count)
                                .build()
                );
            }
        });

        return OrgEnhetArbeidsomraadeInfo.builder()
                .antallLoevnoder(loevnoderMedFeilAntallArbOmr.size())
                .loevnoderMedFeil(loevnoderMedFeilAntallArbOmr)
                .build();
    }

    public List<OrgEnhetOrganisering> listOrgenheterUtenLeder() {
        List<OrgEnhetState> aktiveEnheter = aktiveEnheter(orgEnhetStateRepository.getAll().stream().toList());

        List<OrgEnhetState> orgEnheterMedOrganisering = orgEnheterMedAktivOrganisering(aktiveEnheter);

        List<OrgEnhetState> alleOrgEnheterUtenLeder = new ArrayList<>();

        orgEnheterMedOrganisering.forEach(orgenhet ->
            orgEnhetLederStateRepository.getActiveForOrgEnhet(orgenhet.id())
                .ifPresentOrElse(
                    _ -> {},                // Har leder, gjør ingenting
                    () -> alleOrgEnheterUtenLeder.add(orgenhet) // Har ikke leder
                )
        );

        return alleOrgEnheterUtenLeder.stream()
            .map(oe ->
                new OrgEnhetOrganisering(
                    oe.id(),
                    Objects.requireNonNullElse(oe.uniqueAgressoId(), ""),
                    oe.gyldigFom(),
                    oe.gyldigTom()))
            .toList();
    }

    public List<OrgEnhetState> orgEnheterMedAktivOrganisering(List<OrgEnhetState> orgEnheter) {
        return orgEnheter.stream()
            .filter(orgEnhet -> organiseringStateRepository.orgEnhetHasParent(orgEnhet.id()))
            .toList();
    }

    private boolean manglerOvernode(OrgEnhetState orgenhet, List<String> alleIdUnder) {
        return alleIdUnder.stream().noneMatch(idUnder -> idUnder.equals(orgenhet.id()));
    }

    private int findArbeidsomraadeCountInHierarkiAbove(OrgEnhetState orgenhet) {
        boolean isArbOmr = NomNivaa.ARBEIDSOMRAADE.equals(orgenhet.nomNivaa());
        AtomicInteger countAbove = new AtomicInteger();
        organiseringStateRepository.findActiveById(orgenhet.id())
                .ifPresent(org -> countAbove.set(
                        findArbeidsomraadeCountInHierarkiAbove(orgEnhetStateRepository.findById(org.idOver()).get())
                ));
        return countAbove.get() + (isArbOmr ? 1 : 0);
    }

    private static OrgEnhetState getOrgEnhetState(String orgEnhetId, List<OrgEnhetState> aktiveEnheter) {
        return aktiveEnheter.
            stream()
            .filter(oe -> oe.id().equals(orgEnhetId))
            .findFirst().orElse(null);
    }

    private static OrganiseringState getOrganiseringState(String orgEnhetId, List<OrganiseringState> aktiveOrganiseringer) {
        return aktiveOrganiseringer.
            stream()
            .filter(oe -> oe.id().equals(orgEnhetId))
            .findFirst().orElse(null);
    }

    private boolean givenOrgenhetIsOverenhetToSome(String orgEnhetId, List<OrganiseringState> organiseringer) {
        return organiseringer.stream().anyMatch(organisering -> orgEnhetId.equals(organisering.idOver()));
    }

    private boolean givenOrgenhetHasOverenhet(String orgEnhetId) {
        Optional<OrganiseringState> organiseringState = organiseringStateRepository.findActiveById(orgEnhetId);
        return organiseringState.isPresent() && organiseringState.get().idOver() != null;
    }

    private boolean validHierarchyAndInvalidArbeidsomradeConfig(
        String orgEnhetId,
        List<OrgEnhetState> aktiveEnheter,
        List<OrganiseringState> aktiveOrganiseringer
    ) {
        OrgEnhetState orgEnhetState = getOrgEnhetState(orgEnhetId, aktiveEnheter);
        if (orgEnhetState == null) {
            return false;
        }

        List<OrgEnhetState> orgenheterOver = findAllOrgenheterOver(orgEnhetState, aktiveEnheter, aktiveOrganiseringer);

        var validHierarchy = orgenheterOver
            .stream()
            .filter(oe -> Objects.equals(oe.agressoOrgNivaa(), "ORGNIV0")
                    && Objects.equals(oe.agressoId(), "NAV"))
            .count() == 1;

        long countArbeidsomrade = Objects.equals(orgEnhetState.nomNivaa(), NomNivaa.ARBEIDSOMRAADE) ? 1 : 0;
        long countArbeidsomradeOver = orgenheterOver
            .stream()
            .filter(oe -> Objects.equals(oe.nomNivaa(), NomNivaa.ARBEIDSOMRAADE))
            .count();
        countArbeidsomrade += countArbeidsomradeOver;
        var invalidArbeidomradeConfig = countArbeidsomrade != 1;

        return validHierarchy && invalidArbeidomradeConfig;
    }

    private List<OrgEnhetState> findAllOrgenheterOver(OrgEnhetState orgEnhetState,
                                                      List<OrgEnhetState> aktiveEnheter,
                                                      List<OrganiseringState> aktiveOrganiseringer) {
        return findAllOrgenheterOver(orgEnhetState, new ArrayList<>(), aktiveEnheter, aktiveOrganiseringer);
    }

    private List<OrgEnhetState> findAllOrgenheterOver(
        OrgEnhetState orgEnhetState,
        ArrayList<OrgEnhetState> orgenheterSoFar,
        List<OrgEnhetState> aktiveEnheter,
        List<OrganiseringState> aktiveOrganiseringer
    ) {
        if (orgEnhetState == null) {
            return orgenheterSoFar;
        }

        OrganiseringState organiseringState = getOrganiseringState(orgEnhetState.id(), aktiveOrganiseringer);
        //log.info("In findAllOrgenheterOver orgEnhetState: {}, organiseringState: {}", orgEnhetState, organiseringState);

        if(organiseringState == null) {
            return orgenheterSoFar;
        }

        Optional<OrgEnhetState> overOrgEnhenState = Optional.ofNullable(getOrgEnhetState(organiseringState.idOver(), aktiveEnheter));
        if(overOrgEnhenState.isEmpty()) {
                return orgenheterSoFar;
        }

        return overOrgEnhenState
            .map(ooe -> {
                orgenheterSoFar.add(ooe);
                return findAllOrgenheterOver(ooe, orgenheterSoFar, aktiveEnheter, aktiveOrganiseringer);
            })
            .orElse(orgenheterSoFar);
    }

    public List<OrgEnhetOrganisering> getOrgenheterMedAktiveDeliveringerSomLiggerUtenforHierarki() {
        List<OrgEnhetState> alleEnheter =  orgEnhetStateRepository.getAll().stream().toList();
        List<OrgEnhetState> aktiveEnheter = aktiveEnheter(alleEnheter);
        List<OrganiseringState> aktiveOrganiseringer = organiseringStateRepository.getOrganiseringer();

        var alleAktiveDelegeringer = delegertLederStateRepository.getAll().stream().filter(LederTilgangState::isActive).toList();
        var relevanteDelegeringer = alleAktiveDelegeringer.stream()
            .filter(delegering -> !validHierarchy(delegering.orgenhetId(), aktiveEnheter, aktiveOrganiseringer))
            .toList();

        return relevanteDelegeringer.stream()
            .map(delegering -> getOrgEnhetState(delegering.orgenhetId(), aktiveEnheter))
            .filter(Objects::nonNull)
            .map(oe ->
                new OrgEnhetOrganisering(
                    oe.id(),
                    requireNonNullElse(oe.uniqueAgressoId(), ""),
                    oe.gyldigFom(),
                    oe.gyldigTom()))
            .toList();
    }

    private boolean validHierarchy(
        String orgEnhetId,
        List<OrgEnhetState> aktiveEnheter,
        List<OrganiseringState> aktiveOrganiseringer
    ) {
        OrgEnhetState orgEnhetState = getOrgEnhetState(orgEnhetId, aktiveEnheter);
        if (orgEnhetState == null) {
            return false;
        }

        List<OrgEnhetState> orgenheterOver = findAllOrgenheterOver(orgEnhetState, aktiveEnheter, aktiveOrganiseringer);

        return orgenheterOver
            .stream()
            .filter(oe ->
                Objects.equals(oe.agressoOrgNivaa(), "ORGNIV0")
                    && Objects.equals(oe.agressoId(), "NAV"))
            .count() == 1;
    }

}
