package no.nav.nom.skjermede.personer.foresporsel;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.contract.nom.dto.RessursDTO;
import no.nav.nom.skjermede.personer.common.TokenUtil;
import no.nav.nom.skjermede.personer.foresporsel.dto.ForesporselRequestDTO;
import no.nav.nom.skjermede.personer.foresporsel.dto.ForesporselResponseDTO;
import no.nav.nom.skjermede.personer.skjerming.Kilder;
import no.nav.nom.skjermede.personer.skjerming.SkjermetPerson;
import no.nav.nom.skjermede.personer.skjerming.SkjermetPersonRepository;
import no.nav.security.token.support.core.api.ProtectedWithClaims;
import no.nav.security.token.support.core.api.Unprotected;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(ForesporselConstants.BASE_URL)
@ProtectedWithClaims(issuer = "selvbetjening", claimMap = { "acr=Level4" })
public class ForesporselController {

    @Value("${ressurser.api}")
    private String ressursAPI;

    @Value("${maanderPlusSkjerming}")
    private int mnderSkjermetPlus;

    private final ForesporselRepository foresporselRepository;
    private final SkjermetPersonRepository skjermetPersonRepository;
    private final TokenUtil tokenUtil;
    private final RestTemplate restTemplate;
    private final MeterRegistry meterRegistry;


    @Unprotected
    @Transactional(value = "transactionManager")
    @GetMapping(ForesporselConstants.FORESPORSEL_BEKREFTELSE)
    public String bekreftForesporsel(@RequestParam("id") final UUID funksjonellId) {
        val foresporsel = foresporselRepository
                .findByFunksjonellIdAndBekreftetFalse(funksjonellId)
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
            skjermet.setEndretAv(Kilder.BRUKER);
            skjermetPersonRepository.save(skjermet);
            // Trenger ikke sette kafka produced til false. Hvis endringer på 2 forskjellige rader skjer med lik
            // timestamp i "endret" kolonnen, vil det kunne oppstå feil. Vil overskrives av ny skjerming på kafka uansett.
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("navident", foresporsel.getTilknyttetRessurs());

        val ressurs = restTemplate.exchange(
                String.format("%s/ansatt?gyldig=true&ressursType=INTERN", ressursAPI),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                RessursDTO.class).getBody();

        if (ressurs == null) {
            log.error("Forsøk på bekreftelse av skjerming med ugyldig nav-ident: {}", foresporsel.getTilknyttetRessurs());
            return "Ugyldig operasjon";
        }

        meterRegistry.counter("foresporsel_count", "status", "godkjent").increment();

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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ForesporselConstants.DUPLICATE_FORESPORSEL_MESSAGE);
        }

        val foresporsel = foresporselRequestDTO.toEntity();
        foresporsel.setPersonident(personident);
        foresporsel.setFunksjonellId(UUID.randomUUID());
        foresporsel.setBekreftet(false);
        foresporsel.setOpprettetDato(LocalDateTime.now());

        meterRegistry.counter("foresporsel_count", "status", "opprettet").increment();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ForesporselResponseDTO.fromEntity(foresporselRepository.save(foresporsel)));
    }

    private void validerTilgang() {
        if (!tokenUtil.isAuthorized()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, ForesporselConstants.UNAUTHORIZED_MESSAGE);
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
                .endretAv(Kilder.BRUKER)
                .opprettetAv(Kilder.BRUKER)
                .build();
    }
}
