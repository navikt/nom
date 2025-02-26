package no.nav.nom.apps.api.ressurs;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.ressurs.api.app.NomRemedyIntegrasjonClient;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursCommandService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterFysiskeBrukerstederCommand;
import no.nav.nom.apps.api.ressurs.model.FysiskBrukersted;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import static no.nav.nom.apps.api.common.SpringCacheConfig.FYSISK_BRUKERSTED_CACHE_NAME;

@Slf4j
@Service
public class FysiskBrukerstedService {
    static final String CACHE_TTL = "PT1H";

    private final RessursStateRepository ressursStateRepository;
    private final OrgtilknytningStateRepository orgtilknytningStateRepository;
    private final RessursCommandService ressursCommandService;
    private final FysiskBrukerstedCache fysiskBrukerstedCache;

    public FysiskBrukerstedService(
            RessursStateRepository ressursStateRepository, OrgtilknytningStateRepository orgtilknytningStateRepository,
            RessursCommandService ressursCommandService, FysiskBrukerstedCache fysiskBrukerstedCache
    ) {
        this.ressursStateRepository = ressursStateRepository;
        this.orgtilknytningStateRepository = orgtilknytningStateRepository;
        this.ressursCommandService = ressursCommandService;
        this.fysiskBrukerstedCache = fysiskBrukerstedCache;
    }

    public List<RessursState> getAlleRessurserMedFysiskBrukersted(String fysiskBrukerstedNr) {
        var result =  ressursStateRepository.getAll().stream()
                .filter(ressurs -> Objects.equals(ressurs.primaryFysiskBrukersted().orElse(null), fysiskBrukerstedNr))
                .toList();
        return result;
    }

    public List<RessursState> getAlleRessurserPaaEnhetMedFysiskBrukersted(String orgenhetId, String fysiskBrukerstedNr) {
        var navidenter = orgtilknytningStateRepository.getAktiveEllerFremtidigeNavidenterPaaEnhet(orgenhetId);
        return ressursStateRepository.findByNavidentBulk(navidenter).values().stream()
            .filter(ressurs -> Objects.equals(ressurs.primaryFysiskBrukersted().orElse(null), fysiskBrukerstedNr))
            .toList();
    }

    public void flyttAlleRessurserFraFysiskBrukerstedTilFysiskBrukersted(String fraFysiskBrukerstedNr, String tilFysiskBrukerstedNr, InputMetadata inputMetaData) {
        var navidenter = getAlleRessurserMedFysiskBrukersted(fraFysiskBrukerstedNr).stream().map(RessursState::navident).toList();
        flyttRessurserTilFysiskBrukersted(navidenter, tilFysiskBrukerstedNr, inputMetaData);
    }

    public void flyttAlleRessurserPaaEnhetFraFysiskBrukerstedTilFysiskBrukersted(String orgenhet, String fraFysiskBrukerstedNr, String tilFysiskBrukerstedNr, InputMetadata inputMetaData ) {
        var navidenter = getAlleRessurserPaaEnhetMedFysiskBrukersted(orgenhet, fraFysiskBrukerstedNr).stream().map(RessursState::navident).toList();
        flyttRessurserTilFysiskBrukersted(navidenter, tilFysiskBrukerstedNr, inputMetaData);
    }

    public void flyttRessurserTilFysiskBrukersted(List<String> navidenter, String tilFysiskBrukerstedNr, InputMetadata inputMetaData) {
        navidenter.forEach(navident -> oppdaterRessursFysiskBrukersted(navident, tilFysiskBrukerstedNr, inputMetaData));
    }

    private void oppdaterRessursFysiskBrukersted(String navident, String fysiskBrukerstedNr, InputMetadata inputMetaData) {
        if (!fysiskBrukerstedExists(fysiskBrukerstedNr)) {
            throw new IllegalArgumentException("Ugyldig fysisk brukersted " + fysiskBrukerstedNr);
        }

        var fid = ressursStateRepository.findByNavident(navident)
            .orElseThrow(() -> new IllegalArgumentException("Fant ikke ressurs med navident " + navident))
            .fid();

        var ressursCmd = new OppdaterFysiskeBrukerstederCommand(
            fid,
            Set.of(fysiskBrukerstedNr),
            fysiskBrukerstedNr
        );
        ressursCommandService.oppdaterFysiskeBrukerstederCommand(ressursCmd, inputMetaData);
    }

    public boolean fysiskBrukerstedExists(String fysiskBrukerstedNr) {
        return fysiskBrukerstedCache.getAll().stream()
            .anyMatch(fysiskBrukersted -> fysiskBrukersted.nr().equals(fysiskBrukerstedNr));
    }
}

