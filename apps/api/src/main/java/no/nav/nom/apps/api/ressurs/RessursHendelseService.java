package no.nav.nom.apps.api.ressurs;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.api.common.security.AuthenticationProperties;
import no.nav.nom.apps.api.common.security.TokenRepository;
import no.nav.nom.apps.api.hendelse.contract.HendelseDataType;
import no.nav.nom.apps.api.hendelse.contract.HendelseMetadata;
import no.nav.nom.apps.api.hendelse.contract.HendelseMetadataSerialize;
import no.nav.nom.apps.api.hendelse.contract.HendelseRepository;
import no.nav.nom.apps.api.hendelse.implementation.HendelseService;
import no.nav.nom.apps.api.ressurs.implementations.dvh.DvhRessurs;
import no.nav.nom.apps.api.ressurs.implementations.RessursService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RessursHendelseService {

    private final RessursService ressursService;
    private final TokenRepository tokenRepository;
    private final AuthenticationProperties authenticationProperties;
    private final HendelseService hendelseService;
    private final ObjectMapper objectMapper;
    private final HendelseRepository hendelseRepository;

    public void handterRessursIderFraDvh(List<DvhRessurs> dvhRessurs) {

        val bulkSize = 1000;
        for(int i = 0; i<dvhRessurs.size(); i+=bulkSize){
            val selection = dvhRessurs.subList(i, Math.min(i + bulkSize, dvhRessurs.size()));
            val ressursByNavIdent = ressursService.findByNavidentBulk(selection.stream().map(DvhRessurs::getNavIdent).toList());
            val ressursByPersonIdent = ressursService.findByPersonidentBulk(selection.stream().map(DvhRessurs::getPersonIdent).toList());

            val opprettInserts = new LinkedList<DvhRessurs>();
            val endreinserts = new HashMap<UUID, DvhRessurs>();

            selection.forEach( ressurs -> {
                val bothIdsPresent = ressursByNavIdent.containsKey(ressurs.getNavIdent()) && ressursByPersonIdent.containsKey(ressurs.getPersonIdent());
                val nidOnlyPresent = ressursByNavIdent.containsKey(ressurs.getNavIdent()) && !bothIdsPresent;
                val pidOnlyPresent = ressursByPersonIdent.containsKey(ressurs.getPersonIdent())  && !bothIdsPresent;

                if (bothIdsPresent) {
                    val ressursByNid = ressursByNavIdent.get(ressurs.getNavIdent());
                    val ressursByPid = ressursByPersonIdent.get(ressurs.getPersonIdent());
                    val alreadySaved = (ressursByNid.getNavIdent().equals(ressursByPid.getNavIdent()) && ressursByNid.getPersonIdent().equals(ressursByPid.getPersonIdent()));
                    if (!alreadySaved) {
                        log.warn("NavIdent {} er i konflikt med eksisterende ressurs", ressurs.getNavIdent());
                    }
                } else if (nidOnlyPresent) {
                    val fid = ressursByNavIdent.get(ressurs.getNavIdent()).getId();
                    endreinserts.put(fid, ressurs);
                } else if (pidOnlyPresent) {
                    val ressursByPid = ressursByPersonIdent.get(ressurs.getPersonIdent());
                    val eksisterendeNid = ressursByPid.getNavIdent();
                    log.warn("NavIdent {} i DVH er i konflikt med eksisterende ressurs med navident {}",
                            ressurs.getNavIdent(), eksisterendeNid);
                } else {
                    opprettInserts.add(ressurs);
                }
                    }
            );

            createOpprettetHendelser(opprettInserts);
            createEndreTilNyttPersonnummerHendelser(endreinserts);
        }
    }

    @SneakyThrows
    public void createOpprettetHendelser(List<DvhRessurs> dvhRessursList) {
        val jwt = tokenRepository.getSystemAadToken("api://" + authenticationProperties.aad().clientId() + "/.default");
        HendelseMetadata.OpprettetAv opprettetAv = new HendelseMetadata.OpprettetAv(jwt.getJWTClaimsSet().getClaim("azp_name").toString(), jwt.getJWTClaimsSet().getClaim("azp_name").toString());
        LocalDateTime opprettetTidspunkt = LocalDateTime.now();

        hendelseRepository.saveAll(
                dvhRessursList.stream()
                    .map(r ->{
                        val data = objectMapper.createObjectNode();
                        data.put("navident", r.getNavIdent());
                        data.put("personident", r.getPersonIdent());


                        val metaData = HendelseMetadataSerialize.builder()
                                .opprettetAv(opprettetAv)
                                .opprettetTidspunkt(opprettetTidspunkt)
                                .rawInputData(r.getRawData())
                                .build();

                        return hendelseService.convert(hendelseService.createOpprettHendelse(data, metaData, HendelseDataType.RESSURS_V1));
                    }).toList()
        );
    }

    @SneakyThrows
    public void createEndreTilNyttPersonnummerHendelser(Map<UUID, DvhRessurs> dvhRessursByFid) {
        val jwt = tokenRepository.getSystemAadToken("api://" + authenticationProperties.aad().clientId() + "/.default");
        HendelseMetadata.OpprettetAv opprettetAv = new HendelseMetadata.OpprettetAv(jwt.getJWTClaimsSet().getClaim("azp_name").toString(), jwt.getJWTClaimsSet().getClaim("azp_name").toString());
        LocalDateTime opprettetTidspunkt = LocalDateTime.now();

        hendelseRepository.saveAll(
            dvhRessursByFid.entrySet().stream()
                    .map(r -> {
                        val data = objectMapper.createObjectNode();
                        data.put("personident", r.getValue().getPersonIdent());

                        val metaData = HendelseMetadataSerialize.builder()
                                .opprettetAv(opprettetAv)
                                .opprettetTidspunkt(opprettetTidspunkt)
                                .rawInputData(r.getValue().getRawData())
                                .build();

                        return hendelseService.convert(hendelseService.createEndreHendelse(r.getKey(), data, metaData, HendelseDataType.RESSURS_V1));
                    }).toList()
        );
    }
}
