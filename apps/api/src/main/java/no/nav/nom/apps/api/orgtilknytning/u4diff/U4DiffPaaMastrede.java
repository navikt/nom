package no.nav.nom.apps.api.orgtilknytning.u4diff;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.orgenhet.implementations.OrgEnhet;
import no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil;
import no.nav.nom.apps.api.orgtilknytning.dvh.StillingPeriode;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.AvvikleOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreDagligOppfolgingOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreOrgtilknytningDato;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.FjernOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.OpprettOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.OrgtilknytningCommand;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.isCurrentOrFuturePeriod;
import static no.nav.nom.apps.api.orgtilknytning.u4diff.U4DiffType.EKSTRA_U4_PERIODE;
import static no.nav.nom.apps.api.orgtilknytning.u4diff.U4DiffType.FORSKJELL_I_U4_PERIODE;
import static no.nav.nom.apps.api.orgtilknytning.u4diff.U4DiffType.MANGLENDE_STATLIG_I_U4;
import static no.nav.nom.apps.api.orgtilknytning.u4diff.U4DiffType.MANGLENDE_U4_PERIODE;
import static org.apache.commons.lang3.BooleanUtils.forEach;

@Slf4j
@Service
public class U4DiffPaaMastrede {
    private final U4DiffRepository u4DiffRepository;
    private final OrgEnhetStateRepository orgEnhetStateRepository;
    private final RessursStateRepository ressursStateRepository;
    private final ObjectMapper objectMapper;
    private final OrgtilknytningStateRepository orgtilknytningStateRepository;

    public U4DiffPaaMastrede(U4DiffRepository u4DiffRepository, OrgEnhetStateRepository orgEnhetStateRepository,
                             RessursStateRepository ressursStateRepository, ObjectMapper objectMapper,
                             OrgtilknytningStateRepository orgtilknytningStateRepository
    ) {
        this.u4DiffRepository = u4DiffRepository;
        this.orgEnhetStateRepository = orgEnhetStateRepository;
        this.ressursStateRepository = ressursStateRepository;
        this.objectMapper = objectMapper;
        this.orgtilknytningStateRepository = orgtilknytningStateRepository;
    }

    @Transactional
    public void rapporterU4Diff(String navident, Map<StillingPeriode, OrgtilknytningState> endringer, List<OrgtilknytningState> perioderManglerIU4, List<StillingPeriode> ekstraPerioderIU4) {
        try {
            log.debug("Mottatt U4 diff for navident {}", navident);
            var deleted = u4DiffRepository.deleteByNavident(navident);

            var aktiveOgFremtidigeEkstraPerioder = ekstraPerioderIU4.stream().filter(PeriodeUtil::isCurrentOrFuturePeriod).toList();
            var aktiveOgFremtidigeManglendePerioder = perioderManglerIU4.stream().filter(PeriodeUtil::isCurrentOrFuturePeriod).toList();
            var aktiveOgFremtidigeEndringer = endringer.entrySet().stream().filter(entry -> isCurrentOrFuturePeriod(entry.getKey()) || isCurrentOrFuturePeriod(entry.getValue())).toList();

            aktiveOgFremtidigeEkstraPerioder.forEach(sp -> ekstraPeriodeIU4(sp, navident));
            aktiveOgFremtidigeManglendePerioder.forEach(ot -> periodeManglerIU4(ot, navident, MANGLENDE_U4_PERIODE));
            aktiveOgFremtidigeEndringer.forEach(entry -> forskjellIPerioder(entry.getKey(), entry.getValue(), navident));

            log.debug("Slettet {} tidligere rapporterte U4 diff for navident {}, og opprettet {}", deleted, navident,
                aktiveOgFremtidigeEkstraPerioder.size() + aktiveOgFremtidigeManglendePerioder.size() + aktiveOgFremtidigeEndringer.size());
        } catch (Exception e) {
            log.error("Feil ved rapportering av U4 diff for navident {}", navident, e);
        }
    }

    @Transactional
    public void rapporterIngenU4Diff(String navident) {
        try {
            log.debug("Ingen U4 diff for navident {} - fjerner evt tidligere rapporterte differ", navident);
            var deleted = u4DiffRepository.deleteByNavident(navident);
            log.debug("Slettet {} tidligere rapporterte U4 diff for navident {}", deleted, navident);
        } catch (Exception e) {
            log.error("Feil ved rapportering av U4 diff for navident {}", navident, e);
        }
    }

    @Transactional
    public void rapporterU4NavidenterBehandlet(List<String> u4StatligeNavidenter) {
        try {
            var statligeNotInU4 = ressursStateRepository.getAll().stream()
                .filter(r -> r.sektor() == Sektor.NAV_STATLIG)
                .map(RessursState::navident)
                .distinct()
                .filter(navident -> !u4StatligeNavidenter.contains(navident))
                .toList();

            var oterManglendeIU4 = orgtilknytningStateRepository.findByNavidentIn(statligeNotInU4).stream()
                    .filter(PeriodeUtil::isCurrentOrFuturePeriod)
                    .toList();

            var navidenterForOterManglendeIU4 = oterManglendeIU4.stream().map(OrgtilknytningState::navident).distinct().toList();

            navidenterForOterManglendeIU4.forEach(navident -> {
                log.debug("Funnet manglende U4 for statlig navident {}", navident);
                var deleted = u4DiffRepository.deleteByNavident(navident);
                var oterForNavidentManglendeIU4 = oterManglendeIU4.stream().filter(ot -> ot.navident().equals(navident)).toList();

                oterForNavidentManglendeIU4.forEach(ot -> periodeManglerIU4(ot, navident, MANGLENDE_STATLIG_I_U4));

                log.debug("Slettet {} tidligere rapporterte U4 diff for navident {}, og opprettet {}", deleted, navident, oterForNavidentManglendeIU4.size());
            });

            // Slett alle andre tidligere differ....
            var navidenterForTidligereDiffer = u4DiffRepository.findAll().stream().map(U4Diff::getNavident)
                .distinct()
                .filter(navident -> !navidenterForOterManglendeIU4.contains(navident))
                .filter(navident -> !u4StatligeNavidenter.contains(navident))
                .toList();

            navidenterForTidligereDiffer.forEach(this::rapporterIngenU4Diff);
        } catch (Exception e) {
            log.error("Feil ved rapportering av manglende statlige i U4", e);
        }
    }

    private void ekstraPeriodeIU4(StillingPeriode sp, String navident) {
        u4DiffRepository.save(U4Diff.builder()
                .navident(navident)
                .nomSektor(getSektor(navident))
                .type(EKSTRA_U4_PERIODE)
                .u4OrgenhetId(sp.orgenhet().getId())
                .u4OrgenhetNavn(sp.orgenhet().getNavn())
                .u4GyldigFom(sp.gyldigFom())
                .u4GyldigTom(sp.gyldigTom())
                .u4Dol(sp.erDagligOppfolging())
                .u4Sektor(Sektor.NAV_STATLIG) // Foreløpig kalles dette kun fra Statlig import sammenligning
                .u4StillingPeriode(sp.toString())
                .build());
    }

    private void periodeManglerIU4(OrgtilknytningState ot, String navident, U4DiffType diffType) {
        var orgenhet = orgEnhetStateRepository.findById(ot.orgEnhetId()).orElseThrow();
        var otJson = objectMapper.valueToTree(ot);
        u4DiffRepository.save(U4Diff.builder()
                .navident(navident)
                .nomSektor(getSektor(navident))
                .type(diffType)
                .nomOrgenhetId(orgenhet.id())
                .nomOrgenhetNavn(orgenhet.navn())
                .nomGyldigFom(ot.gyldigFom())
                .nomGyldigTom(ot.gyldigTom().orElse(null))
                .nomDol(ot.erDagligOppfolging())
                .nomOtSektor(ot.sektor())
                .nomOrgtilknytning(otJson.toPrettyString())
                .build());
    }

    private void forskjellIPerioder(StillingPeriode sp, OrgtilknytningState ot, String navident) {
        if (erStillingPeriodeEgentligLikOT(sp, ot)) return;
        var orgenhet = orgEnhetStateRepository.findById(ot.orgEnhetId()).orElseThrow();
        var otJson = objectMapper.valueToTree(ot);
        u4DiffRepository.save(U4Diff.builder()
                .navident(navident)
                .nomSektor(getSektor(navident))
                .type(FORSKJELL_I_U4_PERIODE)
                .u4OrgenhetId(sp.orgenhet().getId())
                .u4OrgenhetNavn(sp.orgenhet().getNavn())
                .u4GyldigFom(sp.gyldigFom())
                .u4GyldigTom(sp.gyldigTom())
                .u4Dol(sp.erDagligOppfolging())
                .u4Sektor(Sektor.NAV_STATLIG) // Foreløpig kalles dette kun fra Statlig import sammenligning
                .u4StillingPeriode(sp.toString())
                .nomOrgenhetId(orgenhet.id())
                .nomOrgenhetNavn(orgenhet.navn())
                .nomGyldigFom(ot.gyldigFom())
                .nomGyldigTom(ot.gyldigTom().orElse(null))
                .nomDol(ot.erDagligOppfolging())
                .nomOtSektor(ot.sektor())
                .nomOrgtilknytning(otJson.toPrettyString())
                .build());
    }

    private static boolean erStillingPeriodeEgentligLikOT(StillingPeriode sp, OrgtilknytningState ot) {
        if (ot.erDagligOppfolging() == sp.erDagligOppfolging() && Objects.equals(ot.gyldigTom().orElse(null), sp.gyldigTom())) {
            if (ot.gyldigFom().isAfter(sp.gyldigFom()) && ot.gyldigFom().isBefore(LocalDate.now().minusYears(3).withDayOfMonth(1))) {
                log.debug("Forskjell i U4 periode er klipp av historisk periode på 3år. Rapporteres ikke som diff");
                return true;
            }
        }
        return false;
    }

    private Sektor getSektor(String navident) {
        return ressursStateRepository.findByNavident(navident).orElseThrow().sektor();
    }

}
