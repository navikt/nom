package no.nav.nom.apps.api.graphql.ressurs;

import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.dvh.dto.DvhTelefon;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.api.graphql.Telefon;
import no.nav.nom.contract.nom.TelefonType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;


@Component
public class TelefonResolver {

    private final static Logger log = LoggerFactory.getLogger(TelefonResolver.class);
    private final DvhRepository dvhRepository;

    public TelefonResolver(DvhRepository dvhRepository) {
        this.dvhRepository = dvhRepository;
    }

    public Map<Ressurs, List<Telefon>> load(Collection<Ressurs> ressurser) {
        return ressurser.stream().collect(toMap(Function.identity(), this::telefon));
    }

    public List<Telefon> telefon(Ressurs ressurs) {
        List<DvhTelefon> tlfList = dvhRepository.getTelefonNrListByNavIdent(ressurs.getNavIdent());

        return tlfList.stream()
                .map(tlf -> telefonFromDvhTelefon(tlf, ressurs))
                .filter(Objects::nonNull)
                .toList();
    }

    private Telefon telefonFromDvhTelefon(DvhTelefon it, Ressurs ressurs) {
        if (it == null) {
            log.warn("DvhTelefon var <null> for ident " + ressurs.getNavIdent());
            return null;
        }
        TelefonType telefonType;
        switch (it.getType()) {
            case TJENESTETELEFON -> telefonType = TelefonType.NAV_TJENESTE_TELEFON;
            case KONTORTELEFON -> telefonType = TelefonType.NAV_KONTOR_TELEFON;
            default -> {
                log.warn("Setter nr til privattelefon for " + it.getNavIdent());
                telefonType = TelefonType.PRIVAT_TELEFON;
            }
        }
        return new Telefon(it.getNummer(), telefonType, "");
    }
}
