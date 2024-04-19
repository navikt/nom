package no.nav.nom.apps.api.rest.admin;

import io.getunleash.Unleash;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.dvh.dto.DvhOrganisering;
import no.nav.nom.apps.api.organisering.eventsourcing.state.OrganiseringStateRepository;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.apps.api.orgtilknytning.contract.Orgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil;
import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static no.nav.nom.apps.api.organisering.OrganiseringConfig.UNLEASH_USE_NEW_ORGANISASJON_SOURCE;

@Tag(name = "Admin for å kjøre en sanity check på data")
@RestController
@RequestMapping("/api/admin/SanityCheck")
@PreAuthorize("hasAuthority('ADMIN')")
public class SanityCheck {
    private final Logger log = LoggerFactory.getLogger(SanityCheck.class);
    private final DvhRepository dvhRepository;
    private final RessursStateRepository ressursStateRepository;
    private final OrgenhetService orgenhetService;
    private final OrgtilknytningService orgtilknytningService;
    private final OrganiseringStateRepository organiseringStateRepository;
    private final Unleash unleash;

    public SanityCheck(
        DvhRepository dvhRepository,
        RessursStateRepository ressursStateRepository,
        OrgenhetService orgenhetService, OrgtilknytningService orgtilknytningService,
        OrganiseringStateRepository organiseringStateRepository,
        Unleash unleash
    ) {
        this.dvhRepository = dvhRepository;
        this.ressursStateRepository = ressursStateRepository;
        this.orgenhetService = orgenhetService;
        this.orgtilknytningService = orgtilknytningService;
        this.organiseringStateRepository = organiseringStateRepository;
        this.unleash = unleash;
    }

    @Operation(
        description = "Sjekk enheter antall og dato",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/enheter")
    public String dataSanityCheckEnheter() {
        StringBuffer result = new StringBuffer();

        result.append(sjekkOrgEnhetStartSluttDato());

        return result.toString();
    }

    @Operation(
        description = "Sjekk aktive enheter og organiseringer",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/enheterOgOrganisering")
    public String dataSanityCheckEnheterOgOrganisering() {
        StringBuffer result = new StringBuffer();

        result.append(sjekkAktiveEnheterOgOrganisering());

        return result.toString();
    }

    @Operation(
        description = "Sjekk aktive enheter og organiseringer",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/ressurser")
    public String dataSanityCheckRessurser() {
        StringBuffer result = new StringBuffer();

        result.append(aktiveRessurser());

        return result.toString();
    }

    @Operation(
        description = "List aktive enheter Uten over-enhet",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/listEnheterUtenOverEnheter")
    public String listEnheterUtenOverEnheter() {
        StringBuffer result = new StringBuffer();

        List<IOrgenhet> alleEnheter = (List<IOrgenhet>) orgenhetService.getAllEnheter();
        List<IOrgenhet> aktiveEnheter = aktiveEnheter(alleEnheter);

        List<IOrgenhet> aktiveEnheterUtenAktivOverEnhet;

        if (unleash.isEnabled(UNLEASH_USE_NEW_ORGANISASJON_SOURCE)) {
            log.info("Lister enheter uten overenheter med ny implementasjon");

            List<OrganiseringState> aktiveOrganiseringer = organiseringStateRepository.getOrganiseringer();

            List<String> aktiveUnderEnheterUniqueIder = uniqueIdForUnderEnhet(aktiveOrganiseringer);
            aktiveEnheterUtenAktivOverEnhet = enheterIkkeBlantEnhetIder(aktiveEnheter, aktiveUnderEnheterUniqueIder);

        } else {
            log.info("Lister enheter uten overenheter med gammel implementasjon");

            List<DvhOrganisering> alleOrganiseringer = dvhRepository.getOrganiseringer();
            List<DvhOrganisering> aktiveOrganiseringer = aktiveOrganiseringer(alleOrganiseringer);

            List<String> aktiveUnderEnheterUniqueAgressoIder = uniqueAgressoIdForUnderEnhet(aktiveOrganiseringer);
            aktiveEnheterUtenAktivOverEnhet = enheterIkkeBlandtAgressoIder(aktiveEnheter, aktiveUnderEnheterUniqueAgressoIder);
        }

        aktiveEnheterUtenAktivOverEnhet.forEach(oe ->
            result.append(oe.getId())
                .append(";")
                .append(oe.getUniqueAgressoId())
                .append(";")
                .append(oe.getGyldigFom())
                .append(";")
                .append(oe.getGyldigTom().isPresent() ? oe.getGyldigTom().get() : "")
                .append("\n")
        );

        return result.toString();
    }

    @Operation(
        description = "List aktive enheter Uten over-enhet men med under-enheter",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/listEnheterUtenOverEnheterMedUnderEnheter")
    public String listEnheterUtenOverEnheterMedUnderEnheter() {
        StringBuffer result = new StringBuffer();

        List<IOrgenhet> alleEnheter = (List<IOrgenhet>) orgenhetService.getAllEnheter();
        List<IOrgenhet> aktiveEnheter = aktiveEnheter(alleEnheter);

        List<IOrgenhet> aktiveEnheterMedAktivUnderEnhetUtenAktivOverEnhet;

        if (unleash.isEnabled(UNLEASH_USE_NEW_ORGANISASJON_SOURCE)) {
            log.info("Lister enheter uten overenheter med ny implementasjon");

            List<OrganiseringState> aktiveOrganiseringer = organiseringStateRepository.getOrganiseringer();

            List<String> aktiveUnderEnheterUniqueIder = uniqueIdForUnderEnhet(aktiveOrganiseringer);
            List<IOrgenhet> aktiveEnheterUtenAktivOverEnhet = enheterIkkeBlantEnhetIder(aktiveEnheter, aktiveUnderEnheterUniqueIder);

            aktiveEnheterMedAktivUnderEnhetUtenAktivOverEnhet = enheterBlantEnhetIder(aktiveEnheterUtenAktivOverEnhet, aktiveUnderEnheterUniqueIder);

        } else {
            log.info("Lister enheter uten overenheter med gammel implementasjon");

            List<DvhOrganisering> alleOrganiseringer = dvhRepository.getOrganiseringer();
            List<DvhOrganisering> aktiveOrganiseringer = aktiveOrganiseringer(alleOrganiseringer);

            List<String> aktiveUnderEnheterUniqueAgressoIder = uniqueAgressoIdForUnderEnhet(aktiveOrganiseringer);
            List<IOrgenhet> aktiveEnheterUtenAktivOverEnhet = enheterIkkeBlandtAgressoIder(aktiveEnheter, aktiveUnderEnheterUniqueAgressoIder);

            aktiveEnheterMedAktivUnderEnhetUtenAktivOverEnhet = enheterForAgressoIder(aktiveEnheterUtenAktivOverEnhet, aktiveUnderEnheterUniqueAgressoIder);
        }

        aktiveEnheterMedAktivUnderEnhetUtenAktivOverEnhet.stream().forEach(oe ->
            result.append(oe.getId() + ";" + oe.getUniqueAgressoId() + ";" + oe.getGyldigFom() + ";" + (oe.getGyldigTom().isPresent() ? oe.getGyldigTom()
                .get() : "") + "\n")
        );

        return result.toString();
    }

    @Operation(
        description = "List ressurser med aktive orgtilknytninger for enhet om er inaktiv eller uten organisering",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/listRessurserTilIkkeAktivEnhet")
    public String listRessurserTilIkkeAktivEnhet(@RequestParam(defaultValue = "false") boolean inkluderEksterne) {
        StringBuffer result = new StringBuffer();

        List<RessursState> alleRessurser = inkluderEksterne
            ? ressursStateRepository.getAll().stream().toList()
            : ressursStateRepository.getAll().stream().filter(r -> !r.sektor().equals(Sektor.EKSTERN)).toList();

        List<IOrgenhet> alleEnheter = (List<IOrgenhet>) orgenhetService.getAllEnheter();

        List<IOrgenhet> enheterIkkeAktiveEllerUtenOrganisering;

        if (unleash.isEnabled(UNLEASH_USE_NEW_ORGANISASJON_SOURCE)) {
            log.info("Finner ressurser knyttet til ikke-aktiv enhet med ny implementasjon");

            List<OrganiseringState> aktiveOrganiseringer = organiseringStateRepository.getOrganiseringer();

            List<String> aktiveOverEnheterUniqueIder = uniqueIdForOverEnhet(aktiveOrganiseringer);
            List<String> aktiveUnderEnheterUniqueIder = uniqueIdForUnderEnhet(aktiveOrganiseringer);

            List<String> aktiveOrganiseringerUniqueIder = new ArrayList<>(aktiveOverEnheterUniqueIder);
            aktiveOrganiseringerUniqueIder.addAll(aktiveUnderEnheterUniqueIder);

            enheterIkkeAktiveEllerUtenOrganisering = enheterIkkeBlantEnhetIder(alleEnheter, aktiveOrganiseringerUniqueIder);

        } else {
            log.info("Finner ressurser knyttet til ikke-aktiv enhet med gammel implementasjon");

            List<DvhOrganisering> alleOrganiseringer = dvhRepository.getOrganiseringer();
            List<DvhOrganisering> aktiveOrganiseringer = aktiveOrganiseringer(alleOrganiseringer);

            List<String> aktiveOverEnheterUniqueAgressoIder = uniqueAgressoIdForOverEnhet(aktiveOrganiseringer);
            List<String> aktiveUnderEnheterUniqueAgressoIder = uniqueAgressoIdForUnderEnhet(aktiveOrganiseringer);

            List<String> aktiveOrganiseringerUniqueAgressoIder = new ArrayList<>(aktiveOverEnheterUniqueAgressoIder);
            aktiveOrganiseringerUniqueAgressoIder.addAll(aktiveUnderEnheterUniqueAgressoIder);

            enheterIkkeAktiveEllerUtenOrganisering = enheterIkkeBlandtAgressoIder(alleEnheter, aktiveOrganiseringerUniqueAgressoIder);
        }

        var alleOrgtilknytningState = orgtilknytningService.getAll();
        var aktiveOrgtilknytningState = aktiveOrgtilknytninger(alleOrgtilknytningState);

        List<RessursState> ressurserMedAktiveTilknytningerTilEnheterIkkeAktiveEllerUtenOrganisering = null;
        List<String> navIdenterMedAktiveTilknytninger = aktiveOrgtilknytningState.stream().map(OrgtilknytningState::navident).toList();
        List<RessursState> ressurserMedAktiveTilknytninger = ressurserForNavIdenter(alleRessurser, navIdenterMedAktiveTilknytninger);

        List<String> enhetIderEnheterIkkeAktiveEllerUtenOrganisering = enheterIkkeAktiveEllerUtenOrganisering.stream().map(IOrgenhet::getId).toList();
        ressurserMedAktiveTilknytningerTilEnheterIkkeAktiveEllerUtenOrganisering = ressursKoblingDagensDatoForEnhetIder(
            ressurserMedAktiveTilknytninger,
            aktiveOrgtilknytningState,
            enhetIderEnheterIkkeAktiveEllerUtenOrganisering
        );

        ressurserMedAktiveTilknytningerTilEnheterIkkeAktiveEllerUtenOrganisering.forEach(r ->
            {
                result.append(r.navident()).append(" [").append(r.fid()).append("]  - ").append(r.sektor()).append(" => ");
                StringBuffer enheter = new StringBuffer();
                aktiveOrgtilknytningState.stream()
                    .filter(ot -> ot.navident().equals(r.navident()))
                    .forEach(ot -> {
                            var enhet = orgenhetService.getById(ot.orgEnhetId()).get();
                            enheter.append(enhet.getId())
                                .append(" - ")
                                .append(enhet.getNavn())
                                .append("[")
                                .append(enhet.getUniqueAgressoId())
                                .append("], ");
                        }
                    );
                if (enheter.isEmpty()) {
                    result.append(" What? - ingen orgenhet i tilknytninger?\n");
                } else {
                    result.append(enheter.substring(0, enheter.length() - 2)).append("\n");
                }
            }
        );

        result.append("\n");

        //        if (unleash.isEnabled(UNLEASH_USE_NEW_ORGANISASJON_SOURCE)) {
        //            List<String> uniqueAgressoIderEnheterIkkeAktiveEllerUtenOrganisering = enheterIkkeAktiveEllerUtenOrganisering.stream()
        //                .map(IOrgenhet::getUniqueAgressoId)
        //                .toList();
        //
        //            uniqueAgressoIderEnheterIkkeAktiveEllerUtenOrganisering.forEach(s -> result.append(s).append("\n"));
        //        } else {
        //            List<String> uniqueIderEnheterIkkeAktiveEllerUtenOrganisering = enheterIkkeAktiveEllerUtenOrganisering.stream()
        //                .map(IOrgenhet::getId)
        //                .toList();
        //
        //            uniqueIderEnheterIkkeAktiveEllerUtenOrganisering.forEach(s -> result.append(s).append("\n"));
        //        }

        return result.toString();
    }

    @Operation(
        description = "List tilknytninger med gyldighet utover enhetens gyldighetsperiode",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/listTilkoblingerUtoverOgEnhetGyldighetPeriode")
    public String listTilkoblingerUtoverOgEnhetGyldighetPeriode() {
        StringBuffer result = new StringBuffer();
        var alleOrgtilknytningState = orgtilknytningService.getAll();
        var aktiveOrgtilknytningState = aktiveOrgtilknytninger(alleOrgtilknytningState);
        var aktiveTilknytningerUtoverEnhetPeriode = orgtilknytningerUtoverEnhetPeriode(aktiveOrgtilknytningState);

        aktiveTilknytningerUtoverEnhetPeriode.forEach(t -> {
            var oe = orgenhetService.getById(t.orgEnhetId()).get();
            result.append(t.navident())
                .append(";")
                .append(oe.getUniqueAgressoId())
                .append(";")
                .append(t.orgEnhetId())
                .append(";")
                .append(t.gyldigFom())
                .append(";")
                .append(t.gyldigTom().isPresent() ? t.gyldigTom().get() : "")
                .append(";")
                .append(oe.getGyldigFom())
                .append(";")
                .append(oe.getGyldigTom().isPresent() ? oe.getGyldigTom().get() : "")
                .append("\n");
        });

        return result.toString();
    }

    @Operation(
        description = "List aktive enheter som er duplikat på samme unique agresso id ('agressoId+orgNiv+Navn')",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/listDuplikateEnheter")
    public String listDuplikateEnheter() {
        StringBuffer result = new StringBuffer();

        List<IOrgenhet> alleEnheter = (List<IOrgenhet>) orgenhetService.getAllEnheter();
        List<IOrgenhet> aktiveEnheter = aktiveEnheter(alleEnheter);
        Map<String, List<IOrgenhet>> aktiveEnheterDuplikatAvSammeUniqueAgressoId = duplikatPaaSammeUniqueAgresssoId(aktiveEnheter);

        result.append("Totalt antall duplikater: %s\n".formatted(aktiveEnheterDuplikatAvSammeUniqueAgressoId.size()));
        aktiveEnheterDuplikatAvSammeUniqueAgressoId.forEach((faktiskId, duplikater) -> {
            IOrgenhet faktisk = orgenhetService.getById(faktiskId).get();
            String uniqueAgressoId = faktisk.getAgressoId() + "-" + faktisk.getNavn() + "-" + faktisk.getOrgNiv();
            String periode = faktisk.getGyldigFom().toString() + " - " + (faktisk.getGyldigTom().isPresent() ? faktisk.getGyldigTom().get() : " ");
            result.append("   Enhet %s for UniqueAgressoId %s og periode [%s] har duplikater:\n".formatted(faktiskId, uniqueAgressoId, periode));
            duplikater.forEach(duplikat -> {
                String duplikatUniqueAgressoId = duplikat.getAgressoId() + "-" + duplikat.getNavn() + "-" + duplikat.getOrgNiv();
                String duplikatPeriode = duplikat.getGyldigFom().toString() + " - " + (duplikat.getGyldigTom().isPresent() ? duplikat.getGyldigTom()
                    .get() : " ");
                result.append("      %s (%s) periode [%s]\n".formatted(duplikat.getId(), duplikatUniqueAgressoId, duplikatPeriode));
            });
        });

        return result.toString();
    }

    @Operation(
        description = "List duplikate ressurser på samme navident",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/duplikateRessurserPaaNavident")
    public String duplikateRessurserPaaNavident() {
        StringBuffer result = new StringBuffer();

        var ressurserByFid = ressursStateRepository.getAllByFid();
        var ressurserByNavident = ressursStateRepository.getAll();

        result.append("Ant ressurser ByFid/ByNavident: %s/%s\n".formatted(ressurserByFid.size(), ressurserByNavident.size()));

        log.info(result.toString());

        Set<String> fidNavidenter = new HashSet<>();
        var duplicateRessurser = ressurserByFid.stream()
            .filter(n -> !fidNavidenter.add(n.navident()))
            .toList();

        result.append("Ant duplikater på navident: %s\n".formatted(duplicateRessurser.size()));
        log.info(result.toString());
        duplicateRessurser.forEach(r -> {
            result.append("      %s (%s)\n".formatted(r.fid(), r.visningsnavn().toString()));
        });

        return result.toString();
    }

    @Operation(
        description = "List orgtilknytninger med negativ periode (gyldigTom < gyldigFom)",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/orgtilknytningNegativPeriode")
    public List<OrgtilknytningState> orgtilknytningNegativPeriode() {
        return orgtilknytningService.getAll().stream()
            .filter(ot -> ot.gyldigTom().isPresent() && ot.gyldigTom().get().isBefore(ot.gyldigFom()))
            .toList();
    }

    @Operation(
        description = "Valider angivelse av ARBEIDSOMRÅDE på enheter",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/validerArbeidsomraade")
    public String validerArbeidsomraade() {
        var alleEnheter = orgenhetService.getAllEnheterMedOrganisering(); // Aktive enheter i organisasjonstreet
        var alleIdOver = organiseringStateRepository.getActiveOrganisering().values().stream().map(OrganiseringState::idOver).toList();
        var alleLeafNoder = alleEnheter.stream().filter(o -> !alleIdOver.contains(o.getId())).toList();

        StringBuffer result = new StringBuffer();
        result.append("Antall løvnoder: %s\n".formatted(alleLeafNoder.size()));

        AtomicInteger branchesMedFeilAntAvArbomr = new AtomicInteger();
        alleLeafNoder.forEach(orgenhet -> {
            var count = findArbeidsomraadeCountInHierarkiAbove(orgenhet);
            if (count != 1) {
                branchesMedFeilAntAvArbomr.getAndIncrement();
                result.append("%s %s har %s arbeidsområder over seg\n".formatted(orgenhet.getId(), orgenhet.getNavn(), count));
            }
        });
        return result.toString();
    }

    private int findArbeidsomraadeCountInHierarkiAbove(IOrgenhet orgenhet) {
        boolean isArbOmr = NomNivaa.ARBEIDSOMRAADE.equals(orgenhet.getNomNivaa().orElse(null));
        AtomicInteger countAbove = new AtomicInteger();
        organiseringStateRepository.findActiveById(orgenhet.getId()).ifPresent(org -> {
            countAbove.set(findArbeidsomraadeCountInHierarkiAbove(orgenhetService.getById(org.idOver()).get()));
        });
        return countAbove.get() + (isArbOmr ? 1 : 0);
    }

    private String sjekkOrgEnhetStartSluttDato() {
        StringBuffer result = new StringBuffer();

        List<IOrgenhet> alleEnheter = (List<IOrgenhet>) orgenhetService.getAllEnheter();

        result.append("Totalt antall enheter: %s\n".formatted(alleEnheter.size()));
        result.append("   Med startdato: %s\n".formatted(enheterMedStartdato(alleEnheter).size()));
        result.append("   Med sluttdato: %s\n".formatted(enheterMedSluttdato(alleEnheter).size()));
        result.append("   Med sluttdato før startdato: %s\n\n".formatted(enheterMedSluttdatoFoerStartdato(enheterMedSluttdato(alleEnheter)).size()));
        log.info("Resultat so far:\n{}", result);
        return result.toString();
    }

    private String sjekkAktiveEnheterOgOrganisering() {
        StringBuffer result = new StringBuffer();

        List<IOrgenhet> alleEnheter = (List<IOrgenhet>) orgenhetService.getAllEnheter();
        List<IOrgenhet> aktiveEnheter = aktiveEnheter(alleEnheter);

        int numberOfAktiveOrganiseringer;
        int numberOfAktiveOverEnheter;
        int numberOfAktiveUnderEnheter;
        List<IOrgenhet> aktiveEnheterUtenAktivOverEnhet;
        List<IOrgenhet> aktiveEnheterUtenAktivUnderEnhet;
        List<IOrgenhet> aktiveEnheterMedAktivUnderEnhetUtenAktivOverEnhet;
        List<IOrgenhet> aktiveEnheterUtenOrganisering;
        Map<String, List<IOrgenhet>> aktiveEnheterDuplikatAvSammeUniqueAgressoId;

        if (unleash.isEnabled(UNLEASH_USE_NEW_ORGANISASJON_SOURCE)) {
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


            aktiveEnheterDuplikatAvSammeUniqueAgressoId = Collections.emptyMap();

        } else {
            log.info("Beregner aktive enheter og organisering med gammel implementasjon");

            List<DvhOrganisering> alleOrganiseringer = dvhRepository.getOrganiseringer();

            List<DvhOrganisering> aktiveOrganiseringer = aktiveOrganiseringer(alleOrganiseringer);
            numberOfAktiveOrganiseringer = aktiveOrganiseringer.size();

            List<String> aktiveOverEnheterUniqueAgressoIder = uniqueAgressoIdForOverEnhet(aktiveOrganiseringer);
            numberOfAktiveOverEnheter = aktiveOverEnheterUniqueAgressoIder.size();

            List<String> aktiveUnderEnheterUniqueAgressoIder = uniqueAgressoIdForUnderEnhet(aktiveOrganiseringer);
            numberOfAktiveUnderEnheter = aktiveUnderEnheterUniqueAgressoIder.size();

            List<String> aktiveOrganiseringerUniqueAgressoIder = new ArrayList<>(aktiveOverEnheterUniqueAgressoIder);
            aktiveOrganiseringerUniqueAgressoIder.addAll(aktiveUnderEnheterUniqueAgressoIder);

            aktiveEnheterUtenAktivOverEnhet = enheterIkkeBlandtAgressoIder(aktiveEnheter, aktiveUnderEnheterUniqueAgressoIder);
            aktiveEnheterUtenAktivUnderEnhet = enheterIkkeBlandtAgressoIder(aktiveEnheter, aktiveOverEnheterUniqueAgressoIder);

            aktiveEnheterMedAktivUnderEnhetUtenAktivOverEnhet = enheterForAgressoIder(aktiveEnheterUtenAktivOverEnhet, aktiveUnderEnheterUniqueAgressoIder);
            aktiveEnheterUtenOrganisering = enheterIkkeBlandtAgressoIder(aktiveEnheter, aktiveOrganiseringerUniqueAgressoIder);

            aktiveEnheterDuplikatAvSammeUniqueAgressoId = duplikatPaaSammeUniqueAgresssoId(aktiveEnheter);

        }


        result.append("Aktive enheter: %s\n".formatted(aktiveEnheter.size()));

        result.append("     Aktive organiseringer: %s\n".formatted(numberOfAktiveOrganiseringer));
        result.append("        Antall over-enheter: %s\n".formatted(numberOfAktiveOverEnheter));
        result.append("        Antall under-enheter: %s\n".formatted(numberOfAktiveUnderEnheter));

        result.append("     Aktive enheter uten OverEnheter: %s\n".formatted(aktiveEnheterUtenAktivOverEnhet.size()));
        result.append("     Aktive enheter uten UnderEnheter: %s\n".formatted(aktiveEnheterUtenAktivUnderEnhet.size()));

        result.append("     Aktive enheter med UnderEnheter uten OverEnheter: %s\n".formatted(aktiveEnheterMedAktivUnderEnhetUtenAktivOverEnhet.size()));
        result.append("     Aktive enheter uten noen organisering: %s\n\n".formatted(aktiveEnheterUtenOrganisering.size()));

        result.append("     Duplikate enheter: %s\n".formatted(aktiveEnheterDuplikatAvSammeUniqueAgressoId.size()));
        log.info("Resultat so far:\n{}", result);

        return result.toString();
    }

    private String aktiveRessurser() {
        StringBuffer result = new StringBuffer();
        List<RessursState> alleRessurser = ressursStateRepository.getAll().stream().toList();

        List<IOrgenhet> alleEnheter = (List<IOrgenhet>) orgenhetService.getAllEnheter();

        List<IOrgenhet> enheterIkkeAktiveEllerUtenOrganisering;

        if (unleash.isEnabled(UNLEASH_USE_NEW_ORGANISASJON_SOURCE)) {
            log.info("Beregner aktive ressurser med ny implementasjon");

            List<OrganiseringState> aktiveOrganiseringer = organiseringStateRepository.getOrganiseringer();

            List<String> aktiveOverEnheterUniqueIder = uniqueIdForOverEnhet(aktiveOrganiseringer);
            List<String> aktiveUnderEnheterUniqueIder = uniqueIdForUnderEnhet(aktiveOrganiseringer);

            List<String> aktiveOrganiseringerUniqueIder = new ArrayList<>(aktiveOverEnheterUniqueIder);
            aktiveOrganiseringerUniqueIder.addAll(aktiveUnderEnheterUniqueIder);

            enheterIkkeAktiveEllerUtenOrganisering = enheterIkkeBlantEnhetIder(alleEnheter, aktiveOrganiseringerUniqueIder);

        } else {
            log.info("Beregner aktive ressurser med gammel implementasjon");
            List<DvhOrganisering> alleOrganiseringer = dvhRepository.getOrganiseringer();

            List<DvhOrganisering> aktiveOrganiseringer = aktiveOrganiseringer(alleOrganiseringer);
            List<String> aktiveOverEnheterUniqueAgressoIder = uniqueAgressoIdForOverEnhet(aktiveOrganiseringer);
            List<String> aktiveUnderEnheterUniqueAgressoIder = uniqueAgressoIdForUnderEnhet(aktiveOrganiseringer);

            List<String> aktiveOrganiseringerUniqueAgressoIder = new ArrayList<>(aktiveOverEnheterUniqueAgressoIder);
            aktiveOrganiseringerUniqueAgressoIder.addAll(aktiveUnderEnheterUniqueAgressoIder);

            enheterIkkeAktiveEllerUtenOrganisering = enheterIkkeBlandtAgressoIder(alleEnheter, aktiveOrganiseringerUniqueAgressoIder);
        }


        List<String> uniqueAgressoIderEnheterIkkeAktiveEllerUtenOrganisering = enheterIkkeAktiveEllerUtenOrganisering.stream()
            .map(IOrgenhet::getUniqueAgressoId)
            .toList();

        var alleOrgtilknytningState = orgtilknytningService.getAll();
        var aktiveOrgtilknytningState = aktiveOrgtilknytninger(alleOrgtilknytningState);
        var aktiveOrgtilknytningerStateUtoverEnhetPeriode = orgtilknytningerUtoverEnhetPeriode(aktiveOrgtilknytningState);

        List<String> navIdenterMedAktiveTilknytninger =
            aktiveOrgtilknytningState.stream().map(OrgtilknytningState::navident).toList();

        List<RessursState> ressurserMedAktiveTilknytninger = ressurserForNavIdenter(alleRessurser, navIdenterMedAktiveTilknytninger);
        List<RessursState> ressurserUtenAktiveTilknytninger = ressurserIkkeBlandtNavIdenter(alleRessurser, navIdenterMedAktiveTilknytninger);

        List<String> enhetIderEnheterIkkeAktiveEllerUtenOrganisering = enheterIkkeAktiveEllerUtenOrganisering.stream().map(IOrgenhet::getId).toList();

        List<RessursState> ressurserMedAktiveTilknytningerTilEnheterIkkeAktiveEllerUtenOrganisering =
            ressursKoblingForEnhetIder(ressurserMedAktiveTilknytninger, aktiveOrgtilknytningState, enhetIderEnheterIkkeAktiveEllerUtenOrganisering);

        int aktiveTilknytningerSize = aktiveOrgtilknytningState.size();
        int aktiveTilknytningerUtoverEnhetPeriodeSize = aktiveOrgtilknytningerStateUtoverEnhetPeriode.size();

        result.append("Totalt antall ressurser: %s\n".formatted(alleRessurser.size()));
        result.append("   Aktive orgtilknytninger: %s\n".formatted(aktiveTilknytningerSize));
        result.append("   Ressurser med aktive orgtilknytninger: %s\n".formatted(ressurserMedAktiveTilknytninger.size()));
        result.append("   Ressurser uten aktive orgtilknytninger: %s\n".formatted(ressurserUtenAktiveTilknytninger.size()));
        result.append("   Ressurser med aktive orgtilknytninger for enhet som er inaktiv eller uten organisering: %s\n".formatted(
            ressurserMedAktiveTilknytningerTilEnheterIkkeAktiveEllerUtenOrganisering.size()));
        result.append("   Ressurser med tilknytninger i periode utover enhetens gyldighet: %s\n".formatted(aktiveTilknytningerUtoverEnhetPeriodeSize));

        log.info("Total resultat:\n{}", result);
        return result.toString();
    }

    private List<IOrgenhet> enheterMedStartdato(List<IOrgenhet> enheter) {
        return enheter.stream().filter(oe -> oe.getGyldigFom() != null).toList();
    }

    private List<IOrgenhet> enheterMedSluttdato(List<IOrgenhet> enheter) {
        return enheter.stream().filter(oe -> oe.getGyldigTom().isPresent()).toList();
    }

    private List<IOrgenhet> enheterMedSluttdatoFoerStartdato(List<IOrgenhet> enheter) {
        return enheter.stream().filter(oe -> oe.getGyldigTom().get().isBefore(oe.getGyldigFom())).toList();
    }

    private List<IOrgenhet> aktiveEnheter(List<IOrgenhet> enheter) {
        // FOM er før eller lik dagens dat, samt at TOM er null eller fremtidig
        return enheter.stream().filter(oe ->
            oe.getGyldigFom().isBefore(LocalDate.now().plusDays(1)) &&
                (!oe.getGyldigTom().isPresent() || oe.getGyldigTom().get().isAfter(LocalDate.now()))
        ).toList();
    }

    private List<DvhOrganisering> getFremtidigeOrganiseringer(List<DvhOrganisering> organiseringer) {
        // FOM er fremtidig, samt at TOM er null eller etter FOM
        return organiseringer.stream().filter(o ->
            o.getGyldigFom().isAfter(LocalDate.now()) &&
                (o.getGyldigTom() == null || o.getGyldigTom().isAfter(o.getGyldigFom()))
        ).toList();
    }

    private List<DvhOrganisering> aktiveOrganiseringer(List<DvhOrganisering> organiseringer) {
        // FOM er før eller lik dagens dato, samt at TOM er null eller fremtidig
        return organiseringer.stream().filter(o ->
            o.getGyldigFom().isBefore(LocalDate.now().plusDays(1)) &&
                (o.getGyldigTom() == null || o.getGyldigTom().isAfter(LocalDate.now()))
        ).toList();
    }

    private List<OrgtilknytningState> orgtilknytningerUtoverEnhetPeriode(List<OrgtilknytningState> orgtilknytninger) {
        return orgtilknytninger.stream()
            .filter(t -> {
                var orgEnhet = orgenhetService.getById(t.orgEnhetId());
                if (orgEnhet.get().getGyldigTom().isPresent() && orgEnhet.get().getGyldigTom().get().isBefore(t.gyldigFom())) {
                    return true; // orgEnhet.gyldigTom < tilknytning.gyldigFom
                }
                if (t.gyldigTom().isPresent() && orgEnhet.get().getGyldigFom().isAfter(t.gyldigTom().get())) {
                    return true; // orgEnhet.gyldigFom > tilknytning.gyldigTom
                }
                if (t.gyldigTom().isEmpty() && orgEnhet.get().getGyldigTom().isPresent()) {
                    return true; // orgEnhet avsluttet > tilknytning ikke avsluttet
                }
                return false;
            }).toList();
    }

    public List<String> uniqueAgressoIdForOverEnhet(List<DvhOrganisering> organiseringer) {
        return organiseringer.stream().map(o -> o.uniqueAgressoIdOver()).distinct().toList();
    }

    public List<String> uniqueIdForOverEnhet(List<OrganiseringState> organiseringer) {
        return organiseringer.stream().map(OrganiseringState::idOver).distinct().toList();
    }

    public List<String> uniqueAgressoIdForUnderEnhet(List<DvhOrganisering> organiseringer) {
        return organiseringer.stream().map(o -> o.uniqueAgressoIdUnder()).distinct().toList();
    }

    public List<String> uniqueIdForUnderEnhet(List<OrganiseringState> organiseringer) {
        return organiseringer.stream().map(OrganiseringState::id).distinct().toList();
    }

    private static List<IOrgenhet> enheterForEnhetIder(List<IOrgenhet> enheter, List<String> enehtIder) {
        return enheter.stream().filter(e -> enehtIder.contains(e.getId())).toList();
    }

    private static List<IOrgenhet> enheterIkkeBlantEnhetIder(List<IOrgenhet> enheter, List<String> enehtIder) {
        return enheter.stream().filter(e -> !enehtIder.contains(e.getId())).toList();
    }

    private List<IOrgenhet> enheterForAgressoIder(List<IOrgenhet> enheter, List<String> uniqueAgessoIder) {
        return enheter.stream().filter(oe -> uniqueAgessoIder.contains(oe.getUniqueAgressoId())).toList();
    }

    private List<IOrgenhet> enheterBlantEnhetIder(List<IOrgenhet> enheter, List<String> enhetIder) {
        return enheter.stream().filter(orgenhet -> enhetIder.contains(orgenhet.getId())).toList();
    }

    private List<IOrgenhet> enheterIkkeBlandtAgressoIder(List<IOrgenhet> enheter, List<String> uniqueAgessoIder) {
        // Enheter som ikke er enhet i angitte organiseringer
        return enheter.stream().filter(oe -> !uniqueAgessoIder.contains(oe.getUniqueAgressoId())).toList();
    }

    private Map<String, List<IOrgenhet>> duplikatPaaSammeUniqueAgresssoId(List<IOrgenhet> aktiveEnheter) {
        var duplicateListMap = new HashMap<String, List<IOrgenhet>>();
        aktiveEnheter.forEach(e -> {
                IOrgenhet enhetPaaUniqueAgressoId = orgenhetService.getEnhetByAgressoIdNivaAndName(e.getAgressoId(), e.getOrgNiv(), e.getNavn()).get();
                if (!enhetPaaUniqueAgressoId.getId().equals(e.getId())) {
                    List<IOrgenhet> duplicates = duplicateListMap.get(enhetPaaUniqueAgressoId.getId());
                    if (duplicates == null) {
                        duplicates = new ArrayList<>();
                        duplicateListMap.put(enhetPaaUniqueAgressoId.getId(), duplicates);
                    }
                    duplicates.add(e);
                }
            }
        );
        return duplicateListMap;
    }

    private List<OrgtilknytningState> aktiveOrgtilknytninger(Collection<OrgtilknytningState> orgtilknytninger) {
        return orgtilknytninger.stream().filter(ot ->
            ot.gyldigFom().isBefore(LocalDate.now().plusDays(1)) &&
                (!ot.gyldigTom().isPresent() || ot.gyldigTom().get().isAfter(LocalDate.now()))
        ).toList();
    }

    private List<RessursState> ressurserForKoblinger(List<RessursState> ressurser, List<Orgtilknytning> orgtilknytninger) {
        return ressurser.stream().filter(r -> orgtilknytninger.stream().anyMatch(ot -> r.navident().equals(ot.getNavIdent()))).toList();
    }

    private List<RessursState> ressursKoblingDagensDatoForEnhetIder(
        List<RessursState> ressurser,
        List<OrgtilknytningState> orgtilknytninger,
        List<String> enhetIder
    ) {
        List<OrgtilknytningState> orgtilknytningerForAngitteEnheter =
            orgtilknytninger.stream()
                .filter(ot -> enhetIder.contains(ot.orgEnhetId()))
                .filter(PeriodeUtil::isCurrentPeriod)
                .toList();
        return ressurser.stream().filter(r -> orgtilknytningerForAngitteEnheter.stream().anyMatch(ot -> ot.navident().equals(r.navident()))).toList();
    }

    private List<RessursState> ressursKoblingForEnhetIder(List<RessursState> ressurser, List<OrgtilknytningState> orgtilknytninger, List<String> enhetIder) {
        List<OrgtilknytningState> orgtilknytningerForAngitteEnheter =
            orgtilknytninger.stream().filter(ot -> enhetIder.contains(ot.orgEnhetId())).toList();
        return ressurser.stream().filter(r -> orgtilknytningerForAngitteEnheter.stream().anyMatch(ot -> ot.navident().equals(r.navident()))).toList();
    }

    private List<RessursState> ressurserUtenKoblinger(List<RessursState> ressurser, List<Orgtilknytning> orgtilknytninger) {
        return ressurser.stream().filter(r -> orgtilknytninger.stream().noneMatch(ot -> ot.getNavIdent().equals(r.navident()))).toList();
    }

    private List<RessursState> ressurserForNavIdenter(List<RessursState> ressurser, List<String> navIdenter) {
        return ressurser.stream().filter(r -> navIdenter.contains(r.navident())).toList();
    }

    private List<RessursState> ressurserIkkeBlandtNavIdenter(List<RessursState> ressurser, List<String> navIdenter) {
        return ressurser.stream().filter(r -> !navIdenter.contains(r.navident())).toList();
    }
}


