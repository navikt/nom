package no.nav.nom.apps.admin.sanitycheck.service;

import no.nav.nom.apps.admin.dto.ressurs.RessursHendelseKafkaDTO;
import no.nav.nom.apps.admin.events.ressurs.api.RessursHendelseApiRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RessursHendelseSanityCheckServiceTest {

    private static final String NAVIDENT_1 = "A123456";
    private static final String NAVIDENT_2 = "B654321";
    private static final String NAVIDENT_3 = "C456789";

    Random random = new Random();

    @Mock
    private RessursHendelseApiRepository ressursHendelseApiRepositoryMock;

    @InjectMocks
    private RessursHendelseSanityCheckService service;


    @Test
    @DisplayName("Finne duplikate hendelser i liste over hendelser")
    void should_find_duplicate_events() {
        var hendelser = List.of(
            getRessursHendelse(NAVIDENT_1),
            getRessursHendelse(NAVIDENT_2),
            getRessursHendelse(NAVIDENT_1),
            getRessursHendelse(NAVIDENT_3)
        );

        var duplikater = service.getDuplikateHendelser(hendelser);

        assertThat(duplikater.size()).as("Forventet antall duplikater").isEqualTo(2);
    }

    @Test
    @DisplayName("Hent duplikate hendelser og sorter liste")
    void should_sort_events() {
        var duplikater = List.of(
            getRessursHendelse(NAVIDENT_3),
            getRessursHendelse(NAVIDENT_3),
            getRessursHendelse(NAVIDENT_2),
            getRessursHendelse(NAVIDENT_2),
            getRessursHendelse(NAVIDENT_1),
            getRessursHendelse(NAVIDENT_1)
        );
        when(ressursHendelseApiRepositoryMock.getRessursHendelser()).thenReturn(duplikater);

        var sorterteDuplikater = service.finnDuplikateHendelser();

        assertThat(sorterteDuplikater.getFirst().navident()).isEqualTo(NAVIDENT_1);
        assertThat(sorterteDuplikater.get(5).navident()).isEqualTo(NAVIDENT_3);
    }

    private RessursHendelseKafkaDTO getRessursHendelse(String navident) {
        return RessursHendelseKafkaDTO.builder()
            .fid("ressurs-" + random.nextInt(999999))
            .navident(navident)
            .build();
    }

}