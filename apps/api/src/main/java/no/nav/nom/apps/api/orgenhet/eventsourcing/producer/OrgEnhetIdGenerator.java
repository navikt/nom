package no.nav.nom.apps.api.orgenhet.eventsourcing.producer;

import no.nav.nom.apps.api.orgenhet.OrgEnhetStateService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import static java.util.function.Predicate.not;

@Component
public class OrgEnhetIdGenerator {
    private final Random random = new Random();
    private final OrgEnhetStateService orgEnhetStateService;

    public OrgEnhetIdGenerator(OrgEnhetStateService orgEnhetStateService) {
        this.orgEnhetStateService = orgEnhetStateService;
    }

    List<String> letters = IntStream.rangeClosed('a', 'z')
            .mapToObj(c -> String.valueOf((char)c))
            .filter(not("iol"::contains))
            .toList();

    public String newId() {
        String id = letter()+letter()+digits(3)+letter();
        if (orgEnhetStateService.findById(id).isPresent()) {
            id = newId();
        }
        return id;
    }

    private String letter() {
        return letters.get(random.nextInt(0, letters.size()));
    }

    private String digits(int num) {
        int lower = Double.valueOf(Math.pow(10,num-1)).intValue();
        int upper = Double.valueOf(Math.pow(10,num)).intValue();
        return String.valueOf(random.nextInt(lower, upper));
    }
}
