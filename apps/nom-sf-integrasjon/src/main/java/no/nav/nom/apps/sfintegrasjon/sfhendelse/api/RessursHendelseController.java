package no.nav.nom.apps.sfintegrasjon.sfhendelse.api;

import no.nav.nom.apps.sfintegrasjon.sfhendelse.repository.RessursHendelse;
import no.nav.nom.apps.sfintegrasjon.sfhendelse.repository.RessursHendelseRepository;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@PreAuthorize("hasAnyAuthority('ZALARIS', 'ADMIN')")
@RequestMapping(value = RessursHendelseController.BASE_URL)
public class RessursHendelseController {

    public static final String BASE_URL = "/api/v1/zalaris/ressurshendelse";

    private final RessursHendelseRepository hendelseRepository;
    private final ObjectMapper objectMapper;

    public RessursHendelseController(RessursHendelseRepository hendelseRepository, ObjectMapper objectMapper) {
        this.hendelseRepository = hendelseRepository;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<Void> registrerHendelse(@RequestBody String sfHendelse) {

        var ressursHendelse = from(sfHendelse);
        hendelseRepository.save(ressursHendelse);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    private RessursHendelse from(String hendelse) {
        var jsonNode = objectMapper.readTree(hendelse);
        var sporingsId = jsonNode.path("sporingsId").asString();
        var endretAv = jsonNode.path("endretAv").toString();
        var hendelseType = jsonNode.path("endringshendelser").toString();
        var ressurs = jsonNode.path("ressurs").toString();
        var navident = jsonNode.path("ressurs").path("navident").asString();

        if (ressurs.isEmpty() || navident.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ressurs node incoplete");
        }

        return RessursHendelse.builder()
            .sporingsId(Optional.ofNullable(sporingsId).orElse(""))
            .endretAv(Optional.ofNullable(endretAv).orElse(""))
            .mottattTidspunkt(LocalDateTime.now())
            .hendelseType(Optional.ofNullable(hendelseType).orElse(""))
            .navident(navident)
            .data(ressurs)
            .build();
    }
}
