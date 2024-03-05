package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.remedy;

import no.nav.nom.apps.nomremedyintegrasjon.TestBase;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.remedy.NomIntegrasjonEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;


@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class NomIntegrasjonRepositoryTest extends TestBase {


    @Test
    @DisplayName("Ensure that NomIntegrasjonEntity is immutable")
    void ensureImmutableEntity() {

        var entryId = "000000000000001";

        var initialSavedEntityValues = nomIntegrasjonRepository.save(
            NomIntegrasjonEntity.builder()
                .entryId(entryId)
                .navId("Z654321")
                .enhetsId("some-enhet").build()
        );

        var entityWithUpdatedValues = NomIntegrasjonEntity.builder()
            .entryId(entryId)
            .navId("X123456")
            .enhetsId("Some-other-enhet")
            .fornavn("Donald").build();

        nomIntegrasjonRepository.save(entityWithUpdatedValues);

        assertThat(nomIntegrasjonRepository.findById(entryId))
            .contains(initialSavedEntityValues);
    }
}
