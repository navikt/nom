package no.nav.nom.apps.api.orgenhet.eventsourcing.producer;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
@Slf4j
@SpringJUnitConfig(classes = OrgEnhetIdGenerator.class)
public class OrgEnhetIdGeneratorTest {
    @Autowired
    private OrgEnhetIdGenerator orgEnhetIdGenerator;

    @MockitoBean
    OrgEnhetStateRepository orgEnhetStateService;

    String idRegex = "^[bdfghjkmnprstv][aeuy]\\d{3}[abcdefghjkmnprstuvwxyz]$"; // 2 lowercase letters (consonant + vovel) + 3 digits + 1 lowercase letter

    @Test
    public void test20Randoms() {
        for (int i=0;i<20;i++) {
            String id = orgEnhetIdGenerator.newId();
//            log.info("Generert id: {}", id);
            assertTrue(id.matches(idRegex), "No match: %s".formatted(id));
            assertFalse(orgEnhetStateService.findByFid(id).isPresent());
            orgEnhetStateService.update(newOrgEnhet(id));
        }
    }

    private OrgEnhetState newOrgEnhet(String id) {
        return new OrgEnhetState(
            "fid", id,
            "navn",
            LocalDate.now(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }
}