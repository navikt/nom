package no.nav.nom.apps.api.orgenhet.eventsourcing.producer;

import no.nav.nom.apps.api.orgenhet.eventsourcing.state.OrgEnhetStateRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import static java.util.function.Predicate.not;

@Component
public class OrgEnhetIdGenerator {
    private final Random random = new Random();
    private final OrgEnhetStateRepository orgEnhetStateService;

    public OrgEnhetIdGenerator(OrgEnhetStateRepository orgEnhetStateService) {
        this.orgEnhetStateService = orgEnhetStateService;
    }

    List<String> letter1 = List.of("b", "d", "f", "g", "h", "j", "k", "m", "n", "p", "r", "s", "t", "v");
    List<String> letter2 = List.of("a", "e", "u", "y");
    List<String> letter3 = List.of("a", "b", "c", "d", "e", "f", "g", "h", "j", "k", "m", "n", "p", "r", "s", "t", "u", "v", "w", "x", "y", "z");

    /*
    Målet her er å lage en lesbar id, som er relativt lett å memorisere og gjengi muntlig.
    2 bokstaver (konsonant + vokal) + 3 sifre + en bokstav.
    Og utelatt bokstaver som kan forveksles eller.
     */
    public String newId() {
        String id = letter(letter1)+letter(letter2)+digits(3)+letter(letter3);
        if (orgEnhetStateService.findById(id).isPresent()) {
            id = newId();
        }
        return id;
    }

    private String letter(List<String> letters) {
        return letters.get(random.nextInt(0, letters.size()));
    }

    private String digits(int num) {
        int lower = Double.valueOf(Math.pow(10,num-1)).intValue();
        int upper = Double.valueOf(Math.pow(10,num)).intValue();
        int rand = random.nextInt(lower, upper);
        return String.valueOf(random.nextInt(lower, upper));
    }
}
