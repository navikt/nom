package no.nav.nom.skjermede.personer.skjerming.api.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.skjermede.personer.skjerming.Kilde;
import no.nav.nom.skjermede.personer.skjerming.SkjermetPerson;
import no.nav.nom.skjermede.personer.skjerming.SkjermetPersonRepository;
import no.nav.nom.skjermede.personer.skjerming.api.v1.dto.SkjermetPersonDataRequestDTO;
import no.nav.nom.skjermede.personer.skjerming.api.v1.dto.SkjermetPersonDataResponseDTO;
import no.nav.security.token.support.core.api.ProtectedWithClaims;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(SkjermedePersonerDataController.BASE_URL)
@ProtectedWithClaims(issuer = "reststs", claimMap = { "acr=Level4" })
public class SkjermedePersonerDataController {

    public static final String BASE_URL = "/api/v1/skjermingdata";
    public static final String DELETE_SKJERMING = "{personident}";
    public static final String AVSKJERM = "opphor/{personident}";
    public static final String UKJENT_PERSONIDENT = "Ukjent personident";

    private final SkjermetPersonRepository skjermetPersonRepository;
    private final ObjectMapper objectMapper;

    @PostMapping
    @Transactional(value = "transactionManager")
    public ResponseEntity<List<SkjermetPersonDataResponseDTO>> opprettSkjerminger(@RequestBody final List<SkjermetPersonDataRequestDTO> data) {
        val skjerminger = data.stream().
                map(dto -> {
                    SkjermetPerson entity = dto.toEntity();
                    entity.setOpprettetAv(Kilde.SERVICEBRUKER);
                    entity.setOpprettetDato(LocalDateTime.now());
                    entity.setEndretDato(LocalDateTime.now());
                    entity.setEndretAv(Kilde.SERVICEBRUKER);
                    return entity;
                }).collect(Collectors.toList());
        val lagredeSkjerminger = skjermetPersonRepository
                .saveAll(skjerminger)
                .stream()
                .map(SkjermetPersonDataResponseDTO::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.CREATED).body(lagredeSkjerminger);
    }

    @DeleteMapping(DELETE_SKJERMING)
    @Transactional(value = "transactionManager")
    public void slettSkjerming(@PathVariable final String personident) {
        skjermetPersonRepository.deleteAll(skjermetPersonRepository.findAllByPersonident(personident));
    }

    @PutMapping(AVSKJERM)
    public void opphorSkjerming(@PathVariable final String personident) {
        val skjermetPerson = skjermetPersonRepository
                .getSkjermetPersonByPersonident(personident)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, UKJENT_PERSONIDENT));

        skjermetPerson.setSkjermetTil(LocalDateTime.now());
        skjermetPersonRepository.save(skjermetPerson);
    }
}
