package no.nav.nom.skjermede.personer.skjerming.api.v1;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.skjermede.personer.common.TokenUtil;
import no.nav.nom.skjermede.personer.common.exception.UnauthorizedOperationException;
import no.nav.nom.skjermede.personer.skjerming.SkjermetPerson;
import no.nav.nom.skjermede.personer.skjerming.SkjermetPersonRepository;
import no.nav.nom.skjermede.personer.skjerming.api.v1.dto.SkjermetPersonRequestDTO;
import no.nav.nom.skjermede.personer.skjerming.api.v1.dto.SkjermetPersonResponseDTO;
import no.nav.nom.skjermede.personer.skjerming.api.v1.exception.DuplicateSkjermingException;
import no.nav.nom.skjermede.personer.skjerming.api.v1.exception.SkjermingNotFoundException;
import no.nav.security.token.support.core.api.ProtectedWithClaims;
import no.nav.security.token.support.core.api.Unprotected;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@ProtectedWithClaims(issuer = "selvbetjening", claimMap = { "acr=Level4" })
@RequestMapping(SkjermingConstants.BASE_URL)
public class SkjermedePersonerController {

    private final SkjermetPersonRepository skjermetPersonRepository;
    private final TokenUtil tokenUtil;

    @GetMapping
    @Unprotected
    public List<SkjermetPersonResponseDTO> hentAlleSkjerminger() {
        return skjermetPersonRepository
                .findAll()
                .stream()
                .map(SkjermetPersonResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @GetMapping(SkjermingConstants.BY_NAVIDENT)
    public SkjermetPersonResponseDTO hentSkjerming(@PathVariable final String navident) {
        final SkjermetPerson skjermetPerson = skjermetPersonRepository
                .findDistinctByNavidentAndSkjermetTomNull(navident)
                .orElseThrow(() -> new SkjermingNotFoundException(String.format(SkjermingConstants.NOT_FOUND_MESSAGE, navident)));


        validerTilgang(skjermetPerson.getPersonident());

        return SkjermetPersonResponseDTO.fromEntity(skjermetPerson);
    }

    @GetMapping(SkjermingConstants.FAMILIEMEDLEMER)
    public List<SkjermetPersonResponseDTO> hentSkjermedeFamiliemedlemer() {
        final String personident = Optional
                .ofNullable(tokenUtil.getSubject())
                .orElseThrow(() -> new UnauthorizedOperationException(SkjermingConstants.UNAUTHORIZED_MESSAGE));

        validerTilgang(personident);

        return skjermetPersonRepository.findAllByTilknyttetRessurs(personident)
                .stream()
                .map(SkjermetPersonResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @GetMapping(SkjermingConstants.BY_ANSATT)
    public List<SkjermetPersonResponseDTO> hentSkjermingerForAnsatt() {
        final String personident = Optional
                .ofNullable(tokenUtil.getSubject())
                .orElseThrow(() -> new UnauthorizedOperationException(SkjermingConstants.UNAUTHORIZED_MESSAGE));

        validerTilgang(personident);

        return skjermetPersonRepository.findAllByPersonident(personident)
                .stream()
                .map(SkjermetPersonResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @PostMapping
    @Transactional
    public ResponseEntity<SkjermetPersonResponseDTO> opprettSkjerming(@RequestBody final SkjermetPersonRequestDTO skjermetPersonDto) {
        final String personident = Optional
                .ofNullable(tokenUtil.getSubject())
                .orElseThrow(() -> new UnauthorizedOperationException(SkjermingConstants.UNAUTHORIZED_MESSAGE));

        validerTilgang(personident);

        if (skjermetPersonRepository.existsByPersonidentAndSkjermetTomNull(personident)) {
            throw new DuplicateSkjermingException(SkjermingConstants.DUPLICATE_SKJERMING_MESSAGE);
        }

        final SkjermetPerson skjermetPerson = skjermetPersonDto.toEntity();
        skjermetPerson.setPersonident(personident);
        skjermetPerson.setOpprettetDato(LocalDateTime.now());
        skjermetPerson.setEndretDato(LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SkjermetPersonResponseDTO.fromEntity(skjermetPersonRepository.save(skjermetPerson)));
    }

    @Transactional
    @PutMapping(SkjermingConstants.BY_NAVIDENT)
    public SkjermetPersonResponseDTO oppdaterSkjerming(@PathVariable final String navident,
                                                       @RequestBody final SkjermetPersonRequestDTO skjermetPersonDTO) {
        final SkjermetPerson skjermetPerson = skjermetPersonRepository
                .findDistinctByNavidentAndSkjermetTomNull(navident)
                .orElseThrow(() -> new SkjermingNotFoundException(String.format(SkjermingConstants.NOT_FOUND_MESSAGE, navident)));

        validerTilgang(skjermetPerson.getPersonident());

        skjermetPerson.setSkjermetTom(skjermetPersonDTO.getSkjermetTom());
        skjermetPerson.setEndretDato(LocalDateTime.now());

        return SkjermetPersonResponseDTO.fromEntity(skjermetPersonRepository.save(skjermetPerson));
    }

    @Transactional
    @DeleteMapping(SkjermingConstants.BY_NAVIDENT)
    public void slettSkjerming(@PathVariable final String navident) {
        final SkjermetPerson skjermetPerson = skjermetPersonRepository
                .findDistinctByNavidentAndSkjermetTomNull(navident)
                .orElseThrow(() -> new SkjermingNotFoundException(String.format(SkjermingConstants.NOT_FOUND_MESSAGE, navident)));

        validerTilgang(skjermetPerson.getPersonident());

        skjermetPersonRepository.deleteById(skjermetPerson.getId());
    }

    private void validerTilgang(final String personident) {
        if (!tokenUtil.isAuthorized(personident)) {
            throw new UnauthorizedOperationException(SkjermingConstants.UNAUTHORIZED_MESSAGE);
        }
    }
}
