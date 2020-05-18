package no.nav.nom.skjermede.personer.skjerming.api.v1;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.contract.nom.dto.RessursDTO;
import no.nav.nom.skjermede.personer.common.TokenUtil;
import no.nav.nom.skjermede.personer.skjerming.SkjermetPerson;
import no.nav.nom.skjermede.personer.skjerming.SkjermetPersonRepository;
import no.nav.nom.skjermede.personer.skjerming.api.v1.dto.SkjermetPersonResponseDTO;
import no.nav.security.token.support.core.api.ProtectedWithClaims;
import no.nav.security.token.support.core.api.Unprotected;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(SkjermingConstants.BASE_URL)
@ProtectedWithClaims(issuer = "selvbetjening", claimMap = { "acr=Level4" })
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
        val skjermetPerson = skjermetPersonRepository.getSkjermetPersonByPersonident(personident).orElse(null);

        if (skjermetPerson != null && skjermetPerson.getTilknyttetRessurs() != null) {
            HttpHeaders headers = new HttpHeaders();
            headers.set("navident", skjermetPerson.getTilknyttetRessurs());

            val ressurs = restTemplate.exchange(
                    String.format("%s/ansatt?gyldig=false&ressursType=INTERN", ressursAPI),
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    RessursDTO.class).getBody();

            return SkjermetPersonResponseDTO.fromEntityAndRessurs(skjermetPerson, ressurs);
        }

        return skjermetPerson != null ? SkjermetPersonResponseDTO.fromEntity(skjermetPerson) : null;
    }

    @GetMapping(SkjermingConstants.FAMILIEMEDLEMMER)
    public List<SkjermetPersonResponseDTO> hentSkjermedeFamiliemedlemer() {
        validerTilgang();

        val personident = tokenUtil.getSubject();

        HttpHeaders headers = new HttpHeaders();
        headers.set("personident", personident);
        val ressurs = restTemplate.exchange(
                String.format("%s/ansatt?gyldig=false&ressursType=INTERN", ressursAPI),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                RessursDTO.class).getBody();

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
    @Transactional(value = "transactionManager")
    public ResponseEntity<SkjermetPersonResponseDTO> opprettSkjerming() {
        validerTilgang();

        val personident = tokenUtil.getSubject();

        if (skjermetPersonRepository.getSkjermetPersonByPersonident(personident).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, SkjermingConstants.DUPLICATE_SKJERMING_MESSAGE);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("personident", personident);
        val ressurs = restTemplate.exchange(
                String.format("%s/ansatt?gyldig=true&ressursType=INTERN", ressursAPI),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                RessursDTO.class).getBody();

        val skjermetPerson = SkjermetPerson.builder()
                .personident(personident)
                .fornavn(ressurs.getFornavn())
                .etternavn(ressurs.getEtternavn())
                .skjermetFra(LocalDateTime.now())
                .opprettetDato(LocalDateTime.now())
                .endretDato(LocalDateTime.now())
                .kafkaProduced(false)
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, SkjermingConstants.NOT_FOUND_MESSAGE));

        skjermetPerson.setSkjermetTil(LocalDateTime.now());
        skjermetPerson.setEndretDato(LocalDateTime.now());
        skjermetPerson.setKafkaProduced(false);

        return SkjermetPersonResponseDTO.fromEntity(skjermetPersonRepository.save(skjermetPerson));
    }

    @Transactional(value = "transactionManager")
    @DeleteMapping
    public void slettSkjerming() {
        validerTilgang();

        val personident = tokenUtil.getSubject();
        val skjermetPerson = skjermetPersonRepository
                .getSkjermetPersonByPersonident(personident)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, SkjermingConstants.NOT_FOUND_MESSAGE));

        skjermetPersonRepository.deleteById(skjermetPerson.getId());
    }

    private void validerTilgang() {
        if (!tokenUtil.isAuthorized()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, SkjermingConstants.UNAUTHORIZED_MESSAGE);
        }
    }
}
