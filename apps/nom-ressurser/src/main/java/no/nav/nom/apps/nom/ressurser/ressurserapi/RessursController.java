package no.nav.nom.apps.nom.ressurser.ressurserapi;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.nom.ressurser.ressurser.Ressurs;
import no.nav.nom.apps.nom.ressurser.ressurser.RessursRepository;
import no.nav.nom.apps.nom.ressurser.ressurserapi.exceptions.RessursNotFoundException;
import no.nav.nom.apps.nom.ressurser.ressurserapi.exceptions.UnauthorizedOperationException;
import no.nav.nom.contract.nom.dto.Ressurstype;
import no.nav.security.token.support.core.api.ProtectedWithClaims;
import no.nav.security.token.support.core.api.Unprotected;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;


import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@RestController
@Unprotected
@RequestMapping(value = "/api/v1/ressurs", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class RessursController {

    private static final String UNAUTHORIZED_MESSAGE = "Tilgang nektet";
    private static final String NOT_FOUND_MESSAGE = "Ressurs med NAV-ident %s ikke funnet";

    private final RessursRepository repository;
    private final TokenUtil tokenUtil;

    @GetMapping("/isAnsatt")
    @ProtectedWithClaims(issuer = "selvbetjening", claimMap = { "acr=Level4" })
    public boolean isAnsatt(@RequestParam String ressursType){
        final String personIdent = Optional
                .ofNullable(tokenUtil.getSubject())
                .orElseThrow(() -> new UnauthorizedOperationException(UNAUTHORIZED_MESSAGE));

        validerTilgang(personIdent);

        return repository.findAllByPersonident(personIdent).stream()
                .anyMatch(ressurs -> ressurs.getRessurstype().equals(Ressurstype.valueOf(ressursType.toUpperCase()))
                        && (ressurs.getSluttdato() == null || ressurs.getSluttdato().isAfter(LocalDate.now()))
                );
    }

    @GetMapping("/navn/{navIdent}")
    @ProtectedWithClaims(issuer = "selvbetjening", claimMap = { "acr=Level4" })
    public NavnResponseDTO getNavn(@PathVariable String navIdent){
        final String personIdent = Optional
                .ofNullable(tokenUtil.getSubject())
                .orElseThrow(() -> new UnauthorizedOperationException(UNAUTHORIZED_MESSAGE));

        validerTilgang(personIdent);

        Ressurs gyldigRessurs = repository.findByNavIdent(navIdent).stream()
                .filter(ressurs -> ressurs.getSluttdato() == null || ressurs.getSluttdato().isAfter(LocalDate.now()))
                .findFirst().orElseThrow(() -> new RessursNotFoundException(String.format(NOT_FOUND_MESSAGE, navIdent)));

        return NavnResponseDTO.builder().fornavn(gyldigRessurs.getFornavn()).etternavn(gyldigRessurs.getEtternavn()).build();
    }

    @GetMapping
    public List<Ressurs> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public Ressurs get(@PathVariable Long id) {
        return repository.getOne(id);
    }

    // Midlertidig rest-endepunkt for å manuelt opprette brukere.
    @PostMapping
    @Transactional(value = "transactionManager")
    public Ressurs createOrEdit(@RequestBody RessursRequestDTO ressurs) {
        Ressurs nyRessurs = converted(ressurs);
        nyRessurs.setEndretDato(LocalDate.now());
        return repository.save(nyRessurs);
    }

    private static Ressurs converted(RessursRequestDTO ressursRequestDTO){
        return Ressurs.builder()
                .personident(ressursRequestDTO.getPersonident())
                .navIdent(ressursRequestDTO.getNavIdent())
                .epost(ressursRequestDTO.getEpost())
                .fornavn(ressursRequestDTO.getFornavn())
                .etternavn(ressursRequestDTO.getEtternavn())
                .ressurstype(ressursRequestDTO.getRessurstype())
                .startdato(LocalDate.parse(ressursRequestDTO.getStartdato()))
                .sluttdato(ressursRequestDTO.getSluttdato() == null ? null : LocalDate.parse(ressursRequestDTO.getSluttdato()))
                .build();
    }
    private void validerTilgang(final String personident) {
        if (!tokenUtil.isAuthorized(personident)) {
            throw new UnauthorizedOperationException(UNAUTHORIZED_MESSAGE);
        }
    }

    @ExceptionHandler(UnauthorizedOperationException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedOperationException(final UnauthorizedOperationException e) {
        log.error(e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(e.getMessage(), HttpStatus.UNAUTHORIZED.value()));
    }

    @ExceptionHandler(RessursNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRessursNotFoundException(final RessursNotFoundException e) {
        log.error(e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(e.getMessage(), HttpStatus.UNAUTHORIZED.value()));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private class ErrorResponse {
        private String message;
        private int status;
    }
}
