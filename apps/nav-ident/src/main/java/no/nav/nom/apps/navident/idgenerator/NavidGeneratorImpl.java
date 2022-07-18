package no.nav.nom.apps.navident.idgenerator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NavidGeneratorImpl implements NavidGenerator {

    private final NavidRepository navidRepository;

    @Override
    public String generate(char prefix) {
        return prefix + String.valueOf(navidRepository.getNextval());
    }
}
