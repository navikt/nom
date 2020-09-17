package no.nav.nom.skjermede.personer.foresporsel.api.v1;

import io.micrometer.core.instrument.Counter;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.skjermede.personer.common.NomRessurserService;
import no.nav.nom.skjermede.personer.common.TokenUtil;
import no.nav.nom.skjermede.personer.foresporsel.Foresporsel;
import no.nav.nom.skjermede.personer.foresporsel.ForesporselRepository;
import no.nav.nom.skjermede.personer.common.GraphService;
import no.nav.nom.skjermede.personer.foresporsel.api.v1.dto.ForesporselRequestDTO;
import no.nav.nom.skjermede.personer.foresporsel.api.v1.dto.ForesporselResponseDTO;
import no.nav.nom.skjermede.personer.foresporsel.azure.TokenService;
import no.nav.nom.skjermede.personer.foresporsel.api.v1.dto.TilknyttetForesporselDTO;
import no.nav.nom.skjermede.personer.skjerming.Kilde;
import no.nav.nom.skjermede.personer.skjerming.SkjermetPerson;
import no.nav.nom.skjermede.personer.skjerming.SkjermetPersonRepository;
import no.nav.security.token.support.core.api.ProtectedWithClaims;
import no.nav.security.token.support.core.api.Unprotected;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@Api(tags = "Forespørsel")
@RequiredArgsConstructor
@RequestMapping(value = ForesporselController.BASE_URL, produces = MediaType.APPLICATION_JSON_VALUE)
@ProtectedWithClaims(issuer = "selvbetjening", claimMap = { "acr=Level4" })
public class ForesporselController {

    @Value("${maanderPlusSkjerming}")
    private int mnderSkjermetPlus;

    public static final String BASE_URL = "/api/v1/foresporsel";
    public static final String FORESPORSEL_BEKREFTELSE = "bekreft";
    public static final String TILKNYTTET_FORESPORSEL_BEKREFTELSE = "tilknyttede/bekreft";
    public static final String TILNYTTEDE_FORESPORSLER = "tilknyttede";
    public static final String UNAUTHORIZED_MESSAGE = "Tilgang nektet";
    public static final String DUPLICATE_FORESPORSEL_MESSAGE = "Duplikat skjerming-forespørsel for innlogget bruker";
    public static final String NOT_FOUND_MESSAGE = "Fant ingen forespørsel for innlogget bruker";
    public static final String ANSATT_NOT_ACTIVE_MESSAGE = "Ansatt med tilknyttet NAV-ident er ikke gyldig";
    public static final String TILKNYTTET_FORESPORSEL_NOT_FOUND_MESSAGE = "Fant ikke den tilknyttede forespørselen";

    private final ForesporselRepository foresporselRepository;
    private final SkjermetPersonRepository skjermetPersonRepository;
    private final NomRessurserService nomRessurserService;
    private final TokenUtil tokenUtil;
    private final Counter foresporselGodkjentCounter;
    private final Counter foresporselOpprettetCounter;
    private final Counter foresporselSlettetCounter;
    private final GraphService graphService;
    private final TokenService tokenService;

    @Unprotected
    @Transactional(value = "transactionManager")
    @GetMapping(FORESPORSEL_BEKREFTELSE)
    @ApiOperation(value = "Unavailable", hidden = true)
    public String bekreftForesporsel(@RequestParam("id") final UUID funksjonellId) {
        val foresporsel = foresporselRepository.findByFunksjonellIdAndBekreftetFalse(funksjonellId).orElse(null);
        val accessToken = tokenService.getOboAccessToken();
        val navident = graphService.getNavident(accessToken);

        if (foresporsel == null) {
            log.error("Forsøk på bekreftelse av skjerming med ugyldig funksjonell id: {}", funksjonellId);
            return "Ugyldig operasjon";
        }

        if (!navident.equalsIgnoreCase(foresporsel.getTilknyttetRessurs())) {
            log.error("Forsøk på bekreftelse av skjerming av en uatorisert bruker: {}", navident);
            return UNAUTHORIZED_MESSAGE;
        }

        val skjermet = skjermetPersonRepository.getSkjermetPersonByPersonident(foresporsel.getPersonident()).orElse(null);

        if (skjermet != null) {
            skjermet.setSkjermetTil(LocalDateTime.now());
            skjermet.setEndretDato(LocalDateTime.now());
            skjermet.setEndretAv(Kilde.BRUKER);
            skjermetPersonRepository.save(skjermet);
        }

        val ressurs = nomRessurserService.getInternRessursByNavident(foresporsel.getTilknyttetRessurs(), true);

        foresporselGodkjentCounter.increment();

        foresporsel.setBekreftet(true);
        skjermetPersonRepository.save(mapToSkjermetPerson(foresporsel, ressurs.getSluttdato()));
        foresporselRepository.delete(foresporsel);

        return "Bekreftelse av skjerming registrert"; //OBS: Skjerming-proxy er avhengig av denne. Endrer du denne må du endre begge plassene.
    }

    @Transactional(value = "transactionManager")
    @PostMapping(TILKNYTTET_FORESPORSEL_BEKREFTELSE)
    @ApiOperation(value = "Unavailable", hidden = true)
    public TilknyttetForesporselDTO bekreftTilknyttetForesporsel(@RequestBody final TilknyttetForesporselDTO tilknyttetForesporsel) {
        validerTilgang();

        val personident = tokenUtil.getSubject();

        val ressurs = nomRessurserService.getInternRessursByPersonident(personident, true);

        if (ressurs == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ForesporselController.ANSATT_NOT_ACTIVE_MESSAGE);
        }

        val foresporsel = foresporselRepository.findByTilknyttetRessursAndFornavnAndEtternavnAndBekreftetFalse(ressurs.getNavident(), tilknyttetForesporsel.getFornavn(), tilknyttetForesporsel.getEtternavn())
                .orElse(null);

        if (foresporsel == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ForesporselController.TILKNYTTET_FORESPORSEL_NOT_FOUND_MESSAGE);
        }

        val skjermet = skjermetPersonRepository.getSkjermetPersonByPersonident(personident).orElse(null);

        if (skjermet != null) {
            skjermet.setSkjermetTil(LocalDateTime.now());
            skjermet.setEndretDato(LocalDateTime.now());
            skjermet.setEndretAv(Kilde.BRUKER);
            skjermetPersonRepository.save(skjermet);
        }

        foresporselGodkjentCounter.increment();

        foresporsel.setBekreftet(true);
        skjermetPersonRepository.save(mapToSkjermetPerson(foresporsel, ressurs.getSluttdato()));
        foresporselRepository.delete(foresporsel);

        return tilknyttetForesporsel;
    }

    @GetMapping
    @ApiResponses({
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 401, message = "Ugyldig Bearer token"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @ApiImplicitParam(
            paramType = "header", name = "Authorization",
            value = "\"Bearer \" +  token utstedt av IDPorten", required = true)
    @ApiOperation(
            value = "Henter gjeldende aktive forespørsel opprettet for godkjenning av et ansatt",
            notes = "Forespørselen er aktiv så lenge den ikke er godkjent",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ForesporselResponseDTO hentForesporsel() {
        validerTilgang();

        val personident = tokenUtil.getSubject();
        val foresporsel = foresporselRepository.findByPersonidentAndBekreftetFalse(personident).orElse(null);

        return foresporsel != null ? ForesporselResponseDTO.fromEntity(foresporsel) : null;
    }

    @GetMapping(TILNYTTEDE_FORESPORSLER)
    @ApiResponses({
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 401, message = "Ugyldig Bearer token"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @ApiImplicitParam(
            paramType = "header", name = "Authorization",
            value = "\"Bearer \" +  token utstedt av IDPorten", required = true)
    @ApiOperation(
            value = "Henter aktive forespørsler med tilknyttet ressurs lik den ansattes NAV-ident",
            notes = "Forespørsler er aktive så lenge den ikke er godkjent",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public List<TilknyttetForesporselDTO> hentTilknyttedeForsporsler() {
        validerTilgang();

        val personident = tokenUtil.getSubject();
        val ressurs = nomRessurserService.getInternRessursByPersonident(personident, true);

        if (ressurs == null) {
            return null;
        }

        return foresporselRepository.findAllByTilknyttetRessurs(ressurs.getNavident()).stream().map(TilknyttetForesporselDTO::fromEntity).collect(Collectors.toList());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @ApiResponses({
            @ApiResponse(code = 201, message = "Forespørsel opprettet"),
            @ApiResponse(code = 401, message = "Ugyldig Bearer token"),
            @ApiResponse(code = 404, message = "Duuplikat forespørsel eller ugyldige forespørselopplysninger"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @ApiImplicitParam(
            paramType = "header", name = "Authorization",
            value = "\"Bearer \" +  token utstedt av IDPorten", required = true)
    @ApiOperation(
            value = "Oppretter nytt skjermingsforespørsel",
            notes = "Skjermingsforespørsler opprettes av interne ansattes familiemedlemmer",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ForesporselResponseDTO> opprettForesporsel(@ApiParam(name = "Forespørselopplysninger") @RequestBody final ForesporselRequestDTO foresporselRequestDTO) {
        validerTilgang();

        val personident = tokenUtil.getSubject();

        if (foresporselRepository.existsByPersonidentAndBekreftetFalse(personident)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ForesporselController.DUPLICATE_FORESPORSEL_MESSAGE);
        }

        val foresporsel = foresporselRequestDTO.toEntity();
        foresporsel.setPersonident(personident);
        foresporsel.setFunksjonellId(UUID.randomUUID());
        foresporsel.setBekreftet(false);
        foresporsel.setOpprettetDato(LocalDateTime.now());

        foresporselOpprettetCounter.increment();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ForesporselResponseDTO.fromEntity(foresporselRepository.save(foresporsel)));
    }

    @DeleteMapping
    @ApiResponses({
            @ApiResponse(code = 200, message = "Slettet"),
            @ApiResponse(code = 401, message = "Ugyldig Bearer token"),
            @ApiResponse(code = 404, message = "Ingen aktivt skjermingsforespørsel funnet"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @ApiImplicitParam(
            paramType = "header", name = "Authorization",
            value = "\"Bearer \" +  token utstedt av IDPorten", required = true)
    @ApiOperation(
            value = "Sletter gjeldende aktive skjermingsforespørsel",
            notes = "Forespørselen er aktiv så lenge den ikke er godkjent",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ForesporselResponseDTO slettForesporsel() {
        validerTilgang();

        val personident = tokenUtil.getSubject();
        val foresporsel = foresporselRepository.findByPersonidentAndBekreftetFalse(personident)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, NOT_FOUND_MESSAGE));

        foresporselRepository.delete(foresporsel);
        foresporselSlettetCounter.increment();

        return ForesporselResponseDTO.fromEntity(foresporsel);
    }

    private void validerTilgang() {
        if (!tokenUtil.isAuthorized()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, ForesporselController.UNAUTHORIZED_MESSAGE);
        }
    }

    private SkjermetPerson mapToSkjermetPerson(final Foresporsel foresporsel, LocalDate sluttdato) {
        return SkjermetPerson.builder()
                .personident(foresporsel.getPersonident())
                .fornavn(foresporsel.getFornavn())
                .etternavn(foresporsel.getEtternavn())
                .tilknyttetRessurs(foresporsel.getTilknyttetRessurs())
                .skjermetFra(LocalDateTime.now())
                .skjermetTil(sluttdato != null ? sluttdato.plusMonths(mnderSkjermetPlus).atTime(23, 59) : null)
                .opprettetDato(LocalDateTime.now())
                .endretDato(LocalDateTime.now())
                .kafkaProduced(false)
                .endretAv(Kilde.BRUKER)
                .opprettetAv(Kilde.BRUKER)
                .build();
    }
}
