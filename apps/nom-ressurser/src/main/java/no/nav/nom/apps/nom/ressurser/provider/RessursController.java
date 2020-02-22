package no.nav.nom.apps.nom.ressurser.provider;

import lombok.RequiredArgsConstructor;
import no.nav.nom.apps.nom.ressurser.domain.Ressurs;
import no.nav.nom.apps.nom.ressurser.provider.dto.RessursRequest;
import no.nav.nom.apps.nom.ressurser.repository.RessursRepository;
import no.nav.nom.apps.nom.ressurser.service.RessursService;
import org.springframework.web.bind.annotation.*;


import java.time.LocalDate;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/api/v1/ressurs", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class RessursController {

    private final RessursService service;
    private final RessursRepository repository;

    @GetMapping
    public List<Ressurs> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public Ressurs get(@PathVariable Long id) {
        return repository.getOne(id);
    }

    @PostMapping
    public Ressurs createOrEdit(@RequestBody RessursRequest ressurs) {
        return service.saveAndSend(converted(ressurs));
    }

    private static Ressurs converted(RessursRequest ressursRequest){
        return Ressurs.builder()
                .personident(ressursRequest.getPersonident())
                .ressurstype(ressursRequest.getRessurstype())
                .startdato(LocalDate.parse(ressursRequest.getStartdato()))
                .sluttdato(ressursRequest.getSluttdato() == null ? null : LocalDate.parse(ressursRequest.getSluttdato()))
                .build();
    }
}
