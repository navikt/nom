package no.nav.nom.apps.api.telefon;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.dvh.dto.DvhTelefon;
import no.nav.nom.apps.api.ressurs.api.exceptions.RessursNotFoundException;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursCommandService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterJobbTelefonCommand;
import no.nav.nom.contract.nom.TelefonType;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Jobbtelefon;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class TelefonService {

    private final RessursStateRepository ressursStateRepository;
    private final DvhRepository dvhRepository;
    private final RessursCommandService ressursCommandService;


    public TelefonService(RessursStateRepository ressursStateRepository, DvhRepository dvhRepository, RessursCommandService ressursCommandService) {
        this.ressursStateRepository = ressursStateRepository;
        this.dvhRepository = dvhRepository;
        this.ressursCommandService = ressursCommandService;
    }

    // Listen med telefonnummer for hver navident er sortert med høyest prioriterte nummer først 
    public Map<String, List<Telefon>> hentPrioriterteTelefonnummer() {
        var navTelefoner = fetchNavTelefoner();
        var privattelefoner = fetchPrivattelefoner();

        Map<String, List<Telefon>> sorted = new HashMap<>();
        Stream.concat(navTelefoner.stream(), privattelefoner.stream())
                .collect(Collectors.groupingBy(
                                Telefon::navident,
                                Collectors.toList()
                        )
                ).forEach((navident, telefoner) -> {
                            telefoner.sort(telefonComparator());
                            sorted.put(navident, telefoner);
                        }
                );

        return sorted;
    }

    public List<Telefon> fetchNavTelefoner() {
        return dvhRepository.getNavidentTelefonNrMap().stream()
                .filter(Objects::nonNull)
                .map(this::toTelefon)
                .toList();
    }

    public List<Telefon> fetchPrivattelefoner() {
        return ressursStateRepository.findAllActiveRessurser()
                .values().stream()
                .filter(ressurs -> ressurs.primaryTelefonnummer().isPresent())
                .map(r -> new Telefon(r.navident(), r.primaryTelefonnummer().get(), TelefonType.PRIVAT_TELEFON))
                .toList();
    }

    public String getPrioritertTelefonnummer(String navident) {
        List<Telefon> telefoner = getAlleTelefonnummer(navident);
        return telefoner.isEmpty() ? null : telefoner.getFirst().nummer();
    }

    public Jobbtelefon getRessursStateTelefonnummer(String navident) {
        return findRessursStateByNavident(navident).jobbtelefon();
    }

    public List<Telefon> getAlleTelefonnummer(String navident) {
        List<Telefon> telefoner = dvhRepository.getTelefonNrListByNavIdent(navident)
                .stream()
                .filter(Objects::nonNull)
                .map(tlf -> new Telefon(navident, tlf.getNummer(), mapType(tlf.getType())))
                .toList();

        Optional<Telefon> privatNummer = findRessursStateByNavident(navident)
                .primaryTelefonnummer()
                .flatMap(
                        p -> Optional.of(new Telefon(navident, p, TelefonType.PRIVAT_TELEFON))
                );

        return Stream.concat(telefoner.stream(), privatNummer.stream())
                .sorted(telefonComparator())
                .toList();
    }


    /*
        Sorteringsrekkefølge:
        1. NAV_TJENESTE_TELEFON
        2. NAV_KONTOR_TELEFON
        3. PRIVAT_TELEFON
     */
    private static Comparator<Telefon> telefonComparator() {
        return (o1, o2) -> {
            if (o1.type().equals(o2.type())) return 0;
            if (o1.type().equals(TelefonType.NAV_TJENESTE_TELEFON)) return -1;
            if (o2.type().equals(TelefonType.NAV_TJENESTE_TELEFON)) return 1;
            if (o1.type().equals(TelefonType.NAV_KONTOR_TELEFON)) return -1;
            if (o2.type().equals(TelefonType.NAV_KONTOR_TELEFON)) return 1;
            if (o1.type().equals(TelefonType.PRIVAT_TELEFON)) return -1;
            if (o2.type().equals(TelefonType.PRIVAT_TELEFON)) return 1;
            return 0;
        };
    }

    private TelefonType mapType(DvhTelefon.DvhTelefonType dvhTelefonType) {
        return switch (dvhTelefonType) {
            case TJENESTETELEFON -> TelefonType.NAV_TJENESTE_TELEFON;
            case KONTORTELEFON -> TelefonType.NAV_KONTOR_TELEFON;
        };
    }

    private Telefon toTelefon(DvhTelefon dvhTelefon) {
        return new Telefon(dvhTelefon.getNavIdent(), dvhTelefon.getNummer(), mapType(dvhTelefon.getType()));
    }

    private RessursState findRessursStateByNavident(String navident) {
        return ressursStateRepository.findByNavident(navident).orElseThrow(() -> RessursNotFoundException.navident(navident));
    }

    public void oppdaterRessursStateJobbtelefonBulk(List<DvhTelefon> dvhTelefonerMedEndring) {
        Origination origination = new Origination("nom-api", "nom-api");
        InputMetadata inputMetadata = new InputMetadata("nom-api BatchJob", "Datavarehus telefonoppdatering", null, origination);
        dvhTelefonerMedEndring.forEach(dvhTelefon -> {
            RessursState ressursState = findRessursStateByNavident(dvhTelefon.getNavIdent());
            inputMetadata.toBuilder().inputInterfaceMethodArguments(dvhTelefon).build();
            ressursCommandService.oppdaterJobbtelefonEvent(new OppdaterJobbTelefonCommand(
                    ressursState.fid(),
                    ressursState.navident(),
                    new Jobbtelefon(dvhTelefon.getNummer(),
                    mapType(dvhTelefon.getType()))
            ), inputMetadata);
        });
    }
}

