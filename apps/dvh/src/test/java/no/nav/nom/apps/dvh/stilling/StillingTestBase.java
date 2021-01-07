package no.nav.nom.apps.dvh.stilling;

import no.nav.nom.apps.dvh.TestBase;
import org.springframework.beans.factory.annotation.Autowired;

abstract class StillingTestBase extends TestBase {

    @Autowired
    StillingDvhTask stillingDvhTask;
}
