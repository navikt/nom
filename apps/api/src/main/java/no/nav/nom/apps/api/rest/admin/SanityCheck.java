package no.nav.nom.apps.api.rest.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.dvh.dto.DvhOrganisering;
import no.nav.nom.apps.api.orgTilknytning.DvhOrgtilknytning;
import no.nav.nom.apps.api.orgTilknytning.contract.Orgtilknytning;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.IOrgenhetService;
import no.nav.nom.apps.api.ressurs.IRessursService;
import no.nav.nom.contract.nom.state.RessursState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Tag(name = "Admin for å kjøre en sanity check på data")
@RestController
@RequestMapping("/api/v1/admin/SanityCheck")
public class SanityCheck {
    private final Logger log = LoggerFactory.getLogger(SanityCheck.class);

    private final DvhRepository dvhRepository;
    private final IRessursService ressursService;
    private final IOrgenhetService orgenhetService;

    public SanityCheck(DvhRepository dvhRepository, IRessursService ressursService, IOrgenhetService orgenhetService) {
        this.dvhRepository = dvhRepository;
        this.ressursService = ressursService;
        this.orgenhetService = orgenhetService;
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

        aktiveEnheterUtenAktivOverEnhet.stream().forEach( oe ->
            result.append(oe.getId() +";" + oe.getUniqueAgressoId() + ";" + oe.getGyldigFom() + ";" + (oe.getGyldigTom().isPresent() ? oe.getGyldigTom().get() : "") + "\n")
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

        List<RessursState> alleRessurser = ressursService.getAll().stream().toList();
        List<DvhOrgtilknytning> alleTilknytninger = dvhRepository.getKoblinger().stream().map(DvhOrgtilknytning.class::cast).toList();
        List<IOrgenhet> alleEnheter = (List<IOrgenhet>) orgenhetService.getAllEnheter();
        List<DvhOrganisering> alleOrganiseringer = dvhRepository.getOrganiseringer();

        List<DvhOrganisering> aktiveOrganiseringer = aktiveOrganiseringer(alleOrganiseringer);
        List<String> aktiveOverEnheterUniqueAgressoIder = uniqueAgressoIdForOverEnhet(aktiveOrganiseringer);
        List<String> aktiveUnderEnheterUniqueAgressoIder = uniqueAgressoIdForUnderEnhet(aktiveOrganiseringer);

        List<String> aktiveOrganiseringerUniqueAgressoIder = new ArrayList<>(aktiveOverEnheterUniqueAgressoIder);
        aktiveOrganiseringerUniqueAgressoIder.addAll(aktiveUnderEnheterUniqueAgressoIder);

        List<DvhOrgtilknytning> aktiveTilknytninger = aktiveOrgtilknytninger(alleTilknytninger);
        List<String> navIdenterMedAktiveTilknytninger = aktiveTilknytninger.stream().map(DvhOrgtilknytning::getNavIdent).toList();

        List<IOrgenhet> enheterIkkeAktiveEllerUtenOrganisering = enheterIkkeBlandtAgressoIder(alleEnheter, aktiveOrganiseringerUniqueAgressoIder);
        List<String> uniqueAgressoIderEnheterIkkeAktiveEllerUtenOrganisering = enheterIkkeAktiveEllerUtenOrganisering.stream().map(IOrgenhet::getUniqueAgressoId).toList();

        List<RessursState> ressurserMedAktiveTilknytninger = ressurserForNavIdenter(alleRessurser, navIdenterMedAktiveTilknytninger);
        List<RessursState> ressurserMedAktiveTilknytningerTilEnheterIkkeAktiveEllerUtenOrganisering = ressursKoblingForUniqueAgressoIder(ressurserMedAktiveTilknytninger, aktiveTilknytninger, uniqueAgressoIderEnheterIkkeAktiveEllerUtenOrganisering);


        ressurserMedAktiveTilknytningerTilEnheterIkkeAktiveEllerUtenOrganisering.stream().forEach( r ->
            {
                result.append(r.navident() + " [" + r.fid() + "]  - " + r.sektor() + " => ");
                StringBuffer enheter = new StringBuffer();
                aktiveTilknytninger.stream()
                    .filter( ot -> ot.getNavIdent().equals(r.navident()))
                    .forEach( ot ->
                        enheter.append(ot.getUniqueAgressoId() + ", ")
                    );
                if (enheter.length() == 0) {
                    result.append(" What? - ingen orgenhet i tilknytninger?\n");
                } else {
                    result.append(enheter.substring(0, enheter.length() - 2) + "\n");
                }
            }
        );

        result.append("\n");
        uniqueAgressoIderEnheterIkkeAktiveEllerUtenOrganisering.stream().forEach( s -> result.append(s + "\n"));

        return result.toString();
    }

    private String sjekkOrgEnhetStartSluttDato() {
        StringBuffer result = new StringBuffer();

        List<IOrgenhet> alleEnheter = (List<IOrgenhet>) orgenhetService.getAllEnheter();

        result.append("Totalt antall enheter: %s\n".formatted(alleEnheter.size()));
        result.append("   Med startdato: %s\n".formatted(enheterMedStartdato(alleEnheter).size()));
        result.append("   Med sluttdato: %s\n".formatted(enheterMedSluttdato(alleEnheter).size()));
        result.append("   Med sluttdato før startdato: %s\n\n".formatted(enheterMedSluttdatoFørStartdato(enheterMedSluttdato(alleEnheter)).size()));
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

        result.append("Aktive enheter: %s\n".formatted(aktiveEnheter.size()));

        result.append("     Aktive organiseringer: %s\n".formatted(aktiveOrganiseringer.size()));
        result.append("        Antall over-enheter: %s\n".formatted(aktiveOverEnheterUniqueAgressoIder.size()));
        result.append("        Antall under-enheter: %s\n".formatted(aktiveUnderEnheterUniqueAgressoIder.size()));

        result.append("     Aktive enheter uten OverEnheter: %s\n".formatted(aktiveEnheterUtenAktivOverEnhet.size()));
        result.append("     Aktive enheter uten UnderEnheter: %s\n".formatted(aktiveEnheterUtenAktivUnderEnhet.size()));
        result.append("     Aktive enheter med UnderEnheter uten OverEnheter: %s\n".formatted(aktiveEnheterMedAktivUnderEnhetUtenAktivOverEnhet.size()));
        result.append("     Aktive enheter uten noen organisering: %s\n".formatted(aktiveEnheterUtenOrganisering.size()));
        log.info("Resultat so far:\n{}", result);

        return result.toString();
    }

    private String aktiveRessurser() {
        StringBuffer result = new StringBuffer();
        List<RessursState> alleRessurser = ressursService.getAll().stream().toList();
        List<DvhOrgtilknytning> alleTilknytninger = dvhRepository.getKoblinger().stream().map(DvhOrgtilknytning.class::cast).toList();
        List<IOrgenhet> alleEnheter = (List<IOrgenhet>) orgenhetService.getAllEnheter();
        List<DvhOrganisering> alleOrganiseringer = dvhRepository.getOrganiseringer();

        List<DvhOrganisering> aktiveOrganiseringer = aktiveOrganiseringer(alleOrganiseringer);
        List<String> aktiveOverEnheterUniqueAgressoIder = uniqueAgressoIdForOverEnhet(aktiveOrganiseringer);
        List<String> aktiveUnderEnheterUniqueAgressoIder = uniqueAgressoIdForUnderEnhet(aktiveOrganiseringer);

        List<String> aktiveOrganiseringerUniqueAgressoIder = new ArrayList<>(aktiveOverEnheterUniqueAgressoIder);
        aktiveOrganiseringerUniqueAgressoIder.addAll(aktiveUnderEnheterUniqueAgressoIder);

        List<DvhOrgtilknytning> aktiveTilknytninger = aktiveOrgtilknytninger(alleTilknytninger);
        List<String> navIdenterMedAktiveTilknytninger = aktiveTilknytninger.stream().map(DvhOrgtilknytning::getNavIdent).toList();
//        List<String> uniqueAgresssoIderAktiveTilknytninger = aktiveTilknytninger.stream().map(DvhOrgtilknytning::getUniqueAgressoId).toList();

//        List<IOrgenhet> aktiveEnheter = aktiveEnheter(alleEnheter);
//        List<IOrgenhet> aktiveEnheterMedOrganisering = enheterForAgressoIder(aktiveEnheter, uniqueAgresssoIderAktiveTilknytninger);

//        Map<String, IOrgenhet> uniqueAgressoIderMapAktiveEnheterMedOrganisering = aktiveEnheterMedOrganisering.stream().collect(Collectors.toMap(IOrgenhet::getUniqueAgressoId, Function.identity()));
//        List<String> enhetIderAktiveEnheterMedOrganisering = aktiveEnheterMedOrganisering.stream().map(IOrgenhet::getId).toList();
        List<IOrgenhet> enheterIkkeAktiveEllerUtenOrganisering = enheterIkkeBlandtAgressoIder(alleEnheter, aktiveOrganiseringerUniqueAgressoIder);

        List<String> uniqueAgressoIderEnheterIkkeAktiveEllerUtenOrganisering = enheterIkkeAktiveEllerUtenOrganisering.stream().map(IOrgenhet::getUniqueAgressoId).toList();


        List<RessursState> ressurserMedAktiveTilknytninger = ressurserForNavIdenter(alleRessurser, navIdenterMedAktiveTilknytninger);
        List<RessursState> ressurserUtenAktiveTilknytninger = ressurserIkkeBlandtNavIdenter(alleRessurser, navIdenterMedAktiveTilknytninger);
        List<RessursState> ressurserMedAktiveTilknytningerTilEnheterIkkeAktiveEllerUtenOrganisering = ressursKoblingForUniqueAgressoIder(ressurserMedAktiveTilknytninger, aktiveTilknytninger, uniqueAgressoIderEnheterIkkeAktiveEllerUtenOrganisering);
//        List<RessursState> aktiveRessurserMedKoblingTilEnhetUtenOverEnhet = ressursKoblingForEnheter(aktiveRessurser, aktiveTilknytninger, aktiveEnheterUtenAktivOverEnhet);

        result.append("Totalt antall ressurser: %s\n".formatted(alleRessurser.size()));
        result.append("   Aktive orgtilknytninger: %s\n".formatted(aktiveTilknytninger.size()));
        result.append("   Ressurser med aktive orgtilknytninger: %s\n".formatted(ressurserMedAktiveTilknytninger.size()));
        result.append("   Ressurser uten aktive orgtilknytninger: %s\n".formatted(ressurserUtenAktiveTilknytninger.size()));
        result.append("   Ressurser med aktive orgtilknytninger for enhet om er inaktiv eller uten organisering: %s\n".formatted(ressurserMedAktiveTilknytningerTilEnheterIkkeAktiveEllerUtenOrganisering.size()));
//        result.append("   Aktive med orgtilknytning til enhet uten overenhet: %s\n".formatted(aktiveRessurserMedKoblingTilEnhetUtenOverEnhet.size()));

        log.info("Total resultat:\n{}", result);
        return result.toString();
    }

    private List<IOrgenhet> enheterMedStartdato(List<IOrgenhet> enheter) {
        return enheter.stream().filter( oe -> oe.getGyldigFom() != null).toList();
    }

    private List<IOrgenhet> enheterMedSluttdato(List<IOrgenhet> enheter) {
        return enheter.stream().filter( oe -> oe.getGyldigTom().isPresent()).toList();
    }

    private List<IOrgenhet> enheterMedSluttdatoFørStartdato(List<IOrgenhet> enheter) {
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


    private List<DvhOrgtilknytning> aktiveOrgtilknytninger(List<DvhOrgtilknytning> orgtilknytninger) {
        // FOM er før eller lik dagens dato, samt at TOM er null eller fremtidig
        return orgtilknytninger.stream().filter( ot ->
            ot.getGyldigFom().isBefore(LocalDate.now().plusDays(1)) &&
                (!ot.getGyldigTom().isPresent() || ot.getGyldigTom().get().isAfter(LocalDate.now()))
        ).toList();
    }

    private List<RessursState> ressurserForKoblinger(List<RessursState> ressurser, List<Orgtilknytning> orgtilknytninger) {
        return ressurser.stream().filter( r -> orgtilknytninger.stream().anyMatch(ot -> r.navident().equals(ot.getNavIdent()))).toList();
    }

    private List<RessursState> ressursKoblingForUniqueAgressoIder(List<RessursState> ressurser,List<DvhOrgtilknytning> orgtilknytninger, List<String> uniqueAgressoIder) {
        List<DvhOrgtilknytning> orgtilknytningerForAngitteUniqueAgressoIder =
            orgtilknytninger.stream().filter( e -> uniqueAgressoIder.contains(e.getUniqueAgressoId())).toList();

        return ressurser.stream().filter( r -> orgtilknytningerForAngitteUniqueAgressoIder.stream().anyMatch( ot -> ot.getNavIdent().equals(r.navident()))).toList();
    }

    private List<RessursState> ressurserUtenKoblinger(List<RessursState> ressurser, List<Orgtilknytning> orgtilknytninger) {
        return ressurser.stream().filter( r -> !orgtilknytninger.stream().anyMatch( ot -> ot.getNavIdent().equals(r.navident()))).toList();
    }

    private List<RessursState> ressurserForNavIdenter(List<RessursState> ressurser, List<String> navIdenter) {
        return ressurser.stream().filter( r -> navIdenter.contains(r.navident())).toList();
    }

    private List<RessursState> ressurserIkkeBlandtNavIdenter(List<RessursState> ressurser, List<String> navIdenter) {
        return ressurser.stream().filter( r -> !navIdenter.contains(r.navident())).toList();
    }
}


