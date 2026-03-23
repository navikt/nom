package no.nav.nom.apps.api.telefon;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursCommandService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterJobbTelefonCommand;
import no.nav.nom.contract.nom.TelefonType;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Jobbtelefon;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.trim;

@Slf4j
@Service
public class TelefonService {

    private final RessursStateRepository ressursStateRepository;
    private final RessursCommandService ressursCommandService;


    public TelefonService(RessursStateRepository ressursStateRepository, RessursCommandService ressursCommandService) {
        this.ressursStateRepository = ressursStateRepository;
        this.ressursCommandService = ressursCommandService;
    }

    public List<Telefon> hentTelefonnummreForAktiveRessurser() {
        var ressurser = ressursStateRepository.findAllActiveRessurser().values();
        var jobbtelefoner = ressurser.stream()
            .filter(ressursState -> nonNull(ressursState.jobbtelefon()) && nonNull(trim(ressursState.jobbtelefon().nummer())))
            .map(ressursState -> new Telefon(ressursState.navident(), ressursState.jobbtelefon().nummer(), ressursState.jobbtelefon().type()));
        var privattelefoner = ressurser.stream()
            .filter(ressursState -> isNotBlank(ressursState.primaryTelefonnummer()))
            .map(ressursState -> new Telefon(ressursState.navident(), ressursState.primaryTelefonnummer(), TelefonType.PRIVAT_TELEFON));

        return Stream.concat(privattelefoner, jobbtelefoner).toList();
    }

    public String getPrioritertTelefonnummer(String navident) {
        var ressursState = ressursStateRepository.findByNavident(navident);
        var jobbtelefon = ressursState.map(RessursState::jobbtelefon).map(Jobbtelefon::nummer);
        var primaryTelefonnummer = ressursState.map(RessursState::primaryTelefonnummer);
        return jobbtelefon.orElseGet(() -> primaryTelefonnummer.orElse(null));
    }

    public List<Jobbtelefon> getAlleTelefonnummer(String navident) {
        var ressursState = ressursStateRepository.findByNavident(navident);
        var jobbtelefon = ressursState.map(RessursState::jobbtelefon);
        var primaryTelefonnummer = ressursState.map(ressurs -> new Jobbtelefon(ressurs.primaryTelefonnummer(), TelefonType.PRIVAT_TELEFON));
        return Stream.of(jobbtelefon, primaryTelefonnummer).filter(Optional::isPresent).map(Optional::get).toList();
    }

    public Optional<RessursState> findRessursStateByNavident(String navident) {
        return ressursStateRepository.findByNavident(navident);
    }

    public Map<String, Telefon> getExistingTelefoner() {
        return ressursStateRepository.getAll().stream()
            .filter(ressurs -> ressurs.jobbtelefon() != null && ressurs.navident() != null)
            .collect(Collectors.toMap(
                RessursState::fid,
                ressurs -> new Telefon(ressurs.navident(), ressurs.jobbtelefon().nummer(), ressurs.jobbtelefon().type())
            ));
    }

    public void oppdaterRessursStateJobbtelefon(String fid, String navident, Jobbtelefon telefon) {
        Origination origination = new Origination("nom-api", "nom-api");
        InputMetadata inputMetadata = new InputMetadata("nom-api BatchJob", "Datavarehus telefonoppdatering", null, origination);
        if (telefon != null) {
            inputMetadata.toBuilder().inputInterfaceMethodArguments(telefon).build();
        }

        ressursCommandService.oppdaterJobbtelefonEvent(
            new OppdaterJobbTelefonCommand(
                fid,
                navident,
                telefon
            ),
            inputMetadata
        );
    }
}

