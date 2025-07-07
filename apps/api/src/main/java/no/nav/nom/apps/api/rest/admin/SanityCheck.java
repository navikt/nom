package no.nav.nom.apps.api.rest.admin;

import io.getunleash.Unleash;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.dvh.dto.DvhLeder;
import no.nav.nom.apps.api.dvh.dto.DvhOrganisering;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.apps.api.orgtilknytning.contract.Orgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil;
import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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

    public SanityCheck(
        DvhRepository dvhRepository,
        RessursStateRepository ressursStateRepository,
        OrgenhetService orgenhetService, OrgtilknytningService orgtilknytningService,
        OrganiseringStateRepository organiseringStateRepository
    ) {
        this.dvhRepository = dvhRepository;
        this.ressursStateRepository = ressursStateRepository;
        this.orgenhetService = orgenhetService;
        this.orgtilknytningService = orgtilknytningService;
        this.organiseringStateRepository = organiseringStateRepository;
    }
    //TODO: (Test Testesen 4) Denne er ferdig implementert i nom-admin, kan fjernes.
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

    //TODO: (Test Testesen 4) Denne er ferdig implementert i nom-admin, kan fjernes.
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

    //TODO: (Test Testesen 4) Denne er ferdig implementert i nom-admin, kan fjernes.
    @Operation(
        description = "Sjekk aktive ressurser",
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

    //TODO: (Test Testesen 4) Denne er ferdig implementert i nom-admin, kan fjernes.
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

        log.info("Lister enheter uten overenheter med ny implementasjon");

        List<OrganiseringState> aktiveOrganiseringer = organiseringStateRepository.getOrganiseringer();

        List<String> aktiveUnderEnheterUniqueIder = uniqueIdForUnderEnhet(aktiveOrganiseringer);
        aktiveEnheterUtenAktivOverEnhet = enheterIkkeBlantEnhetIder(aktiveEnheter, aktiveUnderEnheterUniqueIder);

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

    //TODO: (Test Testesen 4) Denne er ferdig implementert i nom-admin, kan fjernes.
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

        log.info("Lister enheter uten overenheter med ny implementasjon");

        List<OrganiseringState> aktiveOrganiseringer = organiseringStateRepository.getOrganiseringer();

        List<String> aktiveUnderEnheterUniqueIder = uniqueIdForUnderEnhet(aktiveOrganiseringer);
        List<IOrgenhet> aktiveEnheterUtenAktivOverEnhet = enheterIkkeBlantEnhetIder(aktiveEnheter, aktiveUnderEnheterUniqueIder);

        aktiveEnheterMedAktivUnderEnhetUtenAktivOverEnhet = enheterBlantEnhetIder(aktiveEnheterUtenAktivOverEnhet, aktiveUnderEnheterUniqueIder);

        aktiveEnheterMedAktivUnderEnhetUtenAktivOverEnhet.stream().forEach(oe ->
            result.append(oe.getId() + ";" + oe.getUniqueAgressoId() + ";" + oe.getGyldigFom() + ";" + (oe.getGyldigTom().isPresent() ? oe.getGyldigTom()
                .get() : "") + "\n")
        );

        return result.toString();
    }

    //TODO: (Test Testesen 4) Denne er ferdig implementert i nom-admin, kan fjernes.
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

        log.info("Finner ressurser knyttet til ikke-aktiv enhet med ny implementasjon");

        List<OrganiseringState> aktiveOrganiseringer = organiseringStateRepository.getOrganiseringer();

        List<String> aktiveOverEnheterUniqueIder = uniqueIdForOverEnhet(aktiveOrganiseringer);
        List<String> aktiveUnderEnheterUniqueIder = uniqueIdForUnderEnhet(aktiveOrganiseringer);

        List<String> aktiveOrganiseringerUniqueIder = new ArrayList<>(aktiveOverEnheterUniqueIder);
        aktiveOrganiseringerUniqueIder.addAll(aktiveUnderEnheterUniqueIder);

        enheterIkkeAktiveEllerUtenOrganisering = enheterIkkeBlantEnhetIder(alleEnheter, aktiveOrganiseringerUniqueIder);

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

        return result.toString();
    }

    //TODO: (Test Testesen 4) Denne er ferdig implementert i nom-admin, kan fjernes.
    @Operation(
        description = "List tilknytninger med gyldighet utover ressurens gyldighetsperiode",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/listTilkoblingerUtoverRessursGyldighetPeriode")
    public String listTilkoblingerUtoverRessursGyldighetPeriode() {
        StringBuffer result = new StringBuffer();

        var alleOrgtilknytningState = orgtilknytningService.getAll().stream().toList();
        log.info("alleOrgtilknytningState, size: {}", alleOrgtilknytningState.size());

        var tilknytningerUtoverRessursPeriode = orgtilknytningerUtoverRessursPeriode(alleOrgtilknytningState);
        log.info("tilknytningerUtoverRessursPeriode, size: {}", tilknytningerUtoverRessursPeriode.size());

        tilknytningerUtoverRessursPeriode.forEach(t -> {
            var oe = orgenhetService.getById(t.orgEnhetId()).get();
            var ressurs = ressursStateRepository.findByNavident(t.navident()).get();
            result.append(t.navident())
                .append(";")
                .append(ressurs.visningsnavn())
                .append(";")
                .append(ressurs.startdato())
                .append(";")
                .append(ressurs.sluttdato())
                .append(";")
                .append(oe.getUniqueAgressoId())
                .append(";")
                .append(t.orgEnhetId())
                .append(";")
                .append(t.gyldigFom())
                .append(";")
                .append(t.gyldigTom().isPresent() ? t.gyldigTom().get() : "")
                .append("\n");
        });
        return result.toString();
    }

    private List<OrgtilknytningState> orgtilknytningerUtoverRessursPeriode(List<OrgtilknytningState> orgtilknytninger) {
        return orgtilknytninger.stream()
            .filter(t -> {
                var ressurs = ressursStateRepository.findByNavident(t.navident());

                if (ressurs.get().sluttdato().isPresent() && ressurs.get().sluttdato().get().isBefore(t.gyldigFom())) {
                    return true; // ressurs.sluttDato < tilknytning.gyldigFom
                }
                if (t.gyldigTom().isPresent() && ressurs.get().startdato().isPresent() && ressurs.get().startdato().get().isAfter(t.gyldigTom().get())) {
                    return true; // ressurs.startDato > tilknytning.gyldigTom
                }
                if (t.gyldigTom().isEmpty() && ressurs.get().sluttdato().isPresent()) {
                    return true; // ressurs avsluttet > tilknytning ikke avsluttet
                }
                return false;
            }).toList();
    }

    //TODO: (Test Testesen 4) Denne er ferdig implementert i nom-admin, kan fjernes.
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

    //TODO: (Test Testesen 4) Denne er ferdig implementert i nom-admin, kan fjernes.
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


    //TODO: (Test Testesen 4) Denne er ferdig implementert i nom-admin, kan fjernes.
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

    //TODO: (Test Testesen 4) Denne er ferdig implementert i nom-admin, kan fjernes.
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

    //TODO: (Test Testesen 4) Denne er ferdig implementert i nom-admin, kan fjernes.
    @Operation(
        description = "List orgtilknytninger med flere dagligOppfolging i current periode (gyldigFom < now < gyldigTom)",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/orgtilknytningMedFlereAktivDagligOppfolging")
    public List<String> orgtilknytningMedFlereAktivDagligOppfolging() {
        // Group by navident for current period and dagligOppfolging
        Map<String, List<OrgtilknytningState>> groupedByNavidentForCurrentPeriodWithDagligDagligOppfolging = orgtilknytningService.getAll().stream()
            .filter(ot -> ot.gyldigFom().isBefore(LocalDate.now().plusDays(1)) &&
                (ot.gyldigTom().isEmpty() || ot.gyldigTom().get().isAfter(LocalDate.now()))) // er current date
            .filter(OrgtilknytningState::erDagligOppfolging) // er dagligoppfolging
            .collect(Collectors.groupingBy(OrgtilknytningState::navident));

        // Filter out groups where there is more than one dagligOppfolging in current period
        return groupedByNavidentForCurrentPeriodWithDagligDagligOppfolging.values().stream()
            .filter(group -> group.size() > 1)
            .flatMap(List::stream)
            .map(ot -> String.format("%s %s %s %s %s %s", ot.navident(), ot.orgEnhetId(), ot.fid(), ot.gyldigFom(), ot.gyldigTom(), ot.erDagligOppfolging()))
            .toList();
    }

    // TODO Lagt over til nom-admin, kan fjernes
    @Operation(
        description = "List orgtilknytninger med flere overlappende dagligOppfolging i aktiv eller fremtidig periode (gyldigTom > now)",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/orgtilknytningMedFlereOverlappendeDagligOppfolgingCurrentOgFuture")
    public String orgtilknytningMedFlereOverlappendeDagligOppfolgingCurrentOgFuture() {
        // Group by navident for current period and dagligOppfolging
        Map<String, List<OrgtilknytningState>> groupedByNavidentForCurrentAndFuturePeriodWithDagligDagligOppfolging = orgtilknytningService.getAll().stream()
            .filter( PeriodeUtil::isCurrentOrFuturePeriod) // er current eller fremtidig
            .filter(OrgtilknytningState::erDagligOppfolging) // er dagligoppfolging
            .sorted(Comparator.comparing(OrgtilknytningState::gyldigFom))
            .collect(Collectors.groupingBy(OrgtilknytningState::navident));

        StringBuffer result = new StringBuffer("Duplikate overlappende OTer aktiv og/eller fremtidig periode:\n");

        // Filter out groups where there is more than one dagligOppfolging in current period
        groupedByNavidentForCurrentAndFuturePeriodWithDagligDagligOppfolging.values().stream()
            .filter(group -> group.size() > 1)
            .forEach(otEr -> {
                OrgtilknytningState previous = null;
                for (OrgtilknytningState ot : otEr) {
                    if (previous != null) {
                        if (PeriodeUtil.overlapsInterval(ot, previous)) {
                            result.append("%s %s %s %s %s %s\n".formatted(previous.navident(), previous.orgEnhetId(), previous.fid(), previous.gyldigFom(), previous.gyldigTom(), previous.erDagligOppfolging()));
                            result.append("%s %s %s %s %s %s\n".formatted(ot.navident(), ot.orgEnhetId(), ot.fid(), ot.gyldigFom(), ot.gyldigTom(), ot.erDagligOppfolging()));
                        }
                    }
                    previous = ot;
                }
            });
        return result.toString();
    }

    // TODO Lagt over til nom-admin, kan fjernes
    @Operation(
        description = "List duplikate orgtilknytninger på samme orgenhet og navident",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/duplikateOrgtilknytninger")
    public String duplikateOrgtilknytninger() {
        // Group by navident
        Map<String, List<OrgtilknytningState>> groupedByNavident = orgtilknytningService.getAll().stream()
            .sorted(Comparator
                .comparing(OrgtilknytningState::orgEnhetId)
                .thenComparing(OrgtilknytningState::gyldigFom)
            )
            .collect(Collectors.groupingBy(OrgtilknytningState::navident));

        StringBuffer result = new StringBuffer("Duplikate OTer (samme navident, enhet og gyldigFom):\n");

        // Filter out groups where there is more than one dagligOppfolging in current period
        groupedByNavident.values().stream()
            .filter(group -> group.size() > 1)
            .forEach(otEr -> {
                OrgtilknytningState original = null;
                for (OrgtilknytningState ot : otEr) {
                    if (original != null) {
                        if (original.orgEnhetId().equals(ot.orgEnhetId()) && original.gyldigFom().equals(ot.gyldigFom())) {
                            result.append("%s har duplikat %s %s %s %s %s %s\n".formatted(original.fid(),ot.navident(), ot.orgEnhetId(), ot.fid(), ot.gyldigFom(), ot.gyldigTom(), ot.erDagligOppfolging()));
                        } else {
                            original = ot;
                        }
                    } else {
                        original = ot;
                    }
                }
            });
        return result.toString();
    }

    // TODO Lagt over til nom-admin, kan fjernes
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

    @Operation(
        description = "List orgenher i NAV orghierarkiet som ikke har leder",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/listOrgenheterUtenLeder")
    public String listOrgenheterUtenLeder() {
        StringBuffer result = new StringBuffer();
        var alleEnheter = orgenhetService.getAllEnheterMedOrganisering().stream().toList(); // Aktive enheter i organisasjonstreet
        List<IOrgenhet> alleOrgEnheterUtenLeder = new ArrayList<>();
        for (IOrgenhet orgenhet : alleEnheter) {
            List<DvhLeder> leaders = dvhRepository.getLederByAgressoIdAndNivAndNavn(orgenhet.getAgressoId(), orgenhet.getOrgNiv(), orgenhet.getNavn())
                .stream()
                .filter(Objects::nonNull)
                .toList();
            if (leaders.isEmpty()) {
                alleOrgEnheterUtenLeder.add(orgenhet);
            }
        }

        alleOrgEnheterUtenLeder.forEach(oe ->
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
        description = "Valider ressurs som har sluttet, men står som leder på orgEnhet",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/sluttetRessursMedLederRollePaaOrgEnhet")
    public List<EnhtetMedRessursSomHarSluttetOgErLederSanityCheckResponse> validerRessursMedLederRollePaaOrgEnhet() {
        var alleEnheter = orgenhetService.getAllEnheterMedOrganisering().stream().toList(); // Aktive enheter i organisasjonstreet
        var alleRessurserWithSluttDatoBeforeToday = ressursStateRepository.findAllWithSluttdatoBeforeToday();
        Map<RessursState, List<IOrgenhet>> alleOrgEnheterMedLederSomHarSluttet = new HashMap<>();
        for (IOrgenhet orgenhet : alleEnheter) {
            validerEnhetsledereSomHarSluttet(orgenhet, alleRessurserWithSluttDatoBeforeToday).ifPresent(ressursState -> {
                if (!alleOrgEnheterMedLederSomHarSluttet.containsKey(ressursState)) {
                    alleOrgEnheterMedLederSomHarSluttet.computeIfAbsent(ressursState, org -> new ArrayList<>()).add(orgenhet);
                } else {
                    alleOrgEnheterMedLederSomHarSluttet.get(ressursState).add(orgenhet);
                }
            });
        }
        return EnhtetMedRessursSomHarSluttetOgErLederSanityCheckResponse.from(alleOrgEnheterMedLederSomHarSluttet);
    }

    // TODO Lagt over til nom-admin, kan fjernes
    @Operation(
        description = "Valider ressurser som har en ulik sluttdato på organisasjonstilknytning og ressurs sluttdato",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        @ApiResponse(responseCode = "200", description = "Success")
    )
    @GetMapping(value = "/ressursSluttdatoUlikOrgTilknytningSluttdato")
    public String ressurserMedUlikOrgTilknyttningSluttDato() {
        var alleRessurserSomErAktive = ressursStateRepository.findAllActiveRessurser();
        var alleOrgTilknytninger = orgtilknytningService.getByNavidenter(alleRessurserSomErAktive.keySet(), orgtilknytningState -> orgtilknytningState);
        Map<String, Collection<OrgtilknytningState>> sisteOrgtilknytninger = getSisteOrgtilknytninger(alleOrgTilknytninger);

        StringBuilder ulikeSluttdatoer = new StringBuilder();

        for (OrgtilknytningState orgtilknytning : sisteOrgtilknytninger.values().stream().flatMap(Collection::stream).toList()) {
            var ressurs = alleRessurserSomErAktive.get(orgtilknytning.navident());
            if (orgtilknytning.gyldigTom().isPresent() && ressurs.sluttdato().isPresent()) {
                if (!orgtilknytning.gyldigTom().get().isEqual(ressurs.sluttdato().get())) {
                    appendUlikeDatoerToString(orgtilknytning, ulikeSluttdatoer, ressurs);
                }
            } else if (orgtilknytning.gyldigTom().isPresent()) {
                appendUlikeDatoerToString(orgtilknytning, ulikeSluttdatoer, ressurs);
            } else if (ressurs.sluttdato().isPresent()) {
                appendUlikeDatoerToString(orgtilknytning, ulikeSluttdatoer, ressurs);
            }
        }

        return ulikeSluttdatoer.toString();
    }

    private Map<String, Collection<OrgtilknytningState>> getSisteOrgtilknytninger(Map<String, Collection<OrgtilknytningState>> alleOrgTilknytninger) {
        Map<String, Collection<OrgtilknytningState>> result = new HashMap<>();
        for (Map.Entry<String, Collection<OrgtilknytningState>> entry : alleOrgTilknytninger.entrySet()) {
            String key = entry.getKey();
            Collection<OrgtilknytningState> orgtilknytningState = entry.getValue();

            Optional<LocalDate> latestDates = orgtilknytningState.stream()
                .map(OrgtilknytningState::gyldigTom)
                .flatMap(Optional::stream)
                .max(LocalDate::compareTo);

            if (latestDates.isPresent()) {
                Collection<OrgtilknytningState> filteredOrgtilknytningState = orgtilknytningState.stream()
                    .filter(org -> org.gyldigTom().equals(latestDates))
                    .toList();

                if (!filteredOrgtilknytningState.isEmpty()) {
                    result.put(key, filteredOrgtilknytningState);
                }
            }
        }
        return result;
    }

    private <K, V> Map<K, List<V>> addOrPut(Map<K, List<V>> map, K key, V value) {
        if (map.containsKey(key)) {
            map.get(key).add(value);
        } else {
            map.put(key, new ArrayList<>());
            map.get(key).add(value);
        }
        return map;
    }

    // TODO Lagt over til nom-admin, kan fjernes
    @Operation(
        description = "Valider ressurser som har flere enn en DOL",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        @ApiResponse(responseCode = "200", description = "Success")
    )
    @GetMapping(value = "ressursMedFlereEnnEnDOL")
    public String ressursMedFlereEnnEnDOL() {
        var alleRessurserSomErAtkive = ressursStateRepository.findAllActiveRessurser();
        var alleOrgtilknytningerKobletTilRessurs = orgtilknytningService.getByNavidenter(alleRessurserSomErAtkive.keySet(), orgtilknytningState -> orgtilknytningState);
        var alleOrgEnheter = orgenhetService.getAllEnheter();

        Map<String, List<OrgtilknytningState>> flereDolTilknytningerList = alleOrgtilknytningerKobletTilRessurs.entrySet().stream()
            .filter(orgtilknytning -> orgtilknytning.getValue().size() > 1)
            .filter(orgtilknytning -> validerOmRessursHarFlereDol(orgtilknytning.getValue()))
            .filter(orgtilknytning -> validerDatoOverlapp(orgtilknytning.getValue().stream().filter(Objects::nonNull).toList()))
            .flatMap(orgtilknytning -> orgtilknytning.getValue().stream())
            .collect(Collectors.groupingBy(OrgtilknytningState::navident, Collectors.toList()));

        StringBuilder flereDol = new StringBuilder();
        flereDolTilknytningerList.forEach((navident, orgtilknytningStates) -> {
            flereDol.append(navident)
                .append(";");
            orgtilknytningStates.forEach(orgtilknytningState -> {
                alleOrgEnheter.stream().filter(orgenhet -> orgenhet.getId().equals(orgtilknytningState.orgEnhetId())).findFirst().ifPresent(orgenhet -> {
                    flereDol.append(orgenhet.getNavn())
                        .append("=");
                });
                flereDol.append(orgtilknytningState.orgEnhetId())
                    .append(";")
                    .append(orgtilknytningState.gyldigFom())
                    .append(";")
                    .append(orgtilknytningState.gyldigTom().isPresent() ? orgtilknytningState.gyldigTom().get() : "Ingen Sluttdato")
                    .append(";");
            });
            flereDol.append("\n");
        });
        if (flereDolTilknytningerList.isEmpty()) flereDol.append("Ingen ressurs med flere DOL");
        return flereDol.toString();
    }

    // TODO Lagt over til nom-admin, kan fjernes
    @Operation(
        description = "Valider aktive ressurser uten orgtilknytning",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        @ApiResponse(responseCode = "200", description = "Success")
    )
    @GetMapping("/ressursUtenOrgtilknytning")
    public String ressursUtenOrgtilknytning() {
        var alleRessurserSomErAtkive = ressursStateRepository.findAllActiveRessurser();
        var alleOrgtilknytningerKobletTilRessurs = orgtilknytningService.getByNavidenter(alleRessurserSomErAtkive.keySet(), orgtilknytningState -> orgtilknytningState);

        var utenOrgtilknytninger = alleOrgtilknytningerKobletTilRessurs.entrySet().stream()
            .filter(orgtilknytning -> orgtilknytning.getValue().isEmpty())
            .map(Map.Entry::getKey);

        var medOrgtilknytningerSomIkkeErAktiv = alleOrgtilknytningerKobletTilRessurs.entrySet().stream()
            .filter(orgtilknytning -> !orgtilknytning.getValue().isEmpty())
            .filter(orgtilknytning -> orgtilknytning.getValue().stream()
                .allMatch(orgtilknytningState -> orgtilknytningState.gyldigTom().isPresent() && orgtilknytningState.gyldigTom().get().isBefore(LocalDate.now())))
            .map(Map.Entry::getKey);

        var ressursUtenOrgtilknytning = Stream.concat(utenOrgtilknytninger, medOrgtilknytningerSomIkkeErAktiv).toList();

        StringBuilder ressurserUtenOrgtilknytning = new StringBuilder();
        ressursUtenOrgtilknytning.forEach(navident -> ressurserUtenOrgtilknytning.append(navident).append(";").append("\n"));
        return ressurserUtenOrgtilknytning.toString();
    }

    // TODO Lagt over til nom-admin, kan fjernes
    @Operation(
        description = "List robot ressurser",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        @ApiResponse(responseCode = "200", description = "Success")
    )
    @GetMapping("/robotRessurser")
    public List<RobotRessursSanityCheckResponse> listRobotRessurser() {
        var alleAtkiveRessurser = ressursStateRepository.findAllActiveRessurser();
        var alleAktiveRoboter = alleAtkiveRessurser.entrySet().stream()
            .filter(ressurs -> ressurs.getValue().identType() != null)
            .filter(ressurs -> ressurs.getValue().identType().name().equals(IdentType.ROBOT.name()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        var alleOrgtilknytningerKobletTilRessurs = orgtilknytningService.getByNavidenter(alleAktiveRoboter.keySet(), orgtilknytningState -> orgtilknytningState);

        var result = new ArrayList<RobotRessursSanityCheckResponse>();
        alleAktiveRoboter.forEach((navident, ressurs) -> {
            if (alleOrgtilknytningerKobletTilRessurs.containsKey(navident)) {
                result.add(new RobotRessursSanityCheckResponse(
                    navident,
                    ressurs.visningsnavn().getFormattedVisningsnavn(),
                    ressurs.sektor(),
                    ressurs.startdato().isPresent() ? ressurs.startdato() : null,
                    ressurs.sluttdato().isPresent() ? ressurs.sluttdato() : null,
                    alleOrgtilknytningerKobletTilRessurs.get(navident)
                        .stream()
                        .map(orgtilknytningState -> new RobotRessursSanityCheckResponse.RobotOrgtilknytningSanityCheckResponse(
                            orgtilknytningState.orgEnhetId(),
                            orgenhetService.getById(orgtilknytningState.orgEnhetId()).isPresent() ? orgenhetService.getById(orgtilknytningState.orgEnhetId()).get().getNavn() : "Ukjent",
                            orgtilknytningState.gyldigFom(),
                            Optional.of(orgtilknytningState.gyldigTom()).orElse(null)
                        ))
                        .toList()
                ));
            } else {
                result.add(new RobotRessursSanityCheckResponse(
                    navident,
                    ressurs.visningsnavn().getFormattedVisningsnavn(),
                    ressurs.sektor(),
                    ressurs.startdato().isPresent() ? ressurs.startdato() : null,
                    ressurs.sluttdato().isPresent() ? ressurs.sluttdato() : null,
                    Collections.emptyList()
                ));
            }
        });

        return result;
    }


    private boolean validerDatoOverlapp(List<OrgtilknytningState> orgtilknytningStates) {
        List<OrgtilknytningState> sortedByFomDato = orgtilknytningStates.stream().sorted(Comparator.comparing(OrgtilknytningState::gyldigFom)).toList();
        return IntStream.range(0, sortedByFomDato.size()).anyMatch(i ->  {
            OrgtilknytningState orgtilknytningStateEn = orgtilknytningStates.get(i);
            LocalDate sluttdatoEn = orgtilknytningStateEn.gyldigTom().orElse(LocalDate.MAX);

            return IntStream.range(i + 1, sortedByFomDato.size()).anyMatch(j ->   {
                OrgtilknytningState orgtilknytningStateTo = orgtilknytningStates.get(j);
                LocalDate startdatoEn = orgtilknytningStateEn.gyldigFom();
                LocalDate startdatoTo = orgtilknytningStateTo.gyldigFom();
                LocalDate sluttdatoTo = orgtilknytningStateTo.gyldigTom().orElse(LocalDate.MAX);

                return !startdatoEn.isAfter(sluttdatoTo) && !sluttdatoEn.isBefore(startdatoTo);
            });
        });
    }

    private boolean validerOmRessursHarFlereDol(Collection<OrgtilknytningState> orgtilknytningStates) {
        return orgtilknytningStates.stream().allMatch(OrgtilknytningState::erDagligOppfolging);
    }

    private static void appendUlikeDatoerToString(OrgtilknytningState orgtilknytning, StringBuilder ulikeSluttdatoer, RessursState ressurs) {
        ulikeSluttdatoer.append(ressurs.navident())
            .append(";")
            .append(orgtilknytning.orgEnhetId())
            .append(";")
            .append("RessursSluttdato:")
            .append(ressurs.sluttdato().isPresent() ? ressurs.sluttdato().get() : "null")
            .append(";")
            .append("OrgGyldigTom:")
            .append(orgtilknytning.gyldigTom().isPresent() ? orgtilknytning.gyldigTom().get() : "null")
            .append("\n");
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

        log.info("Beregner aktive ressurser med ny implementasjon");

        List<OrganiseringState> aktiveOrganiseringer = organiseringStateRepository.getOrganiseringer();

        List<String> aktiveOverEnheterUniqueIder = uniqueIdForOverEnhet(aktiveOrganiseringer);
        List<String> aktiveUnderEnheterUniqueIder = uniqueIdForUnderEnhet(aktiveOrganiseringer);

        List<String> aktiveOrganiseringerUniqueIder = new ArrayList<>(aktiveOverEnheterUniqueIder);
        aktiveOrganiseringerUniqueIder.addAll(aktiveUnderEnheterUniqueIder);

        enheterIkkeAktiveEllerUtenOrganisering = enheterIkkeBlantEnhetIder(alleEnheter, aktiveOrganiseringerUniqueIder);


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
                (oe.getGyldigTom().isEmpty() || oe.getGyldigTom().get().isAfter(LocalDate.now()))
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

    public List<String> uniqueIdForOverEnhet(List<OrganiseringState> organiseringer) {
        return organiseringer.stream().map(OrganiseringState::idOver).distinct().toList();
    }

    public List<String> uniqueIdForUnderEnhet(List<OrganiseringState> organiseringer) {
        return organiseringer.stream().map(OrganiseringState::id).distinct().toList();
    }

    private static List<IOrgenhet> enheterIkkeBlantEnhetIder(List<IOrgenhet> enheter, List<String> enehtIder) {
        return enheter.stream().filter(e -> !enehtIder.contains(e.getId())).toList();
    }

    private List<IOrgenhet> enheterBlantEnhetIder(List<IOrgenhet> enheter, List<String> enhetIder) {
        return enheter.stream().filter(orgenhet -> enhetIder.contains(orgenhet.getId())).toList();
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

    private Optional<RessursState> validerEnhetsledereSomHarSluttet(IOrgenhet orgenhet, Map<String, RessursState> alleRessurserWithSluttDatoBeforeToday) {
        return dvhRepository.getLederByAgressoIdAndNivAndNavn(orgenhet.getAgressoId(), orgenhet.getOrgNiv(), orgenhet.getNavn()).stream()
            .map(leder -> alleRessurserWithSluttDatoBeforeToday.get(leder.getNavIdent()))
            .filter(Objects::nonNull)
            .findFirst();
    }
}


