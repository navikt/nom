package no.nav.nom.apps.sfintegrasjon.sfhendelse;

import no.nav.nom.apps.sfintegrasjon.behandling.repository.Behandling;
import no.nav.nom.apps.sfintegrasjon.behandling.repository.BehandlingRepository;
import no.nav.nom.apps.sfintegrasjon.behandling.repository.BehandlingStatus;
import no.nav.nom.apps.sfintegrasjon.orgenhet.NomApiClient;
import no.nav.nom.apps.sfintegrasjon.sfhendelse.dto.HendelseDiff;
import no.nav.nom.apps.sfintegrasjon.sfhendelse.dto.NyRessurs;
import no.nav.nom.apps.sfintegrasjon.sfhendelse.dto.OrgtilknytningerEndring;
import no.nav.nom.apps.sfintegrasjon.sfhendelse.dto.VisningsnavnEndring;
import no.nav.nom.apps.sfintegrasjon.sfhendelse.repository.RessursHendelse;
import no.nav.nom.apps.sfintegrasjon.sfhendelse.repository.RessursHendelseRepository;
import no.nav.nom.apps.sfintegrasjon.sfhendelse.repository.SfHendelseBehandling;
import no.nav.nom.apps.sfintegrasjon.sfhendelse.repository.SfHendelseBehandlingRepository;
import no.nav.nom.contract.nom.AnsettelsesType;
import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.stereotype.Component;

import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class SfHendelseBehandler {

    private final SfHendelseResolver sfHendelseResolver;
    private final RessursStateRepository ressursStateRepository;
    private final OrgtilknytningStateRepository orgtilknytningStateRepository;
    private final BehandlingRepository behandlingRepository;
    private final SfHendelseBehandlingRepository sfHendelseBehandlingRepository;
    private final RessursHendelseRepository ressursHendelseRepository;
    private final NomApiClient nomApiClient;


    public SfHendelseBehandler(
        RessursHendelseRepository ressursHendelseRepository,
        SfHendelseResolver sfHendelseResolver,
        RessursStateRepository ressursStateRepository, OrgtilknytningStateRepository orgtilknytningStateRepository, BehandlingRepository behandlingRepository,
        SfHendelseBehandlingRepository sfHendelseBehandlingRepository, RessursHendelseRepository ressursHendelseRepository1, NomApiClient nomApiClient
    ) {
        this.sfHendelseResolver = sfHendelseResolver;
        this.ressursStateRepository = ressursStateRepository;
        this.orgtilknytningStateRepository = orgtilknytningStateRepository;
        this.behandlingRepository = behandlingRepository;
        this.sfHendelseBehandlingRepository = sfHendelseBehandlingRepository;
        this.ressursHendelseRepository = ressursHendelseRepository1;
        this.nomApiClient = nomApiClient;
    }

    public List<RessursHendelse> getMottatteHendelser() {
        return ressursHendelseRepository.getSingleMottattHendelsePerNavident();
    }

    @Transactional
    public void legTilBehandling(RessursHendelse ressursHendelse) {
        var behandling = behandlingMedStatus(BehandlingStatus.MOTTATT, null);
        ressursHendelse.setBehandlingsId(behandling.getBehandlingsId());

        behandlingRepository.save(behandling);
        ressursHendelseRepository.save(ressursHendelse);
    }

    @Transactional
    public void lagreMedStatusProcessert(RessursHendelse hendelse, List<HendelseDiff> diffs) {
        var behandling = behandlingMedStatus(BehandlingStatus.PROSESSERT, hendelse.getBehandlingsId());

        var sfHendelseBehandling = SfHendelseBehandling.builder()
            .navident(hendelse.getNavident())
            .behandlingsId(hendelse.getBehandlingsId())
            .endring(diffs)
            .build();

        behandlingRepository.save(behandling);
        sfHendelseBehandlingRepository.save(sfHendelseBehandling);
    }

    public void setBehandlingFeilet(Long behandlingsId) {
        var behandling = behandlingMedStatus(BehandlingStatus.FEILET, behandlingsId);

        behandlingRepository.save(behandling);
    }

    public void setBehandlingSynket(Long behandlingsId) {
        var behandling = behandlingMedStatus(BehandlingStatus.SYNKET, behandlingsId);

        behandlingRepository.save(behandling);
    }

    public List<RessursHendelse> getUbehandletHendelser() {
        var behandlingerInProgress = behandlingRepository.getIkkeProsesserteBehandlinger().stream().map(Behandling::getBehandlingsId).toList();

        return ressursHendelseRepository.getUbehandledeHendelser(behandlingerInProgress);
    }

    public List<HendelseDiff> utledEndringer(RessursHendelse ressursHendelse) {
        var diffs = new LinkedList<HendelseDiff>();
        var hendelse = sfHendelseResolver.from(ressursHendelse.getData());
        var ressursState = ressursStateRepository.findByNavident(ressursHendelse.getNavident()).orElse(null);

        if (ressursState == null) {
            diffs.add(nyRessurs(hendelse));
        } else if (visningsnavnAvvik(hendelse, ressursState)) {
            diffs.add(visningsnavnEndring(hendelse));
        }

        var sfStillinger = sfHendelseResolver.sfStillinger();
        var orgtilknytningerEndring = strukturerOgKategoriserOter(ressursHendelse.getNavident(), sfStillinger);

        diffs.add(orgtilknytningerEndring);

        return diffs;
    }

    public void synkMotNom(SfHendelseBehandling hendelseBehandling) {
        hendelseBehandling.getEndring().forEach(endring -> {
            if (endring instanceof NyRessurs nyRessurs) {
                nomApiClient.opprettRessurs(nyRessurs);
            } else if (endring instanceof VisningsnavnEndring visningsnavnEndring) {
                nomApiClient.oppdaterVisningsnavn(hendelseBehandling.getNavident(), visningsnavnEndring);
            } else if (endring instanceof OrgtilknytningerEndring orgtilknytningerEndring) {
                nomApiClient.oppdaterOrgtilknytninger(hendelseBehandling.getNavident(), orgtilknytningerEndring);
            }
        });
    }

    OrgtilknytningerEndring strukturerOgKategoriserOter(String navident, List<Stilling> stillinger) {
        var stillingerMedSammenhengendePerioder = stillinger.stream()
            .collect(Collectors.groupingBy(Stilling::stillingId))
            .values().stream().map(this::tilSammenhengende).flatMap(Collection::stream).toList();

        var orgtilknytningEndring = new OrgtilknytningerEndring(new ArrayList<>(), new ArrayList<>());
        var eksisterendeOter = orgtilknytningStateRepository.findByNavident(navident);
        var eksisterendeStatligeOter = eksisterendeOter.stream()
            .filter(ot -> ot.sektor() == Sektor.NAV_STATLIG)
            .sorted(Comparator.comparing(OrgtilknytningState::gyldigFom, Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();

        eksisterendeOter.stream()
            .filter(ot -> ot.sektor() != Sektor.NAV_STATLIG)
            .forEach(ot -> orgtilknytningEndring.eksisterende().add(OrgtilknytningerEndring.EksisterendeOt.from(ot)));

        stillingerMedSammenhengendePerioder.forEach(stilling -> {
            var erstattetOt = finnMatchendeOtState(stilling, eksisterendeStatligeOter);

            if (erstattetOt.isPresent()) {
                orgtilknytningEndring.eksisterende().add(OrgtilknytningerEndring.EksisterendeOt.from(erstattetOt.get().fid(), stilling));
            } else {
                orgtilknytningEndring.nye().add(OrgtilknytningerEndring.NyOt.from(stilling));
            }
        });

        return orgtilknytningEndring;
    }

    private Optional<OrgtilknytningState> finnMatchendeOtState(Stilling stilling, List<OrgtilknytningState> otStates) {
        return otStates.stream().filter(otState ->
                stilling.stillingId().equals(new Stilling.StillingId(otState.orgEnhetId(), otState.orgtilknytningType(), otState.erDagligOppfolging()))
                    && (otState.gyldigFom().isBefore(stilling.gyldigTom().plusDays(1))
                    || otState.gyldigTom().isAfter(stilling.gyldigFom().minusDays(1))))
            .findFirst();
    }

    public Optional<Stilling> erstattendeStilling(List<Stilling> stillinger, OrgtilknytningState otState) {
        return stillinger.stream()
            .filter(stilling -> otState.gyldigFom().isBefore(stilling.gyldigTom().plusDays(1)) && otState.gyldigTom().isAfter(stilling.gyldigFom()))
            .findFirst();
    }

    private List<Stilling> tilSammenhengende(List<Stilling> stillinger) {
        var sammenhengende = new ArrayList<Stilling>();
        var sorterteStillinger = stillinger.stream()
            .sorted(Comparator.comparing(Stilling::gyldigFom).thenComparing(Stilling::gyldigTom, Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();

        LocalDate gyldigFom = null;
        LocalDate gyldigTom = null;

        for (Stilling stilling : sorterteStillinger) {
            if (gyldigFom == null) {
                gyldigFom = stilling.gyldigFom();
                gyldigTom = stilling.gyldigTom();
            } else {
                if (inkluderer(gyldigTom, stilling)) {
                    continue;
                }

                if (erOverlappende(gyldigTom, stilling)) {
                    gyldigTom = maxDate(gyldigTom, stilling.gyldigTom());
                } else {
                    sammenhengende.add(new Stilling(stilling.stillingId(), gyldigFom, gyldigTom, stilling.primaryFysiskBrukersted()));
                    gyldigFom = stilling.gyldigFom();
                    gyldigTom = stilling.gyldigTom();
                }
            }
        }

        sammenhengende.add(new Stilling(
            sorterteStillinger.getLast().stillingId(),
            gyldigFom,
            gyldigTom,
            sorterteStillinger.getLast().primaryFysiskBrukersted()
        ));

        return sammenhengende;
    }

    private LocalDate maxDate(LocalDate date1, LocalDate date2) {
        return date1 == null
            ? date1
            : date2 == null ? date2 : Stream.of(date1, date2).max(Comparator.naturalOrder()).get();
    }

    private boolean erOverlappende(LocalDate gyldigTom, Stilling stilling) {
        return stilling.gyldigFom().isBefore(gyldigTom.plusDays(1))
            && (stilling.gyldigTom() == null || stilling.gyldigTom().isAfter(gyldigTom));
    }

    private boolean inkluderer(LocalDate gyldigTom, Stilling stilling) {
        return gyldigTom == null
            || stilling.gyldigFom().isBefore(gyldigTom.plusDays(1))
            && stilling.gyldigTom() != null
            && stilling.gyldigTom().isBefore(gyldigTom.plusDays(1));
    }

    private boolean visningsnavnAvvik(SfHendelseResolver hendelseResolver, RessursState ressursState) {
        return !hendelseResolver.visningsnavn().equals(ressursState.visningsnavn().toString());
    }

    private NyRessurs nyRessurs(SfHendelseResolver hendelseResolver) {
        return new NyRessurs(
            hendelseResolver.personident(),
            hendelseResolver.fornavn(),
            hendelseResolver.etternavn(),
            Sektor.NAV_STATLIG,
            hendelseResolver.dolOt(),
            hendelseResolver.startdato(),
            hendelseResolver.sluttDato(),
            AnsettelsesType.valueOf(hendelseResolver.tilknytningtype()),
            hendelseResolver.primaryFysiskBrukersted(),
            Tilknytningtype.valueOf(hendelseResolver.tilknytningtype()),
            IdentType.STANDARD
        );
    }

    private VisningsnavnEndring visningsnavnEndring(SfHendelseResolver hendelseResolver) {
        return new VisningsnavnEndring(hendelseResolver.fornavn(), hendelseResolver.etternavn());
    }

    private Behandling behandlingMedStatus(BehandlingStatus status, Long behandlingsId) {
        return Behandling.builder()
            .behandlingsId(Optional.ofNullable(behandlingsId).orElse(behandlingRepository.getNextBehandlingsId()))
            .status(status)
            .behandlingTidspunkt(LocalDateTime.now())
            .build();
    }
}
