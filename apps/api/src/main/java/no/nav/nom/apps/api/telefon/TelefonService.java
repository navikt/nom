package no.nav.nom.apps.api.telefon;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.dvh.dto.DvhTelefon;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursCommandService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterJobbTelefonCommand;
import no.nav.nom.contract.nom.TelefonType;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Jobbtelefon;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.apache.commons.lang3.StringUtils;
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

    public List<Telefon> hentTelefonnummreForAktiveRessurser() {
        return ressursStateRepository.findAllActiveRessurser().values()
            .stream().filter(ressurs -> ressurs.jobbtelefon() != null && StringUtils.trim(ressurs.jobbtelefon().nummer()) != null)
            .map(ressurs -> new Telefon(ressurs.navident(), ressurs.jobbtelefon().nummer(), ressurs.jobbtelefon().type()))
            .toList();
    }

    public List<Telefon> fetchNavTelefoner() {
        return dvhRepository.getNavidentTelefonNrList().stream()
                .filter(Objects::nonNull)
                .map(DvhTelefon::toTelefon)
                .toList();
    }

    public List<Telefon> fetchPrivattelefoner() {
        return ressursStateRepository.findAllActiveRessurser()
                .values().stream()
                .filter(ressurs -> ressurs.primaryTelefonnummer() != null && !ressurs.primaryTelefonnummer().isBlank())
                .map(r -> new Telefon(r.navident(), r.primaryTelefonnummer(), TelefonType.PRIVAT_TELEFON))
                .toList();
    }

    public String getPrioritertTelefonnummer(String navident) {
        List<Telefon> telefoner = getAlleTelefonnummer(navident);
        return telefoner.isEmpty() ? null : telefoner.getFirst().nummer();
    }

    public List<Telefon> getAlleTelefonnummer(String navident) {
        List<Telefon> telefoner = dvhRepository.getTelefonNrListByNavIdent(navident)
                .stream()
                .filter(Objects::nonNull)
                .map(DvhTelefon::toTelefon)
                .toList();

        Optional<Telefon> privatNummer = findRessursStateByNavident(navident).map(RessursState::primaryTelefonnummer)
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
    public static Comparator<Telefon> telefonComparator() {
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

    public Optional<RessursState> findRessursStateByNavident(String navident) {
        return ressursStateRepository.findByNavident(navident);
    }

    public Map<String, Telefon> getExistingTelefoner() {
        return ressursStateRepository.getAll().stream()
            .filter(ressurs -> ressurs.jobbtelefon() != null && ressurs.navident() != null)
            .collect(Collectors.toMap(RessursState::fid, ressurs -> new Telefon(ressurs.navident(), ressurs.jobbtelefon().nummer(), ressurs.jobbtelefon().type())));
    }

    public void oppdaterRessursStateJobbtelefon(String fid, String navident, Jobbtelefon telefon) {
        Origination origination = new Origination("nom-api", "nom-api");
        InputMetadata inputMetadata = new InputMetadata("nom-api BatchJob", "Datavarehus telefonoppdatering", null, origination);
        if (telefon != null) inputMetadata.toBuilder().inputInterfaceMethodArguments(telefon).build();

        ressursCommandService.oppdaterJobbtelefonEvent(new OppdaterJobbTelefonCommand(
            fid,
            navident,
            telefon
        ), inputMetadata);
    }
}

