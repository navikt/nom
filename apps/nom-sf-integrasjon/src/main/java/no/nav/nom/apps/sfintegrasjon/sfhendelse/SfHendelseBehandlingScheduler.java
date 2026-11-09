package no.nav.nom.apps.sfintegrasjon.sfhendelse;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.sfintegrasjon.behandling.repository.Behandling;
import no.nav.nom.apps.sfintegrasjon.behandling.repository.BehandlingRepository;
import no.nav.nom.apps.sfintegrasjon.sfhendelse.repository.RessursHendelseRepository;
import no.nav.nom.apps.sfintegrasjon.sfhendelse.repository.SfHendelseBehandlingRepository;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SfHendelseBehandlingScheduler {

    private final RessursHendelseRepository ressursHendelseRepository;
    private final SfHendelseBehandler hendelseBehandler;
    private final BehandlingRepository behandlingRepository;
    private final SfHendelseBehandlingRepository hendelseBehandlingRepository;
    private final LeaderElection leaderElection;

    public SfHendelseBehandlingScheduler(
        RessursHendelseRepository ressursHendelseRepository, SfHendelseBehandler hendelseBehandler,
        BehandlingRepository behandlingRepository, SfHendelseBehandlingRepository hendelseBehandlingRepository, LeaderElection leaderElection
    ) {
        this.ressursHendelseRepository = ressursHendelseRepository;
        this.hendelseBehandler = hendelseBehandler;
        this.behandlingRepository = behandlingRepository;
        this.hendelseBehandlingRepository = hendelseBehandlingRepository;
        this.leaderElection = leaderElection;
    }

    //@Scheduled(initialDelay = 25000, fixedDelay = 5000)
    public void initBehandling() {
        if (leaderElection.isLeader()) {
            hendelseBehandler.getMottatteHendelser().forEach(hendelse -> {
                try {
                    hendelseBehandler.legTilBehandling(hendelse);
                } catch (Exception e) {
                    log.warn("Initiering av behandling av hendelse med sporingsId {} feilet", hendelse.getSporingsId());
                }
            });
        }
    }

    //@Scheduled(initialDelay = 30000, fixedDelay = 5000)
    public void processHendelse() {
        if (leaderElection.isLeader()) {
            hendelseBehandler.getUbehandletHendelser().forEach(hendelse -> {
                try {
                    var endringer = hendelseBehandler.utledEndringer(hendelse);
                    hendelseBehandler.lagreMedStatusProcessert(hendelse, endringer);
                } catch (Exception e) {
                    log.warn("Behandling av mottatt sf hendelse med sporingsId {} feilet", hendelse.getSporingsId());
                    hendelseBehandler.setBehandlingFeilet(hendelse.getBehandlingsId());
                }
            });
        }
    }

    //@Scheduled(initialDelay = 35000, fixedDelay = 5000)
    public void synkTilNom() {
        if (leaderElection.isLeader()) {
            var usynkede = behandlingRepository.getProsesserteBehandlinger().stream().map(Behandling::getId).toList();

            hendelseBehandlingRepository.getSfHendelseBehandlingsByIdIsIn(usynkede).forEach(hendelseBehandling -> {
                try {
                    hendelseBehandler.synkMotNom(hendelseBehandling);
                    hendelseBehandler.setBehandlingSynket(hendelseBehandling.getBehandlingsId());
                } catch (Exception e) {
                    log.warn("Synk av hendelsebehandling med behandlingsid {} feilet", hendelseBehandling.getId());
                    hendelseBehandler.setBehandlingFeilet(hendelseBehandling.getBehandlingsId());
                }
            });
        }
    }
}
