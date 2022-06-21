package no.nav.nom.apps.navident.idgenerator;

import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class NavIdGeneratorImpl implements NavIdGenerator {
    Random rng = new Random();

    @Override
    public String generate(char prefix) {
        // TODO: implement me
        return prefix + String.valueOf(rng.nextInt(120000, 999999));
    }
}
