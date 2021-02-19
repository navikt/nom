package no.nav.nom.apps.nom.ressurser.api.v1;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.nom.ressurser.api.v1.dto.RessursRequestDTO;
import no.nav.nom.apps.nom.ressurser.api.v1.dto.RessursResponseDTO;
import no.nav.nom.apps.nom.ressurser.ressurser.RessursRepository;
import no.nav.security.token.support.core.api.Unprotected;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@Unprotected
@Profile("!prod")
@RestController
@RequestMapping(value = "/api/v1/ressursdata", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class RessursDataController {

    private final RessursRepository ressursRepository;

    private final static String DUPLIKAT_PERSONIDENT = "Duplikat ressurs - person med oppgitt personident allerede finnes";
    private final static String DUPLIKAT_NAVIDENT = "Duplikat ressurs - person med oppgitt navident allerede finnes";

    @PostMapping()
    @Transactional(value = "transactionManager")
    public ResponseEntity<RessursResponseDTO> createTestRessurs(@RequestBody RessursRequestDTO ressursDTO) {
        val ressurs = RessursRequestDTO.toEntity(ressursDTO);
        ressurs.setOpprettetDato(LocalDate.now());
        ressurs.setEndretDato(LocalDate.now());

        if (ressursRepository.findByPersonident(ressurs.getPersonident()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, DUPLIKAT_PERSONIDENT);
        }

        if (ressursRepository.findByNavident(ressursDTO.getNavident()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, DUPLIKAT_NAVIDENT);
        }

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(RessursResponseDTO.fromEntity(ressursRepository.save(ressurs)));

    }

    @GetMapping("/{personident}")
    public RessursResponseDTO hentByPersonident(@PathVariable String personident) {
        val ressurs = ressursRepository
                .findByPersonident(personident)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ressurs ikke funnet"));

        return RessursResponseDTO.fromEntity(ressurs);
    }
}
