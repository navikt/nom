package no.nav.nom.apps.navident.pdl;

import java.util.List;

public interface PdlService {
    // Return all known id's for provided id.
    // Expect first id in list to be the official active id
    List<String> getKnownPersonider(String id);
}
