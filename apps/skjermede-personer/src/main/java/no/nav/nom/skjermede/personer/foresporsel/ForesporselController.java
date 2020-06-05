package no.nav.nom.skjermede.personer.foresporsel;

import io.micrometer.core.instrument.Counter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.skjermede.personer.common.NomRessurserService;
import no.nav.nom.skjermede.personer.common.TokenUtil;
import no.nav.nom.skjermede.personer.foresporsel.dto.ForesporselRequestDTO;
import no.nav.nom.skjermede.personer.foresporsel.dto.ForesporselResponseDTO;
import no.nav.nom.skjermede.personer.skjerming.Kilde;
import no.nav.nom.skjermede.personer.skjerming.SkjermetPerson;
import no.nav.nom.skjermede.personer.skjerming.SkjermetPersonRepository;
import no.nav.security.token.support.core.api.ProtectedWithClaims;
import no.nav.security.token.support.core.api.Unprotected;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(ForesporselController.BASE_URL)
@ProtectedWithClaims(issuer = "selvbetjening", claimMap = { "acr=Level4" })
public class ForesporselController {

    @Value("${maanderPlusSkjerming}")
    private int mnderSkjermetPlus;

    public static final String BASE_URL = "/api/v1/foresporsel";
    public static final String FORESPORSEL_BEKREFTELSE = "bekreft";
    public static final String UNAUTHORIZED_MESSAGE = "Tilgang nektet";
    public static final String DUPLICATE_FORESPORSEL_MESSAGE = "Duplikat skjerming-forespørsel for innlogget bruker";

    private final ForesporselRepository foresporselRepository;
    private final SkjermetPersonRepository skjermetPersonRepository;
    private final NomRessurserService nomRessurserService;
    private final TokenUtil tokenUtil;
    private final Counter foresporselGodkjentCounter;
    private final Counter foresporselOpprettetCounter;


    @Unprotected
    @Transactional(value = "transactionManager")
    @GetMapping(FORESPORSEL_BEKREFTELSE)
    public String bekreftForesporsel(@RequestParam("id") final UUID funksjonellId) {
        val foresporsel = foresporselRepository.findByFunksjonellIdAndBekreftetFalse(funksjonellId).orElse(null);

        if (foresporsel == null) {
            log.error("Forsøk på bekreftelse av skjerming med ugyldig funksjonell id: {}", funksjonellId);
            return "Ugyldig operasjon";
        }

        val skjermet = skjermetPersonRepository.getSkjermetPersonByPersonident(foresporsel.getPersonident()).orElse(null);

        if (skjermet != null) {
            skjermet.setSkjermetTil(LocalDateTime.now());
            skjermet.setEndretDato(LocalDateTime.now());
            skjermet.setEndretAv(Kilde.BRUKER);
            skjermetPersonRepository.save(skjermet);
        }

        val ressurs = nomRessurserService.getInternRessursByNavident(foresporsel.getTilknyttetRessurs(), true);

        if (ressurs == null) {
            log.error("Forsøk på bekreftelse av skjerming med ugyldig nav-ident: {}", foresporsel.getTilknyttetRessurs());
            return "Ugyldig operasjon";
        }

        foresporselGodkjentCounter.increment();

        foresporsel.setBekreftet(true);
        skjermetPersonRepository.save(mapToSkjermetPerson(foresporsel, ressurs.getSluttdato()));
        foresporselRepository.delete(foresporsel);

        return "Bekreftelse av skjerming registrert";
    }

    @GetMapping
    public ForesporselResponseDTO hentForesporsel() {
        validerTilgang();

        val personident = tokenUtil.getSubject();
        val foresporsel = foresporselRepository.findByPersonidentAndBekreftetFalse(personident).orElse(null);

        return foresporsel != null ? ForesporselResponseDTO.fromEntity(foresporsel) : null;
    }

    @PostMapping
    public ResponseEntity<ForesporselResponseDTO> opprettForesporsel(@RequestBody final ForesporselRequestDTO foresporselRequestDTO) {
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
