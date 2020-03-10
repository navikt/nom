package no.nav.nom.apps.nom.ressurser.ressurserapi;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.nom.ressurser.ressurser.Ressurs;
import no.nav.nom.apps.nom.ressurser.ressurser.RessursRepository;
import no.nav.nom.contract.nom.dto.Ressurstype;
import no.nav.security.token.support.core.api.ProtectedWithClaims;
import no.nav.security.token.support.core.api.Unprotected;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    private final RessursService service;
    private final RessursRepository repository;
    private final TokenUtil tokenUtil;

    @GetMapping("/isAnsatt")
    @ProtectedWithClaims(issuer = "selvbetjening", claimMap = { "acr=Level4" })
    public boolean isAnsatt(@RequestParam String ressursType){
        final String personident = Optional
                .ofNullable(tokenUtil.getSubject())
                .orElseThrow(() -> new UnauthorizedOperationException(UNAUTHORIZED_MESSAGE));

        validerTilgang(personident);

        return service.hentAlleAnsattForholdForPerson(personident).stream()
                .anyMatch(ressurs -> ressurs.getRessurstype().equals(Ressurstype.valueOf(ressursType.toUpperCase()))
                        && (ressurs.getSluttdato() == null || ressurs.getSluttdato().isAfter(LocalDate.now()))
                );
    }

    @GetMapping
    public List<Ressurs> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public Ressurs get(@PathVariable Long id) {
        return repository.getOne(id);
    }

    @PostMapping
    public Ressurs createOrEdit(@RequestBody RessursRequestDTO ressurs) {
        return service.saveAndSend(converted(ressurs));
    }

    private static Ressurs converted(RessursRequestDTO ressursRequestDTO){
        return Ressurs.builder()
                .personident(ressursRequestDTO.getPersonident())
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

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private class ErrorResponse {
        private String message;
        private int status;
    }
}
