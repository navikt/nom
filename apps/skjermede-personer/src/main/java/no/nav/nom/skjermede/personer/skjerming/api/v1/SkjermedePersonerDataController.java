package no.nav.nom.skjermede.personer.skjerming.api.v1;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.skjermede.personer.skjerming.Kilde;
import no.nav.nom.skjermede.personer.skjerming.SkjermetPerson;
import no.nav.nom.skjermede.personer.skjerming.SkjermetPersonRepository;
import no.nav.nom.skjermede.personer.skjerming.api.v1.dto.SkjermetPersonDataRequestDTO;
import no.nav.nom.skjermede.personer.skjerming.api.v1.dto.SkjermetPersonDataResponseDTO;
import no.nav.security.token.support.core.api.ProtectedWithClaims;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@Api(tags = "Skjermingdata")
@RequiredArgsConstructor
@RequestMapping(value = SkjermedePersonerDataController.BASE_URL, produces = MediaType.APPLICATION_JSON_VALUE)
@ProtectedWithClaims(issuer = "reststs")
public class SkjermedePersonerDataController {

    public static final String BASE_URL = "/api/v1/skjermingdata";
    public static final String DELETE_SKJERMING = "{personident}";
    public static final String AVSKJERM = "opphor/{personident}";
    public static final String IKKE_SKJERMET = "Fant ikke skjerming for oppgitte personident";
    public static final String DUPLIKAT_MELDING = "Duplikate skjerminger intraff";

    private final SkjermetPersonRepository skjermetPersonRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional(value = "transactionManager")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Opprettet"),
            @ApiResponse(code = 400, message = "Forsøk på duplikate skjerminger eller ugyldige skjermingsopplysninger"),
            @ApiResponse(code = 401, message = "Ugyldig Bearer token eller uatorisert konsument"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @ApiImplicitParam(
            paramType = "header", name = "Authorization",
            value = "\"Bearer \" +  sts utstedt access token", required = true)
    @ApiOperation(
            value = "Oppretter skjerminger",
            notes = "Konsumeres kun av dolly",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<SkjermetPersonDataResponseDTO>> opprettSkjerminger(
            @ApiParam("Liste med skjerminger")
            @RequestBody final List<SkjermetPersonDataRequestDTO> request) {
        sjekkDuplikater(request);

        val skjerminger = request.stream().
                map(dto -> {
                    SkjermetPerson entity = dto.toEntity();
                    entity.setOpprettetAv(Kilde.SERVICEBRUKER);
                    entity.setOpprettetDato(LocalDateTime.now());
                    entity.setEndretDato(LocalDateTime.now());
                    entity.setEndretAv(Kilde.SERVICEBRUKER);
                    return entity;
                }).collect(Collectors.toList());
        val lagredeSkjerminger = skjermetPersonRepository
                .saveAll(skjerminger)
                .stream()
                .map(SkjermetPersonDataResponseDTO::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.CREATED).body(lagredeSkjerminger);
    }

    @DeleteMapping(DELETE_SKJERMING)
    @Transactional(value = "transactionManager")
    @ApiResponses({
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 401, message = "Ugyldig Bearer token eller uatorisert konsument"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @ApiImplicitParam(
            paramType = "header", name = "Authorization",
            value = "\"Bearer \" +  sts utstedt access token", required = true)
    @ApiOperation(
            value = "Sletter all skjermingshistorikk for spesifisert personident",
            notes = "Konsumeres kun av dolly",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public void slettSkjerming(@ApiParam("Fødselsnummeret til personen") @PathVariable final String personident) {
        skjermetPersonRepository.deleteAll(skjermetPersonRepository.findAllByPersonident(personident));
    }

    @PutMapping(AVSKJERM)
    @Transactional(value = "transactionManager")
    @ApiResponses({
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 401, message = "Ugyldig Bearer token eller uatorisert konsument"),
            @ApiResponse(code = 404, message = "Skjerming for oppgitt personident ikke funnet"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @ApiImplicitParam(
            paramType = "header", name = "Authorization",
            value = "\"Bearer \" +  sts utstedt access token", required = true)
    @ApiOperation(
            value = "Opphever skjerming for spesifisert personident",
            notes = "Konsumeres kun av dolly",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public void opphorSkjerming(@ApiParam("Fødselsnummeret til personen") @PathVariable final String personident) {
        val skjermetPerson = skjermetPersonRepository
                .getSkjermetPersonByPersonident(personident)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, IKKE_SKJERMET));

        skjermetPerson.setSkjermetTil(LocalDateTime.now());
        skjermetPersonRepository.save(skjermetPerson);
    }

    public void sjekkDuplikater(final List<SkjermetPersonDataRequestDTO> request) {
        request.forEach(skjerming -> {
            if (skjermetPersonRepository.getSkjermetPersonByPersonident(skjerming.getPersonident()).isPresent()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, DUPLIKAT_MELDING);
            }
        });
    }
}
