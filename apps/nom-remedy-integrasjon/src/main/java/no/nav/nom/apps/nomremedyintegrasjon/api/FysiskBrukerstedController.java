package no.nav.nom.apps.nomremedyintegrasjon.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = {"/api/fysiskebrukersteder"})
public class FysiskBrukerstedController {

    private final FysiskBrukerstedReadOnlyRepository fysiskBrukerstedReadOnlyRepository;

    public FysiskBrukerstedController(FysiskBrukerstedReadOnlyRepository fysiskBrukerstedReadOnlyRepository) {
        this.fysiskBrukerstedReadOnlyRepository = fysiskBrukerstedReadOnlyRepository;
    }

    @GetMapping
    public ResponseEntity<List<FysiskBrukersted>> getFysiskeBrukersteder() {

        return ResponseEntity.ok(fysiskBrukerstedReadOnlyRepository.findAll());
    }
}
