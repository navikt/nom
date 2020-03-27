package no.nav.nom.skjermede.personer.skjerming.api.v1;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.contract.nom.dto.RessursDto;
import no.nav.nom.skjermede.personer.common.TokenUtil;
import no.nav.nom.skjermede.personer.common.exception.UnauthorizedOperationException;
import no.nav.nom.skjermede.personer.skjerming.SkjermetPerson;
import no.nav.nom.skjermede.personer.skjerming.SkjermetPersonRepository;
import no.nav.nom.skjermede.personer.skjerming.api.v1.dto.SkjermetPersonResponseDTO;
import no.nav.nom.skjermede.personer.skjerming.api.v1.exception.DuplicateSkjermingException;
import no.nav.nom.skjermede.personer.skjerming.api.v1.exception.SkjermingNotFoundException;
import no.nav.security.token.support.core.api.ProtectedWithClaims;
import no.nav.security.token.support.core.api.Unprotected;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@ProtectedWithClaims(issuer = "selvbetjening", claimMap = { "acr=Level4" })
@RequestMapping(SkjermingConstants.BASE_URL)
public class SkjermedePersonerController {

    @Value("${ressurser.api}")
    private String ressursAPI;

    private final RestTemplate restTemplate;
    private final SkjermetPersonRepository skjermetPersonRepository;
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
        val skjermetPerson = skjermetPersonRepository
                .getSkjermetPersonByPersonident(personident)
                .orElseThrow(() -> new SkjermingNotFoundException(String.format(SkjermingConstants.NOT_FOUND_MESSAGE, personident)));

        return SkjermetPersonResponseDTO.fromEntity(skjermetPerson);
    }

    @GetMapping(SkjermingConstants.FAMILIEMEDLEMMER)
    public List<SkjermetPersonResponseDTO> hentSkjermedeFamiliemedlemer() {
        validerTilgang();

        val personident = tokenUtil.getSubject();

        val ressurs = restTemplate.getForObject(
                String.format("%s/personident/%s?ressursType=INTERN", ressursAPI, personident),
                RessursDto.class);

        return skjermetPersonRepository.findAllByTilknyttetRessurs(ressurs.getNavident())
                .stream()
                .map(SkjermetPersonResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @GetMapping(SkjermingConstants.ALLE)
    public List<SkjermetPersonResponseDTO> hentAlleSkjerminger() {
        validerTilgang();

        val personident = tokenUtil.getSubject();

        return skjermetPersonRepository.findAllByPersonident(personident)
                .stream()
                .map(SkjermetPersonResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @PostMapping
    @Transactional
    public ResponseEntity<SkjermetPersonResponseDTO> opprettSkjerming() {
        validerTilgang();

        val personident = tokenUtil.getSubject();

        if (skjermetPersonRepository.getSkjermetPersonByPersonident(personident).isPresent()) {
            throw new DuplicateSkjermingException(SkjermingConstants.DUPLICATE_SKJERMING_MESSAGE);
        }

        val ressurs = restTemplate.getForObject(
                String.format("%s/personident/%s?ressursType=INTERN", ressursAPI, personident),
                RessursDto.class);

        val skjermetPerson = SkjermetPerson.builder()
                .personident(personident)
                .fornavn(ressurs.getFornavn())
                .etternavn(ressurs.getEtternavn())
                .skjermetFom(LocalDate.now())
                .opprettetDato(LocalDateTime.now())
                .endretDato(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SkjermetPersonResponseDTO.fromEntity(skjermetPersonRepository.save(skjermetPerson)));
    }

    @Transactional
    @PutMapping
    public SkjermetPersonResponseDTO opphorSkjerming() {
        validerTilgang();

        val personident = tokenUtil.getSubject();
        val skjermetPerson = skjermetPersonRepository
                .getSkjermetPersonByPersonident(personident)
                .orElseThrow(() -> new SkjermingNotFoundException(String.format(SkjermingConstants.NOT_FOUND_MESSAGE)));

        skjermetPerson.setSkjermetTom(LocalDate.now());
        skjermetPerson.setEndretDato(LocalDateTime.now());

        return SkjermetPersonResponseDTO.fromEntity(skjermetPersonRepository.save(skjermetPerson));
    }

    @Transactional
    @DeleteMapping
    public void slettSkjerming() {
        validerTilgang();

        val personident = tokenUtil.getSubject();
        val skjermetPerson = skjermetPersonRepository
                .getSkjermetPersonByPersonident(personident)
                .orElseThrow(() -> new SkjermingNotFoundException(String.format(SkjermingConstants.NOT_FOUND_MESSAGE, personident)));

        skjermetPersonRepository.deleteById(skjermetPerson.getId());
    }

    private void validerTilgang() {
        if (!tokenUtil.isAuthorized()) {
            throw new UnauthorizedOperationException(SkjermingConstants.UNAUTHORIZED_MESSAGE);
        }
    }
}
