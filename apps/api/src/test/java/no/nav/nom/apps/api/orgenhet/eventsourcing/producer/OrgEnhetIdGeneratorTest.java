package no.nav.nom.apps.api.orgenhet.eventsourcing.producer;

import no.nav.nom.apps.api.orgenhet.OrgEnhetStateService;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringJUnitConfig(classes = OrgEnhetIdGenerator.class)
public class OrgEnhetIdGeneratorTest {
    @Autowired
    private OrgEnhetIdGenerator orgEnhetIdGenerator;

    @MockBean
    OrgEnhetStateService orgEnhetStateService;

    String idRegex = "^(?:(?![oil])[a-z]){2}\\d{3}[a-z^oil]$"; // 2 lowercase letters except 'iol' + 4 digits + 1 lowercase letters except 'iol'

    @Test
    public void test20Randoms() {
        for (int i=0;i<2000;i++) {
            String id = orgEnhetIdGenerator.newId();
            assertTrue(id.matches(idRegex), "No match: %s".formatted(id));
            assertFalse(orgEnhetStateService.findByFid(id).isPresent());
            orgEnhetStateService.putEventAggregate(newOrgEnhet(id));
        }
    }

    private OrgEnhetState newOrgEnhet(String id) {
        return new OrgEnhetState("fid", id, "navn", LocalDate.now(), Optional.empty(),Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty() );
    }

}