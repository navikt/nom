package no.nav.nom.apps.navident;

import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.apps.navident.rnrservices.Respons;

public interface IdentService {

    Respons bestillIdent(String personid, IdentType type, char idPrefix);
}