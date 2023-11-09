package no.nav.nom.apps.nomremedyintegrasjon.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping(value = {"/api/fysiskebrukersteder"})
public class FysiskBrukerstedController {

    @GetMapping
    public ResponseEntity<List<String>> getFysiskeBrukersteder() {
        return ResponseEntity.ok(Collections.emptyList());
    }
}
