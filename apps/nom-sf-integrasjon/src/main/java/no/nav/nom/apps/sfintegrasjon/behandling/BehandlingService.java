package no.nav.nom.apps.sfintegrasjon.behandling;

import no.nav.nom.apps.sfintegrasjon.behandling.repository.Behandling;
import no.nav.nom.apps.sfintegrasjon.behandling.repository.BehandlingRepository;
import no.nav.nom.apps.sfintegrasjon.behandling.repository.BehandlingStatus;
import no.nav.nom.apps.sfintegrasjon.organisasjon.repository.OrganisasjonHendelse;
import no.nav.nom.apps.sfintegrasjon.organisasjon.repository.OrganisasjonHendelseRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class BehandlingService {

    private final BehandlingRepository behandlingRepository;
    private final OrganisasjonHendelseRepository organisasjonHendelseRepository;

    public BehandlingService(BehandlingRepository behandlingRepository, OrganisasjonHendelseRepository organisasjonHendelseRepository) {
        this.behandlingRepository = behandlingRepository;
        this.organisasjonHendelseRepository = organisasjonHendelseRepository;
    }

    public void oppdaterStatus(OrganisasjonHendelse organisasjonHendelse, BehandlingStatus status) {
        if (status == BehandlingStatus.MOTTATT) {
            var behandling = Behandling.builder()
                .behandlingsId(behandlingRepository.getBehandlingsId())
                .status(BehandlingStatus.MOTTATT)
                .behandlingTidspunkt(LocalDateTime.now())
                .build();
            behandlingRepository.save(behandling);

            organisasjonHendelse.setBehandlingsId(behandling.getBehandlingsId());
            organisasjonHendelseRepository.save(organisasjonHendelse);
        }
    }
}
