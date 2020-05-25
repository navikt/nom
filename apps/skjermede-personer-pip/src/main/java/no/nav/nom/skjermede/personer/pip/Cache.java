package no.nav.nom.skjermede.personer.pip;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
@AllArgsConstructor
@Slf4j
public class Cache {

    static private final HashMap<String, Skjermingsperiode> skjermedePersoner = new HashMap<>();

    boolean isSkjermet(String personident) {
        val periode = skjermedePersoner.get(personident);

        return (periode != null) &&
                (periode.fra().isBefore(LocalDateTime.now())) &&
                (periode.til() == null || periode.til().isAfter(LocalDateTime.now()));
    }

    HashMap<String, Skjermingsperiode> getAll() {
        return skjermedePersoner;
    }

    int size() {
        return skjermedePersoner.size();
    }

    void store(String personident, Skjermingsperiode periode) {
        if(periode.til() == null || periode.til().isAfter(LocalDateTime.now())) {
            skjermedePersoner.put(personident, periode);
        }
    }

}
