package no.nav.nom.apps.api.ressurs.contract;

import com.fasterxml.jackson.databind.JsonNode;
import no.nav.nom.apps.api.hendelse.contract.HendelseMetadataSerialize;
import no.nav.nom.contract.nom.api.graphql.Ressurs;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface IRessursService {
    Optional<IRessurs> findByPersonIdent(String personIdent);

    Optional<IRessurs> findByFid(UUID fid);

    Optional<IRessurs> findByNavIdent(String navIdent);
    Map<String, IRessurs> findByPersonIdentBulk(List<String> personIdenter);
    Map<String, IRessurs> findByNavIdentBulk(List<String> navIdenter);
    List<IRessurs> getAll();
    IRessurs opprettRessurs(String personIdent, String navIdent, String navIdentForbokstav, HendelseMetadataSerialize metadata);
    IRessurs opprettRessurs(String personerident, JsonNode data, HendelseMetadataSerialize metadata);
    IRessurs opprettRessurs(String personerident, Map<String, Object> data, HendelseMetadataSerialize metadata);
    Ressurs convertToGraphQlResponse(IRessurs ressurs);
    List<IRessurs> matchByNavident(String navident);
}
