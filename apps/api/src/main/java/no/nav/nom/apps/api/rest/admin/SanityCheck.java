package no.nav.nom.apps.api.rest.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.dvh.dto.DvhOrganisering;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.apps.api.orgtilknytning.contract.Orgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil;
import no.nav.nom.apps.api.ressurs.eventsourcing.state.RessursStateRepository;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Tag(name = "Admin for å kjøre en sanity check på data")
@RestController
@RequestMapping("/api/admin/SanityCheck")
public class SanityCheck {
    private final Logger log = LoggerFactory.getLogger(SanityCheck.class);
    private final DvhRepository dvhRepository;
    private final RessursStateRepository ressursStateRepository;
    private final OrgenhetService orgenhetService;
    private final OrgtilknytningService orgtilknytningService;

    public SanityCheck(DvhRepository dvhRepository,
                       RessursStateRepository ressursStateRepository, OrgenhetService orgenhetService, OrgtilknytningService orgtilknytningService) {
        this.dvhRepository = dvhRepository;
        this.ressursStateRepository = ressursStateRepository;
        this.orgenhetService = orgenhetService;
        this.orgtilknytningService = orgtilknytningService;
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
        List<DvhOrganisering> alleOrganiseringer = dvhRepository.getOrganiseringer();

        List<IOrgenhet> aktiveEnheter = aktiveEnheter(alleEnheter);
        List<DvhOrganisering> aktiveOrganiseringer = aktiveOrganiseringer(alleOrganiseringer);
        List<String> aktiveUnderEnheterUniqueAgressoIder = uniqueAgressoIdForUnderEnhet(aktiveOrganiseringer);
        List<IOrgenhet> aktiveEnheterUtenAktivOverEnhet = enheterIkkeBlandtAgressoIder(aktiveEnheter, aktiveUnderEnheterUniqueAgressoIder);

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
        List<DvhOrganisering> alleOrganiseringer = dvhRepository.getOrganiseringer();

        List<IOrgenhet> aktiveEnheter = aktiveEnheter(alleEnheter);
        List<DvhOrganisering> aktiveOrganiseringer = aktiveOrganiseringer(alleOrganiseringer);
        List<String> aktiveUnderEnheterUniqueAgressoIder = uniqueAgressoIdForUnderEnhet(aktiveOrganiseringer);
        List<IOrgenhet> aktiveEnheterUtenAktivOverEnhet = enheterIkkeBlandtAgressoIder(aktiveEnheter, aktiveUnderEnheterUniqueAgressoIder);

        List<IOrgenhet> aktiveEnheterMedAktivUnderEnhetUtenAktivOverEnhet = enheterForAgressoIder(aktiveEnheterUtenAktivOverEnhet, aktiveUnderEnheterUniqueAgressoIder);

        aktiveEnheterMedAktivUnderEnhetUtenAktivOverEnhet.stream().forEach( oe ->
            result.append(oe.getId() +";" + oe.getUniqueAgressoId() + ";" + oe.getGyldigFom() + ";" + (oe.getGyldigTom().isPresent() ? oe.getGyldigTom().get() : "") + "\n")
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
    public String listRessurserTilIkkeAktivEnhet() {
        StringBuffer result = new StringBuffer();

        List<RessursState> alleRessurser = ressursStateRepository.getAll().stream().toList();
        List<IOrgenhet> alleEnheter = (List<IOrgenhet>) orgenhetService.getAllEnheter();
        List<DvhOrganisering> alleOrganiseringer = dvhRepository.getOrganiseringer();

        List<DvhOrganisering> aktiveOrganiseringer = aktiveOrganiseringer(alleOrganiseringer);
        List<String> aktiveOverEnheterUniqueAgressoIder = uniqueAgressoIdForOverEnhet(aktiveOrganiseringer);
        List<String> aktiveUnderEnheterUniqueAgressoIder = uniqueAgressoIdForUnderEnhet(aktiveOrganiseringer);

        List<String> aktiveOrganiseringerUniqueAgressoIder = new ArrayList<>(aktiveOverEnheterUniqueAgressoIder);
        aktiveOrganiseringerUniqueAgressoIder.addAll(aktiveUnderEnheterUniqueAgressoIder);

        List<IOrgenhet> enheterIkkeAktiveEllerUtenOrganisering = enheterIkkeBlandtAgressoIder(alleEnheter, aktiveOrganiseringerUniqueAgressoIder);
        List<String> uniqueAgressoIderEnheterIkkeAktiveEllerUtenOrganisering = enheterIkkeAktiveEllerUtenOrganisering.stream().map(IOrgenhet::getUniqueAgressoId).toList();

        var alleOrgtilknytningState = orgtilknytningService.getAll();
        var aktiveOrgtilknytningState = aktiveOrgtilknytninger(alleOrgtilknytningState);

        List<RessursState> ressurserMedAktiveTilknytningerTilEnheterIkkeAktiveEllerUtenOrganisering = null;
        List<String> navIdenterMedAktiveTilknytninger = aktiveOrgtilknytningState.stream().map(OrgtilknytningState::navident).toList();
        List<RessursState> ressurserMedAktiveTilknytninger = ressurserForNavIdenter(alleRessurser, navIdenterMedAktiveTilknytninger);

        List<String> enhetIderEnheterIkkeAktiveEllerUtenOrganisering = enheterIkkeAktiveEllerUtenOrganisering.stream().map(IOrgenhet::getId).toList();
        ressurserMedAktiveTilknytningerTilEnheterIkkeAktiveEllerUtenOrganisering = ressursKoblingDagensDatoForEnhetIder(ressurserMedAktiveTilknytninger, aktiveOrgtilknytningState, enhetIderEnheterIkkeAktiveEllerUtenOrganisering);

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

                if (enheter.length() == 0) {
                    result.append(" What? - ingen orgenhet i tilknytninger?\n");
                } else {
                    result.append(enheter.substring(0, enheter.length() - 2)).append("\n");
                }
            }
        );

        result.append("\n");
        uniqueAgressoIderEnheterIkkeAktiveEllerUtenOrganisering.forEach(s -> result.append(s).append("\n"));

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
        aktiveEnheterDuplikatAvSammeUniqueAgressoId.forEach( (faktiskId, duplikater) -> {
            IOrgenhet faktisk = orgenhetService.getById(faktiskId).get();
            String uniqueAgressoId = faktisk.getAgressoId() + "-" + faktisk.getNavn() + "-" + faktisk.getOrgNiv();
            String periode = faktisk.getGyldigFom().toString() + " - " + (faktisk.getGyldigTom().isPresent() ? faktisk.getGyldigTom().get() :  " ");
            result.append("   Enhet %s for UniqueAgressoId %s og periode [%s] har duplikater:\n".formatted(faktiskId, uniqueAgressoId, periode));
            duplikater.forEach( duplikat -> {
                String duplikatUniqueAgressoId = duplikat.getAgressoId() + "-" + duplikat.getNavn() + "-" + duplikat.getOrgNiv();
                String duplikatPeriode = duplikat.getGyldigFom().toString() + " - " + (duplikat.getGyldigTom().isPresent() ? duplikat.getGyldigTom().get() :  " ");
                result.append("      %s (%s) periode [%s]\n".formatted(duplikat.getId(), duplikatUniqueAgressoId, duplikatPeriode));
            });
        });

        return result.toString();
    }

    @Operation(
        description = "Listduplikate ressurser på samme navident",
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
        List<DvhOrganisering> alleOrganiseringer = dvhRepository.getOrganiseringer();

        List<IOrgenhet> aktiveEnheter = aktiveEnheter(alleEnheter);
        List<DvhOrganisering> aktiveOrganiseringer = aktiveOrganiseringer(alleOrganiseringer);

        List<String> aktiveOverEnheterUniqueAgressoIder = uniqueAgressoIdForOverEnhet(aktiveOrganiseringer);
        List<String> aktiveUnderEnheterUniqueAgressoIder = uniqueAgressoIdForUnderEnhet(aktiveOrganiseringer);

        List<String> aktiveOrganiseringerUniqueAgressoIder = new ArrayList<>(aktiveOverEnheterUniqueAgressoIder);
        aktiveOrganiseringerUniqueAgressoIder.addAll(aktiveUnderEnheterUniqueAgressoIder);

        List<IOrgenhet> aktiveEnheterUtenAktivOverEnhet = enheterIkkeBlandtAgressoIder(aktiveEnheter, aktiveUnderEnheterUniqueAgressoIder);
        List<IOrgenhet> aktiveEnheterUtenAktivUnderEnhet = enheterIkkeBlandtAgressoIder(aktiveEnheter, aktiveOverEnheterUniqueAgressoIder);

        List<IOrgenhet> aktiveEnheterMedAktivUnderEnhetUtenAktivOverEnhet = enheterForAgressoIder(aktiveEnheterUtenAktivOverEnhet, aktiveUnderEnheterUniqueAgressoIder);
        List<IOrgenhet> aktiveEnheterUtenOrganisering = enheterIkkeBlandtAgressoIder(aktiveEnheter, aktiveOrganiseringerUniqueAgressoIder);

        Map<String, List<IOrgenhet>> aktiveEnheterDuplikatAvSammeUniqueAgressoId = duplikatPaaSammeUniqueAgresssoId(aktiveEnheter);

        result.append("Aktive enheter: %s\n".formatted(aktiveEnheter.size()));

        result.append("     Aktive organiseringer: %s\n".formatted(aktiveOrganiseringer.size()));
        result.append("        Antall over-enheter: %s\n".formatted(aktiveOverEnheterUniqueAgressoIder.size()));
        result.append("        Antall under-enheter: %s\n".formatted(aktiveUnderEnheterUniqueAgressoIder.size()));

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
        List<DvhOrganisering> alleOrganiseringer = dvhRepository.getOrganiseringer();

        List<DvhOrganisering> aktiveOrganiseringer = aktiveOrganiseringer(alleOrganiseringer);
        List<String> aktiveOverEnheterUniqueAgressoIder = uniqueAgressoIdForOverEnhet(aktiveOrganiseringer);
        List<String> aktiveUnderEnheterUniqueAgressoIder = uniqueAgressoIdForUnderEnhet(aktiveOrganiseringer);

        List<String> aktiveOrganiseringerUniqueAgressoIder = new ArrayList<>(aktiveOverEnheterUniqueAgressoIder);
        aktiveOrganiseringerUniqueAgressoIder.addAll(aktiveUnderEnheterUniqueAgressoIder);

        List<IOrgenhet> enheterIkkeAktiveEllerUtenOrganisering = enheterIkkeBlandtAgressoIder(alleEnheter, aktiveOrganiseringerUniqueAgressoIder);

        List<String> uniqueAgressoIderEnheterIkkeAktiveEllerUtenOrganisering = enheterIkkeAktiveEllerUtenOrganisering.stream().map(IOrgenhet::getUniqueAgressoId).toList();

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
        result.append("   Ressurser med aktive orgtilknytninger for enhet om er inaktiv eller uten organisering: %s\n".formatted(ressurserMedAktiveTilknytningerTilEnheterIkkeAktiveEllerUtenOrganisering.size()));
        result.append("   Ressurser med tilknytninger i periode utover enhetens gyldighet: %s\n".formatted(aktiveTilknytningerUtoverEnhetPeriodeSize));

        log.info("Total resultat:\n{}", result);
        return result.toString();
    }

    private List<IOrgenhet> enheterMedStartdato(List<IOrgenhet> enheter) {
        return enheter.stream().filter( oe -> oe.getGyldigFom() != null).toList();
    }

    private List<IOrgenhet> enheterMedSluttdato(List<IOrgenhet> enheter) {
        return enheter.stream().filter( oe -> oe.getGyldigTom().isPresent()).toList();
    }

    private List<IOrgenhet> enheterMedSluttdatoFoerStartdato(List<IOrgenhet> enheter) {
        return enheter.stream().filter( oe -> oe.getGyldigTom().get().isBefore(oe.getGyldigFom())).toList();
    }

    private List<IOrgenhet> aktiveEnheter(List<IOrgenhet> enheter) {
        // FOM er før eller lik dagens dat, samt at TOM er null eller fremtidig
        return enheter.stream().filter( oe ->
                oe.getGyldigFom().isBefore(LocalDate.now().plusDays(1)) &&
                (!oe.getGyldigTom().isPresent() || oe.getGyldigTom().get().isAfter(LocalDate.now()))
        ).toList();
    }

    private List<DvhOrganisering> getFremtidigeOrganiseringer(List<DvhOrganisering> organiseringer) {
        // FOM er fremtidig, samt at TOM er null eller etter FOM
        return organiseringer.stream().filter( o ->
                o.getGyldigFom().isAfter(LocalDate.now()) &&
                (o.getGyldigTom() == null || o.getGyldigTom().isAfter(o.getGyldigFom()))
        ).toList();
    }

    private List<DvhOrganisering> aktiveOrganiseringer(List<DvhOrganisering> organiseringer) {
        // FOM er før eller lik dagens dato, samt at TOM er null eller fremtidig
        return organiseringer.stream().filter( o ->
                o.getGyldigFom().isBefore(LocalDate.now().plusDays(1)) &&
                (o.getGyldigTom() == null || o.getGyldigTom().isAfter(LocalDate.now()))
        ).toList();
    }

    private List<OrgtilknytningState> orgtilknytningerUtoverEnhetPeriode(List<OrgtilknytningState> orgtilknytninger) {
        return orgtilknytninger.stream()
            .filter( t -> {
                var orgEnhet = orgenhetService.getById(t.orgEnhetId());
                if (orgEnhet.get().getGyldigTom().isPresent() && orgEnhet.get().getGyldigTom().get().isBefore(t.gyldigFom())) {
                    return true; // orgEnhet.gyldigTom < tilknytning.gyldigFom
                }
                if (t.gyldigTom().isPresent() && orgEnhet.get().getGyldigFom().isAfter(t.gyldigTom().get())) {
                    return true; // orgEnhet.gyldigFom > tilknytning.gyldigTom
                }
                return false;
            } ).toList();
    }

    public List<String> uniqueAgressoIdForOverEnhet(List<DvhOrganisering> organiseringer) {
        return organiseringer.stream().map(o -> o.uniqueAgressoIdOver()).distinct().toList();
    }

    public List<String> uniqueAgressoIdForUnderEnhet(List<DvhOrganisering> organiseringer) {
        return organiseringer.stream().map(o -> o.uniqueAgressoIdUnder()).distinct().toList();
    }

    private static List<IOrgenhet> enheterForEnhetIder(List<IOrgenhet> enheter, List<String> enehtIder) {
        return enheter.stream().filter(e -> enehtIder.contains(e.getId())).toList();
    }

    private static List<IOrgenhet> enheterIkkeBlandtEnhetIder(List<IOrgenhet> enheter, List<String> enehtIder) {
        return enheter.stream().filter(e -> !enehtIder.contains(e.getId())).toList();
    }

    private List<IOrgenhet> enheterForAgressoIder(List<IOrgenhet> enheter, List<String> uniqueAgessoIder) {
        return enheter.stream().filter( oe -> uniqueAgessoIder.contains(oe.getUniqueAgressoId())).toList();
    }

    private List<IOrgenhet> enheterIkkeBlandtAgressoIder(List<IOrgenhet> enheter, List<String> uniqueAgessoIder) {
        // Enheter som ikke er enhet i angitte organiseringer
        return enheter.stream().filter( oe -> !uniqueAgessoIder.contains(oe.getUniqueAgressoId())).toList();
    }


    private Map<String, List<IOrgenhet>> duplikatPaaSammeUniqueAgresssoId(List<IOrgenhet> aktiveEnheter) {
        var duplicateListMap = new HashMap<String, List<IOrgenhet>>();
        aktiveEnheter.forEach( e -> {
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
        return orgtilknytninger.stream().filter( ot ->
            ot.gyldigFom().isBefore(LocalDate.now().plusDays(1)) &&
                (!ot.gyldigTom().isPresent() || ot.gyldigTom().get().isAfter(LocalDate.now()))
        ).toList();
    }

    private List<RessursState> ressurserForKoblinger(List<RessursState> ressurser, List<Orgtilknytning> orgtilknytninger) {
        return ressurser.stream().filter( r -> orgtilknytninger.stream().anyMatch(ot -> r.navident().equals(ot.getNavIdent()))).toList();
    }

    private List<RessursState> ressursKoblingDagensDatoForEnhetIder(List<RessursState> ressurser, List<OrgtilknytningState> orgtilknytninger, List<String> enhetIder) {
        List<OrgtilknytningState> orgtilknytningerForAngitteEnheter =
            orgtilknytninger.stream()
                .filter( ot -> enhetIder.contains(ot.orgEnhetId()))
                .filter(PeriodeUtil::isCurrentPeriod)
                .toList();
        return ressurser.stream().filter( r -> orgtilknytningerForAngitteEnheter.stream().anyMatch( ot -> ot.navident().equals(r.navident()))).toList();
    }

    private List<RessursState> ressursKoblingForEnhetIder(List<RessursState> ressurser, List<OrgtilknytningState> orgtilknytninger, List<String> enhetIder) {
        List<OrgtilknytningState> orgtilknytningerForAngitteEnheter =
            orgtilknytninger.stream().filter( ot -> enhetIder.contains(ot.orgEnhetId())).toList();
        return ressurser.stream().filter( r -> orgtilknytningerForAngitteEnheter.stream().anyMatch( ot -> ot.navident().equals(r.navident()))).toList();
    }

    private List<RessursState> ressurserUtenKoblinger(List<RessursState> ressurser, List<Orgtilknytning> orgtilknytninger) {
        return ressurser.stream().filter( r -> orgtilknytninger.stream().noneMatch(ot -> ot.getNavIdent().equals(r.navident()))).toList();
    }

    private List<RessursState> ressurserForNavIdenter(List<RessursState> ressurser, List<String> navIdenter) {
        return ressurser.stream().filter( r -> navIdenter.contains(r.navident())).toList();
    }

    private List<RessursState> ressurserIkkeBlandtNavIdenter(List<RessursState> ressurser, List<String> navIdenter) {
        return ressurser.stream().filter( r -> !navIdenter.contains(r.navident())).toList();
    }
}


