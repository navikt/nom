package no.nav.nom.skjermede.personer.skjerming.api.v1;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.skjermede.personer.common.NomRessurserService;
import no.nav.nom.skjermede.personer.common.TokenUtil;
import no.nav.nom.skjermede.personer.common.Unntakskode;
import no.nav.nom.skjermede.personer.skjerming.Kilde;
import no.nav.nom.skjermede.personer.skjerming.SkjermetPerson;
import no.nav.nom.skjermede.personer.skjerming.SkjermetPersonRepository;
import no.nav.nom.skjermede.personer.skjerming.api.v1.dto.SkjermetPersonResponseDTO;
import no.nav.security.token.support.core.api.ProtectedWithClaims;
import no.nav.security.token.support.core.api.Unprotected;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@Api(tags = "skjerming-ui")
@RequiredArgsConstructor
@RequestMapping(value = SkjermedePersonerUIController.BASE_URL, produces = MediaType.APPLICATION_JSON_VALUE)
@ProtectedWithClaims(issuer = "selvbetjening", claimMap = { "acr=Level4" })
public class SkjermedePersonerUIController {

    public static final String BASE_URL = "/api/v1/skjerming-ui";
    public static final String ALLE = "alle";
    public static final String FAMILIEMEDLEMMER = "familiemedlemmer";
    public static final String NOT_FOUND_MESSAGE = "Fant ingen skjerming for innlogget bruker";
    public static final String UNAUTHORIZED_MESSAGE = "Tilgang nektet";
    public static final String DUPLICATE_SKJERMING_MESSAGE = "Personen er allerede skjermet";

    private final SkjermetPersonRepository skjermetPersonRepository;
    private final NomRessurserService nomRessurserService;
    private final TokenUtil tokenUtil;

    @GetMapping("all")
    @Unprotected
    @ApiOperation(value = "unavailable", hidden = true)
    public List<SkjermetPersonResponseDTO> hentAlle() {
        return skjermetPersonRepository
                .findAll()
                .stream()
                .map(SkjermetPersonResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @GetMapping
    @ApiResponses({
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 401, message = "Ugyldig Bearer token eller uatorisert konsument"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @ApiImplicitParam(
            paramType = "header", name = "Authorization",
            value = "\"Bearer \" +  token utstedt av IDPorten", required = true)
    @ApiOperation(
            value = "Henter opplysninger om en aktiv skjerming for et internt ansatt",
            notes = "Konsumenten, identifisert via id-tokenet, kan hente skjermingsopplysninger kun om seg selv",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public SkjermetPersonResponseDTO hentSkjerming() {
        validerTilgang();

        val personident = tokenUtil.getSubject();
        val skjermetPerson = skjermetPersonRepository.getSkjermetPersonByPersonident(personident).orElse(null);

        if (skjermetPerson != null && skjermetPerson.getTilknyttetRessurs() != null
            && Arrays.stream(Unntakskode.values()).noneMatch(kode -> kode.toString().equals(skjermetPerson.getTilknyttetRessurs()))) {
            val ressurs = nomRessurserService.getInternRessursByNavident(skjermetPerson.getTilknyttetRessurs(), false);

            return SkjermetPersonResponseDTO.fromEntityAndRessurs(skjermetPerson, ressurs);
        }

        return skjermetPerson != null ? SkjermetPersonResponseDTO.fromEntity(skjermetPerson) : null;
    }

    @GetMapping(FAMILIEMEDLEMMER)
    @ApiResponses({
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 401, message = "Ugyldig Bearer token eller uatorisert konsument"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @ApiImplicitParam(
            paramType = "header", name = "Authorization",
            value = "\"Bearer \" +  token utstedt av IDPorten", required = true)
    @ApiOperation(
            value = "Henter opplysninger om aktive skjerminger for familiemedlemmer til et internt ansatt",
            notes = "Konsumenten, identifisert via id-tokenet, kan hente skjermingsopplysninger kun om sine familimedlemmer",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public List<SkjermetPersonResponseDTO> hentSkjermedeFamiliemedlemer() {
        validerTilgang();

        val personident = tokenUtil.getSubject();
        val ressurs = nomRessurserService.getInternRessursByPersonident(personident, Boolean.FALSE);

        return skjermetPersonRepository.findAllByTilknyttetRessurs(ressurs.getNavident())
                .stream()
                .map(SkjermetPersonResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @GetMapping(ALLE)
    @ApiResponses({
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 401, message = "Ugyldig Bearer token eller uatorisert konsument"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @ApiImplicitParam(
            paramType = "header", name = "Authorization",
            value = "\"Bearer \" +  token utstedt av IDPorten", required = true)
    @ApiOperation(
            value = "Henter opplysninger om aktiv og historiske skjerminger for et internt ansatt",
            notes = "Konsumenten, identifisert via id-tokenet, kan hente skjermingsopplysninger kun om seg selv",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public List<SkjermetPersonResponseDTO> hentAlleSkjerminger() {
        validerTilgang();

        val personident = tokenUtil.getSubject();

        return skjermetPersonRepository.findAllByPersonident(personident)
                .stream()
                .map(SkjermetPersonResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional(value = "transactionManager")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Skjerming opprettet"),
            @ApiResponse(code = 400, message = "Forsøk på duplikat skjerming"),
            @ApiResponse(code = 401, message = "Ugyldig Bearer token eller uatorisert konsument"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @ApiImplicitParam(
            paramType = "header", name = "Authorization",
            value = "\"Bearer \" +  token utstedt av IDPorten", required = true)
    @ApiOperation(
            value = "Skjermer et internt ansatt",
            notes = "Konsumenten, identifisert via id-tokenet, kan skjerme kun seg selv",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SkjermetPersonResponseDTO> opprettSkjerming() {
        validerTilgang();

        val personident = tokenUtil.getSubject();

        if (skjermetPersonRepository.getSkjermetPersonByPersonident(personident).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, DUPLICATE_SKJERMING_MESSAGE);
        }

        val ressurs = nomRessurserService.getInternRessursByPersonident(personident, Boolean.TRUE);
        val skjermetPerson = SkjermetPerson.builder()
                .personident(personident)
                .fornavn(ressurs.getFornavn())
                .etternavn(ressurs.getEtternavn())
                .skjermetFra(LocalDateTime.now())
                .opprettetDato(LocalDateTime.now())
                .endretDato(LocalDateTime.now())
                .kafkaProduced(false)
                .endretAv(Kilde.BRUKER)
                .opprettetAv(Kilde.BRUKER)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SkjermetPersonResponseDTO.fromEntity(skjermetPersonRepository.save(skjermetPerson)));
    }

    @PutMapping
    @Transactional(value = "transactionManager")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Skjerming opphørt"),
            @ApiResponse(code = 401, message = "Ugyldig Bearer token eller uatorisert konsument"),
            @ApiResponse(code = 404, message = "Skjerming for gjeldende ansatt ikke funnet"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @ApiImplicitParam(
            paramType = "header", name = "Authorization",
            value = "\"Bearer \" +  token utstedt av IDPorten", required = true)
    @ApiOperation(
            value = "Opphever aktiv skjerming for et internt ansatt",
            notes = "Konsumenten, identifisert via id-tokenet, kan avskjerme kun seg selv",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public SkjermetPersonResponseDTO opphorSkjerming() {
        validerTilgang();

        val personident = tokenUtil.getSubject();
        val skjermetPerson = skjermetPersonRepository
                .getSkjermetPersonByPersonident(personident)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, NOT_FOUND_MESSAGE));

        skjermetPerson.setSkjermetTil(LocalDateTime.now());
        skjermetPerson.setEndretDato(LocalDateTime.now());
        skjermetPerson.setKafkaProduced(false);
        skjermetPerson.setEndretAv(Kilde.BRUKER);

        return SkjermetPersonResponseDTO.fromEntity(skjermetPersonRepository.save(skjermetPerson));
    }

    private void validerTilgang() {
        if (!tokenUtil.isAuthorized()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, UNAUTHORIZED_MESSAGE);
        }
    }
}
