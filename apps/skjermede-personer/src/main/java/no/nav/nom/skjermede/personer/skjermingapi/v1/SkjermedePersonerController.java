package no.nav.nom.skjermede.personer.skjermingapi.v1;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.skjermede.personer.skjerming.SkjermetPerson;
import no.nav.nom.skjermede.personer.skjermingapi.v1.dto.SkjermetPersonRequestDTO;
import no.nav.nom.skjermede.personer.skjermingapi.v1.dto.SkjermetPersonResponseDTO;
import no.nav.nom.skjermede.personer.skjermingapi.v1.exception.DuplicateSkjermingException;
import no.nav.nom.skjermede.personer.skjermingapi.v1.exception.SkjermingNotFoundException;
import no.nav.nom.skjermede.personer.skjermingapi.v1.exception.UnauthorizedOperationException;
import no.nav.nom.skjermede.personer.skjerming.SkjermetPersonRepository;
import no.nav.security.token.support.core.api.ProtectedWithClaims;
import no.nav.security.token.support.core.api.Unprotected;
import no.nav.security.token.support.core.exceptions.JwtTokenValidatorException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@ProtectedWithClaims(issuer = "selvbetjening", claimMap = { "acr=Level4" })
@RequestMapping(SkjermedePersonerController.BASE_URL)
public class SkjermedePersonerController {

    public static final String BASE_URL = "/api/v1/skjerming";
    public static final String BY_FUNKSJONELL_ID = "{funksjonellId}";
    public static final String BY_ANSATT = "ansatt";
    public static final String FAMILIEMEDLEMER = "familiemedlemer";
    public static final String NOT_FOUND_MESSAGE = "Skjerming med funksjonell id %d ikke funnet";
    public static final String UNAUTHORIZED_MESSAGE = "Tilgang nektet";
    public static final String DUPLICATE_MESSAGE = "Personen er allerede skjermet";

    private final SkjermetPersonRepository skjermetPersonRepository;
    private final TokenUtil tokenUtil;

    @GetMapping
    @Unprotected
    public List<SkjermetPersonResponseDTO> hentAlleSkjerminger() {
        return skjermetPersonRepository
                .findAll()
                .stream()
                .map(SkjermetPersonResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @GetMapping(SkjermedePersonerController.BY_FUNKSJONELL_ID)
    public SkjermetPersonResponseDTO hentSkjerming(@PathVariable final Long funksjonellId) {
        final SkjermetPerson skjermetPerson = skjermetPersonRepository
                .findDistinctByFunksjonellId(funksjonellId)
                .orElseThrow(() -> new SkjermingNotFoundException(String.format(NOT_FOUND_MESSAGE, funksjonellId)));

        validerTilgang(skjermetPerson.getPersonident());

        return SkjermetPersonResponseDTO.fromEntity(skjermetPerson);
    }

    @GetMapping(SkjermedePersonerController.FAMILIEMEDLEMER)
    public List<SkjermetPersonResponseDTO> hentSkjermedeFamiliemedlemer() {
        final String personident = Optional
                .ofNullable(tokenUtil.getSubject())
                .orElseThrow(() -> new UnauthorizedOperationException(UNAUTHORIZED_MESSAGE));

        validerTilgang(personident);

        return skjermetPersonRepository.findAllByTilknyttetRessurs(personident)
                .stream()
                .map(SkjermetPersonResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @GetMapping(SkjermedePersonerController.BY_ANSATT)
    public List<SkjermetPersonResponseDTO> hentSkjermingerForAnsatt() {
        final String personident = Optional
                .ofNullable(tokenUtil.getSubject())
                .orElseThrow(() -> new UnauthorizedOperationException(UNAUTHORIZED_MESSAGE));

        validerTilgang(personident);

        return skjermetPersonRepository.findAllByPersonident(personident)
                .stream()
                .map(SkjermetPersonResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @PostMapping
    @Transactional
    public SkjermetPersonResponseDTO opprettSkjerming(@RequestBody final SkjermetPersonRequestDTO skjermetPersonDto) {
        if (skjermetPersonRepository.existsByPersonidentAndSkjermetTomNull(skjermetPersonDto.getPersonident())) {
            throw new DuplicateSkjermingException(DUPLICATE_MESSAGE);
        }

        validerTilgang(skjermetPersonDto.getPersonident());

        final SkjermetPerson skjermetPerson = skjermetPersonDto.toEntity();
        skjermetPerson.setFunksjonellId(skjermetPersonRepository.nextFunctionalId());
        skjermetPerson.setOpprettetDato(LocalDateTime.now());
        skjermetPerson.setEndretDato(LocalDateTime.now());

        return SkjermetPersonResponseDTO.fromEntity(skjermetPersonRepository.save(skjermetPerson));
    }

    @PutMapping(SkjermedePersonerController.BY_FUNKSJONELL_ID)
    @Transactional
    public SkjermetPersonResponseDTO oppdaterSkjerming(@PathVariable final Long funksjonellId,
                                                       @RequestBody final SkjermetPersonRequestDTO skjermetPersonDTO) {
        final SkjermetPerson skjermetPerson = skjermetPersonRepository
                .findDistinctByFunksjonellIdAndSkjermetTomNull(funksjonellId)
                .orElseThrow(() -> new SkjermingNotFoundException(String.format(NOT_FOUND_MESSAGE, funksjonellId)));

        validerTilgang(skjermetPerson.getPersonident());

        skjermetPerson.setSkjermetTom(skjermetPersonDTO.getSkjermetTom());
        skjermetPerson.setEndretDato(LocalDateTime.now());

        return SkjermetPersonResponseDTO.fromEntity(skjermetPersonRepository.save(skjermetPerson));
    }

    @DeleteMapping(SkjermedePersonerController.BY_FUNKSJONELL_ID)
    @Transactional
    public void slettSkjerming(@PathVariable final Long funksjonellId) {
        final SkjermetPerson skjermetPerson = skjermetPersonRepository
                .findDistinctByFunksjonellId(funksjonellId)
                .orElseThrow(() -> new SkjermingNotFoundException(String.format(NOT_FOUND_MESSAGE, funksjonellId)));

        validerTilgang(skjermetPerson.getPersonident());
        skjermetPersonRepository.deleteById(skjermetPerson.getId());
    }

    private void validerTilgang(final String personident) {
        if (!tokenUtil.isAuthorized(personident)) {
            throw new UnauthorizedOperationException(UNAUTHORIZED_MESSAGE);
        }
    }

    @ExceptionHandler(SkjermingNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSkjermingNotFoundException(final SkjermingNotFoundException e) {
        log.info(e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND.value()));
    }

    @ExceptionHandler(DuplicateSkjermingException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateSkjermingException(final DuplicateSkjermingException e) {
        log.error(e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST.value()));
    }

    @ExceptionHandler(UnauthorizedOperationException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedOperationException(final UnauthorizedOperationException e) {
        log.error(e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(e.getMessage(), HttpStatus.UNAUTHORIZED.value()));
    }

    @ExceptionHandler(JwtTokenValidatorException.class)
    public ResponseEntity<ErrorResponse> handleJwtTokenValidatorException(final JwtTokenValidatorException e) {
        log.error(e.getCause().getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(e.getCause().getMessage(), HttpStatus.UNAUTHORIZED.value()));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private class ErrorResponse {
        private String message;
        private int status;
    }
}
