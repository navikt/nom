package no.nav.nom.apps.sfintegrasjon.behandling;

import no.nav.nom.apps.sfintegrasjon.behandling.repository.BehandlingStatus;
import no.nav.nom.apps.sfintegrasjon.organisasjon.repository.OrganisasjonHendelseRepository;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class BehandlingScheduler {

    private final OrganisasjonHendelseRepository organisasjonHendelseRepository;
    private final BehandlingService behandlingService;
    private final LeaderElection leaderElection;

    private final long DELTA_SECONDS = 5;

    public BehandlingScheduler(OrganisasjonHendelseRepository organisasjonHendelseRepository, BehandlingService behandlingService,
                               LeaderElection leaderElection
    ) {
        this.organisasjonHendelseRepository = organisasjonHendelseRepository;
        this.behandlingService = behandlingService;
        this.leaderElection = leaderElection;
    }

    // Ved :00, før byggeren (:20) og sendingen (:40) i samme minutt
    @Scheduled(cron = "0 * * * * *")
    public void setMottattStatusForOrganisasjonsHendelser() {
        if (leaderElection.isLeader()) {
            organisasjonHendelseRepository.getOrgranisasjonHendelserUtenStatus(LocalDateTime.now().minusSeconds(DELTA_SECONDS))
                .forEach(hendelse -> behandlingService.oppdaterStatus(hendelse, BehandlingStatus.MOTTATT));
        }
    }
}
