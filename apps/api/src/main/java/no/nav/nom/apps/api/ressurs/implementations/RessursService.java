package no.nav.nom.apps.api.ressurs.implementations;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import no.nav.nom.apps.api.common.security.AuthenticationProperties;
import no.nav.nom.apps.api.common.security.TokenRepository;
import no.nav.nom.apps.api.hendelse.contract.HendelseMetadataSerialize;
import no.nav.nom.apps.api.ressurs.contract.RessursI;
import no.nav.nom.apps.api.ressurs.contract.IRessursService;
import no.nav.nom.apps.api.ressurs.implementations.dvh.DvhRessursInterface;
import no.nav.nom.apps.api.ressurs.implementations.postgresql.EventBasedRessursService;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RessursService implements IRessursService {

    private final DvhRessursInterface dvhRessursInterface;
    private final EventBasedRessursService eventBasedRessursService;
    private final TokenRepository tokenRepository;
    private final AuthenticationProperties authenticationProperties;

    public Optional<RessursI> findByFid(UUID fid){
        return eventBasedRessursService.findByFid(fid);
    }

    @Override
    public Optional<RessursI> findByNavIdent(String navIdent){
        return eventBasedRessursService.findByNavIdent(navIdent);
    }

    @Override
    public List<RessursI> matchByNavident(String navident) {
        return eventBasedRessursService.matchByNavident(navident);
    }

    @Override
    public Optional<RessursI> findByPersonIdent(String personIdent){
        return eventBasedRessursService.findByPersonIdent(personIdent);
    }

    public Map<String, RessursI> findByNavIdentBulk(List<String> navIdenter){
        return eventBasedRessursService.findByNavIdentBulk(navIdenter);
    }

    public Map<String, RessursI> findByPersonIdentBulk(List<String> personIdenter){
        return eventBasedRessursService.findByPersonIdentBulk(personIdenter);
    }

    @Override
    public List<RessursI> getAll(){
        return eventBasedRessursService.getAll();
    }

    @Override
    @Transactional
    public RessursI opprettRessurs(String personIdent, String navIdent, String navIdentForbokstav, HendelseMetadataSerialize hendelseMetadata) {
        return eventBasedRessursService.opprettRessurs(personIdent, navIdent, navIdentForbokstav, hendelseMetadata);
    }

    @Override
    @Transactional
    public RessursI opprettRessurs0(String personident, JsonNode data, HendelseMetadataSerialize metadata) {
        return eventBasedRessursService.opprettRessurs0(personident, data, metadata);
    }

    @Override
    public Ressurs convertToGraphQlResponse(RessursI ressurs) {
        return Ressurs.builder()
                .nomId(ressurs.getId())
                .navIdent(ressurs.getNavIdent())
                .navident(ressurs.getNavIdent())
                .personIdent(ressurs.getPersonIdent())
                .personident(ressurs.getPersonIdent())
                .ressurstype(dvhRessursInterface.getRessurstypeGraphQlResponse(ressurs.getNavIdent()))
                .sektor(dvhRessursInterface.getRessurstypeGraphQlResponse(ressurs.getNavIdent()))
                .sluttdato(dvhRessursInterface.getSluttdatoForPersonident(ressurs.getPersonIdent()))
                .build();
    }
}
