package no.nav.nom.apps.dvh.ressurs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

@DisplayName("Ressurs")
class RessursDvhTest extends RessursTestBase {

    @Test
    void scenario1() {
        doReturn(Optional.empty()).when(stateRepository).findSequenceFor(anyString());

        ressursDvhTask.perform();
    }
}
