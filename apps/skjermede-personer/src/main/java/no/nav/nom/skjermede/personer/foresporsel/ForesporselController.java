package no.nav.nom.skjermede.personer.foresporsel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.skjermede.personer.common.TokenUtil;
import no.nav.nom.skjermede.personer.common.exception.UnauthorizedOperationException;
import no.nav.nom.skjermede.personer.foresporsel.exception.DuplicateForesporselException;
import no.nav.nom.skjermede.personer.foresporsel.exception.ForesporselNotFoundException;
import no.nav.nom.skjermede.personer.skjerming.SkjermetPerson;
import no.nav.nom.skjermede.personer.skjerming.SkjermetPersonRepository;
import no.nav.security.token.support.core.api.ProtectedWithClaims;
import no.nav.security.token.support.core.api.Unprotected;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(ForesporselConstants.BASE_URL)
@ProtectedWithClaims(issuer = "selvbetjening", claimMap = { "acr=Level4" })
public class ForesporselController {

    private final ForesporselRepository foresporselRepository;
    private final SkjermetPersonRepository skjermetPersonRepository;
    private final TokenUtil tokenUtil;

    @Unprotected
    @GetMapping(ForesporselConstants.FORESPORSEL_GODKJENNING)
    public String godkjennForesporsel(@RequestParam("id") final UUID funksjonellId) {
        val foresporsel = foresporselRepository
                .findByFunksjonellIdAndBekreftetNull(funksjonellId)
                .orElse(null);

        if (foresporsel == null) {
            log.error("Forsøk på godkjenning av skjerming med ugyldig funksjonell id: {}", funksjonellId);
            return "Ugyldig operasjon";
        }

        foresporsel.setBekreftet(true);
        foresporsel.setBekreftetDato(LocalDate.now());
        skjermetPersonRepository.save(mapToSkjermetPerson(foresporsel));

        return "Godkenning er registrert";
    }

    @GetMapping
    public ForesporselResponseDTO hentForesporsel() {
        final String personident = Optional
                .ofNullable(tokenUtil.getSubject())
                .orElseThrow(() -> new UnauthorizedOperationException(ForesporselConstants.UNAUTHORIZED_MESSAGE));

        validerTilgang(personident);

        Foresporsel foresporsel = foresporselRepository.findByPersonidentAndBekreftetNull(personident)
                .orElseThrow(() -> new ForesporselNotFoundException(String.format(ForesporselConstants.FORESPORSEL_NOT_FOUND, personident)));

        return ForesporselResponseDTO.fromEntity(foresporsel);
    }

    @PostMapping
    public ResponseEntity<ForesporselResponseDTO> opprettForesporsel(@RequestBody final ForesporselRequestDTO foresporselRequestDTO) {
        final String personident = Optional
                .ofNullable(tokenUtil.getSubject())
                .orElseThrow(() -> new UnauthorizedOperationException(ForesporselConstants.UNAUTHORIZED_MESSAGE));

        validerTilgang(personident);

        if (foresporselRepository.existsByPersonident(personident)) {
            throw new DuplicateForesporselException(ForesporselConstants.DUPLICATE_FORESPORSEL_MESSAGE);
        }

        final Foresporsel foresporsel = foresporselRequestDTO.toEntity();
        foresporsel.setPersonident(personident);
        foresporsel.setFunksjonellId(UUID.randomUUID());
        foresporsel.setOpprettetDato(LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ForesporselResponseDTO.fromEntity(foresporselRepository.save(foresporsel)));
    }

    private void validerTilgang(final String personident) {
        if (!tokenUtil.isAuthorized()) {
            throw new UnauthorizedOperationException(ForesporselConstants.UNAUTHORIZED_MESSAGE);
        }
    }

    private SkjermetPerson mapToSkjermetPerson(final Foresporsel foresporsel) {
        return SkjermetPerson.builder()
                .personident(foresporsel.getPersonident())
                .fornavn(foresporsel.getFornavn())
                .etternavn(foresporsel.getEtternavn())
                .tilknyttetRessurs(foresporsel.getTilknyttetRessurs())
                .skjermetFom(LocalDate.now())
                .opprettetDato(LocalDateTime.now())
                .endretDato(LocalDateTime.now())
                .build();
    }
}
