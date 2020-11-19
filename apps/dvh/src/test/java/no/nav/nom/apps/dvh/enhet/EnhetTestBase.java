package no.nav.nom.apps.dvh.enhet;

import no.nav.nom.apps.dvh.TestBase;
import org.springframework.beans.factory.annotation.Autowired;

abstract class EnhetTestBase extends TestBase {

    @Autowired
    EnhetDvhTask task;

}
