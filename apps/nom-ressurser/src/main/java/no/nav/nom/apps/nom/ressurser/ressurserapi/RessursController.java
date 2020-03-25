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
    private static final String RESSURS_MED_NAVIDENT_NOT_FOUND_MESSAGE = "Ressurs med NAV-ident %s ikke funnet";
    private static final String RESSURS_MED_PERSONIDENT_NOT_FOUND_MESSAGE = "Ressurs med personident %s ikke funnet";

    private final RessursRepository repository;
    private final TokenUtil tokenUtil;

    @GetMapping("/ansattStatus")
    @ProtectedWithClaims(issuer = "selvbetjening", claimMap = { "acr=Level4" })
    public AnsattStatusResponseDTO ansattStatus() {
        final String personIdent = Optional
                .ofNullable(tokenUtil.getSubject())
                .orElseThrow(() -> new UnauthorizedOperationException(UNAUTHORIZED_MESSAGE));

        validerTilgang(personIdent);

        List<Ressurs> ressurser = repository.findAllByPersonidentAndRessurstype(personIdent, Ressurstype.INTERN);

        return ressurser.stream()
                .filter(ressurs -> ressurs.getSluttdato() == null || ressurs.getSluttdato().isAfter(LocalDate.now()))
                .map(ressurs -> AnsattStatusResponseDTO.builder().ansattNaaEllerNylig(true).gyldig(true).build())
                .findFirst()
                .orElse(ressurser.stream()
                        .filter(ressurs -> ressurs.getSluttdato() != null && ressurs.getSluttdato().isAfter(LocalDate.now().minusMonths(6)))
                        .map(ressurs -> AnsattStatusResponseDTO.builder().ansattNaaEllerNylig(true).gyldig(false).build())
                        .findFirst()
                        .orElseGet(() ->
                                AnsattStatusResponseDTO.builder().ansattNaaEllerNylig(false).build()));

    }

    @GetMapping("/navn/{navident}")
    @ProtectedWithClaims(issuer = "selvbetjening", claimMap = { "acr=Level4" })
    public NavnResponseDTO getNavn(@PathVariable String navident){
        final String personIdent = Optional
                .ofNullable(tokenUtil.getSubject())
                .orElseThrow(() -> new UnauthorizedOperationException(UNAUTHORIZED_MESSAGE));

        validerTilgang(personIdent);

        Ressurs gyldigRessurs = repository.findByNavident(navident).stream()
                .filter(ressurs -> ressurs.getSluttdato() == null || ressurs.getSluttdato().isAfter(LocalDate.now()))
                .findFirst().orElseThrow(() -> new RessursNotFoundException(String.format(RESSURS_MED_NAVIDENT_NOT_FOUND_MESSAGE, navident)));

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

    @GetMapping("/personident/{personident}")
    public Ressurs getByPersonident(@PathVariable String personident, @RequestParam String ressursType) {
        return repository.findAllByPersonident(personident).stream()
                .filter(ressurs -> ressurs.getRessurstype().equals(Ressurstype.valueOf(ressursType.toUpperCase()))
                        && (ressurs.getSluttdato() == null || ressurs.getSluttdato().isAfter(LocalDate.now())))
                .findFirst().orElseThrow(() -> new RessursNotFoundException(RESSURS_MED_PERSONIDENT_NOT_FOUND_MESSAGE));
    }

    // Midlertidig rest-endepunkt for å manuelt opprette brukere.
    @PostMapping
    @Transactional(value = "transactionManager")
    public Ressurs createOrEdit(@RequestBody RessursRequestDTO ressurs) {
        Ressurs nyRessurs = converted(ressurs);
        return repository.findByPersonident(nyRessurs.getPersonident())
                .map(eksisterende -> {
                    nyRessurs.setId(eksisterende.getId());
                    nyRessurs.setOpprettetDato(eksisterende.getOpprettetDato());
                    nyRessurs.setEndretDato(LocalDate.now());
                    return repository.save(nyRessurs);
                })
                .orElseGet(() -> {
                    nyRessurs.setOpprettetDato(LocalDate.now());
                    nyRessurs.setEndretDato(LocalDate.now());
                    return repository.save(nyRessurs);
                });
    }

    @GetMapping("/epost/{navident}")
    public String getEmailAdress(@PathVariable final String navident) {
        final Ressurs ressurs = repository
                .findFirstByNavidentAndEpostNotNullOrderByOpprettetDatoDesc(navident)
                .orElseThrow(() -> new RessursNotFoundException(String.format("Ressurs med navident %s ikke funnet", navident)));

        return ressurs.getEpost();
    }

    private static Ressurs converted(RessursRequestDTO ressursRequestDTO){
        return Ressurs.builder()
                .personident(ressursRequestDTO.getPersonident())
                .navident(ressursRequestDTO.getNavident())
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
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND.value()));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private class ErrorResponse {
        private String message;
        private int status;
    }
}
