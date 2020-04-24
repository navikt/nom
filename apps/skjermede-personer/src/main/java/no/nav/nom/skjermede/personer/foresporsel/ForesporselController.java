package no.nav.nom.skjermede.personer.foresporsel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.skjermede.personer.common.TokenUtil;
import no.nav.nom.skjermede.personer.common.exception.UnauthorizedOperationException;
import no.nav.nom.skjermede.personer.foresporsel.dto.ForesporselRequestDTO;
import no.nav.nom.skjermede.personer.foresporsel.dto.ForesporselResponseDTO;
import no.nav.nom.skjermede.personer.foresporsel.exception.DuplicateForesporselException;
import no.nav.nom.skjermede.personer.skjerming.SkjermetPerson;
import no.nav.nom.skjermede.personer.skjerming.SkjermetPersonRepository;
import no.nav.security.token.support.core.api.ProtectedWithClaims;
import no.nav.security.token.support.core.api.Unprotected;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
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
    @Transactional
    @GetMapping(ForesporselConstants.FORESPORSEL_BEKREFTELSE)
    public String bekreftForesporsel(@RequestParam("id") final UUID funksjonellId) {
        val foresporsel = foresporselRepository
                .findByFunksjonellIdAndBekreftetNull(funksjonellId)
                .orElse(null);

        if (foresporsel == null) {
            log.error("Forsøk på bekreftelse av skjerming med ugyldig funksjonell id: {}", funksjonellId);
            return "Ugyldig operasjon";
        }

        val skjermet = skjermetPersonRepository
                .getSkjermetPersonByPersonident(foresporsel.getPersonident())
                .orElse(null);

        if (skjermet != null) {
            skjermet.setSkjermetTil(LocalDateTime.now());
            skjermet.setEndretDato(LocalDateTime.now());
            skjermetPersonRepository.save(skjermet);
        }

        foresporsel.setBekreftet(true);
        skjermetPersonRepository.save(mapToSkjermetPerson(foresporsel));
        foresporselRepository.delete(foresporsel);

        return "Bekreftelse av skjerming registrert";
    }

    @GetMapping
    public ForesporselResponseDTO hentForesporsel() {
        validerTilgang();

        val personident = tokenUtil.getSubject();
        val foresporsel = foresporselRepository
                .findByPersonidentAndBekreftetNull(personident)
                .orElse(null);

        return foresporsel != null ? ForesporselResponseDTO.fromEntity(foresporsel) : null;
    }

    @PostMapping
    public ResponseEntity<ForesporselResponseDTO> opprettForesporsel(@RequestBody final ForesporselRequestDTO foresporselRequestDTO) {
        validerTilgang();

        val personident = tokenUtil.getSubject();

        if (foresporselRepository.existsByPersonident(personident)) {
            throw new DuplicateForesporselException(ForesporselConstants.DUPLICATE_FORESPORSEL_MESSAGE);
        }

        val foresporsel = foresporselRequestDTO.toEntity();
        foresporsel.setPersonident(personident);
        foresporsel.setFunksjonellId(UUID.randomUUID());
        foresporsel.setOpprettetDato(LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ForesporselResponseDTO.fromEntity(foresporselRepository.save(foresporsel)));
    }

    private void validerTilgang() {
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
                .skjermetFra(LocalDateTime.now())
                .opprettetDato(LocalDateTime.now())
                .endretDato(LocalDateTime.now())
                .build();
    }
}
