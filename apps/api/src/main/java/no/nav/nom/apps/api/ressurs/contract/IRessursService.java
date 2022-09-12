package no.nav.nom.apps.api.ressurs.contract;

import com.fasterxml.jackson.databind.JsonNode;
import no.nav.nom.apps.api.hendelse.contract.HendelseMetadataSerialize;
import no.nav.nom.contract.nom.api.graphql.Ressurs;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface IRessursService {
    Optional<RessursI> findByPersonIdent(String personIdent);

    Optional<RessursI> findByFid(UUID fid);

    Optional<RessursI> findByNavIdent(String navIdent);
    Map<String, RessursI> findByPersonIdentBulk(List<String> personIdenter);
    Map<String, RessursI> findByNavIdentBulk(List<String> navIdenter);
    List<RessursI> getAll();
    RessursI opprettRessurs(String personIdent, String navIdent, String navIdentForbokstav, HendelseMetadataSerialize metadata);
    RessursI opprettRessurs0(String personerident, JsonNode data, HendelseMetadataSerialize metadata);
    Ressurs convertToGraphQlResponse(RessursI ressurs);
    List<RessursI> matchByNavident(String navident);
}
