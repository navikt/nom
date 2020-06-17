package no.nav.nom.skjermede.personer.skjerming.api.v1;

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
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(SkjermedePersonerController.BASE_URL)
@ProtectedWithClaims(issuer = "selvbetjening", claimMap = { "acr=Level4" })
public class SkjermedePersonerController {

    public static final String BASE_URL = "/api/v1/skjerming";
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
    public List<SkjermetPersonResponseDTO> hentAlle() {
        return skjermetPersonRepository
                .findAll()
                .stream()
                .map(SkjermetPersonResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @GetMapping
    public SkjermetPersonResponseDTO hentSkjerming() {
        validerTilgang();

        val personident = tokenUtil.getSubject();
        val skjermetPerson = skjermetPersonRepository.getSkjermetPersonByPersonident(personident).orElse(null);

        if (skjermetPerson != null && skjermetPerson.getTilknyttetRessurs() != null && Arrays.stream(Unntakskode.values()).noneMatch(kode -> kode.toString().equals(skjermetPerson.getTilknyttetRessurs()))) {
            val ressurs = nomRessurserService.getInternRessursByNavident(skjermetPerson.getTilknyttetRessurs(), false);

            return SkjermetPersonResponseDTO.fromEntityAndRessurs(skjermetPerson, ressurs);
        }

        return skjermetPerson != null ? SkjermetPersonResponseDTO.fromEntity(skjermetPerson) : null;
    }

    @GetMapping(FAMILIEMEDLEMMER)
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
    public List<SkjermetPersonResponseDTO> hentAlleSkjerminger() {
        validerTilgang();

        val personident = tokenUtil.getSubject();

        return skjermetPersonRepository.findAllByPersonident(personident)
                .stream()
                .map(SkjermetPersonResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @PostMapping
    @Transactional(value = "transactionManager")
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

    @Transactional(value = "transactionManager")
    @PutMapping
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

    @Transactional(value = "transactionManager")
    @DeleteMapping
    public void slettSkjerming() {
        validerTilgang();

        val personident = tokenUtil.getSubject();
        val skjermetPerson = skjermetPersonRepository
                .getSkjermetPersonByPersonident(personident)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, NOT_FOUND_MESSAGE));

        skjermetPersonRepository.deleteById(skjermetPerson.getId());
    }

    private void validerTilgang() {
        if (!tokenUtil.isAuthorized()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, UNAUTHORIZED_MESSAGE);
        }
    }
}
