package no.nav.nom.apps.admin.sanitycheck.service;

import no.nav.nom.apps.admin.dto.ressurs.RessursHendelseKafkaDTO;
import no.nav.nom.apps.admin.events.ressurs.api.RessursHendelseApiRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class RessursHendelseSanityCheckServiceTest {

    private static final String NAVIDENT_1 = "A123456";
    private static final String NAVIDENT_2 = "B654321";
    private static final String NAVIDENT_3 = "C456789";

    @Mock
    private RessursHendelseApiRepository ressursHendelseApiRepositoryMock;
    
    private final RessursHendelseSanityCheckService service = new RessursHendelseSanityCheckService(ressursHendelseApiRepositoryMock);
    

    @Test
    @DisplayName("Finne duplikate hendelser i liste over hendelser")
    void should_find_duplicate_events() {
        var hendelse1 = RessursHendelseKafkaDTO.builder()
            .fid("ressurs-1")
            .navident(NAVIDENT_1)
            .build();

        var hendelse2 = RessursHendelseKafkaDTO.builder()
            .fid("ressurs-2")
            .navident(NAVIDENT_2)
            .build();

        var hendelse3 = RessursHendelseKafkaDTO.builder()
            .fid("ressurs-3")
            .navident(NAVIDENT_1)
            .build();

        var hendelse4 = RessursHendelseKafkaDTO.builder()
            .fid("ressurs-4")
            .navident(NAVIDENT_3)
            .build();
        

        var res = service.getDuplikateHendelser(List.of(hendelse1, hendelse2, hendelse3, hendelse4));
        
        
        assertThat(res.size()).as("Forventet antall duplikater").isEqualTo(1);
    }
}