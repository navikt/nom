package no.nav.nom.apps.api.rest.person;

import lombok.RequiredArgsConstructor;
import no.nav.nom.apps.api.graphql.PdlConsumer;
import no.nav.nom.contract.nom.api.graphql.Person;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/person")
public class PersonContoller {

    private final PdlConsumer pdlConsumer;


    @PostMapping("/getPerson")
    public PdlPersonDTO getPerson(@RequestBody PdlPersonRequest request){
        return pdlConsumer.hentPerson(request.getPersonident()).map(this::convert).orElse(null);
    }


    private PdlPersonDTO convert(Person person){
        return PdlPersonDTO.builder()
                .navn(
                        PdlPersonDTO.Navn.builder()
                                .fornavn(person.getNavn().getFornavn())
                                .mellomnavn(person.getNavn().getMellomnavn())
                                .etternavn(person.getNavn().getEtternavn())
                                .build()
                )
                .build();
    }
}
