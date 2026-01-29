package no.nav.nom.apps.api.clients.dvhords;

import tools.jackson.databind.JsonNode;

public interface DvhOrdsClient {
    JsonNode dvhGetJsonData(String urlPath);

    JsonNode dvhGetJsonDataLimitRows(String urlPath, int numRowsToFetch);

    JsonNode dvhGetJsonDataOrdsFilter(String urlPath, String ordsFilter);

    JsonNode dvhGetJsonDataOrdsPaginated(String urlPath, int offset, int limit);
}
