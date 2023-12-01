package no.nav.nom.apps.api.teamcat;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.SneakyThrows;
import no.nav.nom.apps.api.clients.ad.AdClient;
import no.nav.nom.apps.api.common.PrefixedUlidCreator;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhet.api.app.OrgEnhetMapper;
import no.nav.nom.apps.api.orgenhet.implementations.OrgEnhet;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.apps.api.ressurs.api.app.RessursLedereResolver;
import no.nav.nom.apps.api.ressurs.api.dto.RessursLeder;
import no.nav.nom.apps.api.ressurs.eventsourcing.state.RessursStateRepository;
import no.nav.nom.apps.api.rest.RestTestBase;
import no.nav.nom.contract.ad.AdRessurs;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.util.Assert.isTrue;

public class TeamcatControllerTest extends RestTestBase {

    @MockBean
    protected RessursStateRepository ressursStateRepository;

    @MockBean

    private OrgenhetService orgenhetService;

    @MockBean
    private OrgtilknytningService orgtilknytningService;

    @MockBean
    private AdClient adClient;

    @MockBean
    private RessursLedereResolver ressursLedereResolver;

    @MockBean
    private OrgEnhetMapper orgEnhetMapper;


    private Optional<RessursState> createRessursState(String navident) {
        RessursState ressursState = new RessursState(
            PrefixedUlidCreator.createRessursUlid(),
            navident + "person",
            navident,
            Sektor.NAV_STATLIG,
            new Visningsnavn(
                "f" + navident,
                Optional.of("m" + navident),
                "e" + navident
            ),
            Optional.of(LocalDate.now().plusDays(10)),
            Optional.empty()
        );
        return  Optional.of(ressursState);
    }

    private AdRessurs createAdRessurs(String navident){
        return AdRessurs.builder()
            .navIdent(navident)
            .email(navident + "@" + navident + ".com") .build();
    }

    @Test
    @SneakyThrows
    public void kanHenteEnkeltRessurs(){

        var resId = "A123321";

        when(ressursStateRepository.findByNavident(resId)).thenReturn(createRessursState(resId));
        when(adClient.fetchInAdRessursInBatches(anyList())).thenReturn(Map.of(resId,createAdRessurs(resId)));

        // Kan hente enkelt-ressurs
        var y = webTestClient.get()
            .uri("/api/v1/teamcat/ressurs/" + resId)
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(noScopesClientToken());
            })
            .exchange()
            .expectStatus().is2xxSuccessful()
            .expectBody(TeamcatRessursDTO.class);

        var z = y.returnResult();
        var ok1 =  z.getResponseBody().navident().equals(resId);
        var ok2 = z.getResponseBody().ressurstype().equals(TeamcatRessursDTO.ResourceType.INTERN);
        isTrue(ok1 && ok2,"ok");
    }

    @Test
    @SneakyThrows
    public void notfoundExample(){

        var resId = "A123432";
        webTestClient.get()
            .uri("/api/v1/teamcat/ressurs/" + resId)
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(noScopesClientToken());
            })
            .exchange()
            .expectStatus().isNotFound();
    }


    private OrgtilknytningState createOtks(String navident, String oeId){
        return new OrgtilknytningState(oeId,navident,oeId,true,LocalDate.now(),Optional.empty());
    }

    private OrgEnhet createOes(String oeId, String name, String overOeId) {
        return new OrgEnhet(OrgEnhetState.builder().navn(name)
            .fid(oeId)
            .id(oeId)
            .overordnetOrgEnhetFid(Optional.ofNullable(overOeId))
            .agressoId(Optional.of(oeId))
            .build());
    }

    @Test
    @SneakyThrows
    public void kanHenteAvdeling() {

        var resId = "A123321";
        var oeId = "oe1";
        var oeNavn = "Navn";

        when(ressursStateRepository.findByNavident(resId)).thenReturn(createRessursState(resId));
        when(adClient.fetchInAdRessursInBatches(anyList())).thenReturn(Map.of(resId, createAdRessurs(resId)));
        when(orgtilknytningService.getByNavIdent(resId)).thenReturn(List.of(createOtks(resId, oeId)));
        when(orgenhetService.getById(oeId)).thenReturn(Optional.of(createOes(oeId, oeNavn,null)));
        when(ressursLedereResolver.ledere(any())).thenReturn(Stream.of("B321321").map(it -> new RessursLeder(null,it,true)).collect(Collectors.toList()));

        var y = webTestClient.get()
            .uri("/api/v1/teamcat/ressurs/" + resId)
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(noScopesClientToken());
            })
            .exchange()
            .expectStatus().is2xxSuccessful()
            .expectBody(TeamcatRessursDTO.class);

        var z = y.returnResult().getResponseBody();
        Assertions.assertEquals(resId, z.navident());
        Assertions.assertEquals(TeamcatRessursDTO.ResourceType.INTERN, z.ressurstype());
//        Assertions.assertEquals(oeNavn, z.avdelingnavn());
    }



    @Test
    @SneakyThrows
    public void kanHenteAvdelingOver() {

        var oe1 = createOes("oe1","name1",null);
        var oe2 = createOes("oe2","name2",oe1.getId());
        var oe3 = createOes("oe3","name3",oe2.getId());
        var oe4 = createOes("oe4","name4",oe3.getId());

        var resId = "A123321";
        var oeId = oe4.getId();

        when(ressursStateRepository.findByNavident(resId)).thenReturn(createRessursState(resId));
        when(adClient.fetchInAdRessursInBatches(anyList())).thenReturn(Map.of(resId, createAdRessurs(resId)));
        when(orgtilknytningService.getByNavIdent(resId)).thenReturn(List.of(createOtks(resId, oeId)));

        mockEnheterAsHierarchy(List.of(oe1,oe2,oe3,oe4));
        
        
        when(ressursLedereResolver.ledere(any())).thenReturn(Stream.of("B321321").map(it -> new RessursLeder(null,it,true)).collect(Collectors.toList()));
        
        
        var y = webTestClient.get()
            .uri("/api/v1/teamcat/ressurs/" + resId)
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(noScopesClientToken());
            })
            .exchange()
            .expectStatus().is2xxSuccessful()
            .expectBody(TeamcatRessursDTO.class);

        var z = y.returnResult().getResponseBody();
        Assertions.assertEquals(resId, z.navident());
        Assertions.assertEquals(TeamcatRessursDTO.ResourceType.INTERN, z.ressurstype());


        var w = webTestClient.get().uri("/api/v1/teamcat/enhet/" + oe4.getId())
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(noScopesClientToken());
            })
            .exchange()
            .expectStatus().is2xxSuccessful()
            .expectBody(TeamcatOrgenhetDTO.class)
            .returnResult().getResponseBody();


        assert w != null;
        Assertions.assertEquals(oe3.getNavn(), w.avdelingnavn());

    }


    @Test
    @SneakyThrows
    public void kanHenteMangeRessurser() {

        var oe1 = createOes("oe1","name1",null);
        var oe2 = createOes("oe2","name2",oe1.getId());
        var oe3 = createOes("oe3","name3",oe2.getId());
        var oe4 = createOes("oe4","name4",oe3.getId());

        var res1 = createRessursState("A123321");
        var res2 = createRessursState("A123322");
        var resid1 = res1.get().navident();
        var resid2 = res2.get().navident();

        when(ressursStateRepository.findByNavidentBulk(List.of(resid1,resid2))).thenReturn(Map.of(resid1,res1.get(),resid2,res2.get()));
        when(adClient.fetchInAdRessursInBatches(anyList())).thenReturn(Map.of(resid1, createAdRessurs(resid1),resid2,createAdRessurs(resid2)));
        when(orgtilknytningService.getByNavIdent(resid1)).thenReturn(List.of(createOtks(resid1, oe1.getId())));
        mockEnheterAsHierarchy(List.of(oe1,oe2,oe3,oe4));

        var y = webTestClient.post()
            .uri("/api/v1/teamcat/ressurser")
            .bodyValue(List.of(resid1,resid2))
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(noScopesClientToken());
            })
            .exchange()
            .expectStatus().is2xxSuccessful()
            .expectBody(Map.class);

        var z = (Map<String,Object>) y.returnResult().getResponseBody();
        Assertions.assertNotNull(z);
        Assertions.assertEquals(2,z.size());
    }

    @Test
    @SneakyThrows
    public void kanHenteFlereEnheter() {

        var oe1 = createOes("oe1","name1",null);
        var oe2 = createOes("oe2","name2",oe1.getId());
        var oe3 = createOes("oe3","name3",oe2.getId());
        var oe4 = createOes("oe4","name4",oe3.getId());



        mockEnheterAsHierarchy(List.of(oe1,oe2,oe3,oe4));

        var w = webTestClient.get().uri("/api/v1/teamcat/alleEnheter")
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(noScopesClientToken());
            })
            .exchange()
            .expectStatus().is2xxSuccessful()
            .expectBody(JsonNode.class)
            .returnResult().getResponseBody();


        assert w != null;
        Assertions.assertEquals(4,w.size());
        Assertions.assertTrue(w.has("oe1"));
//        Assertions.assertTrue(w.values().stream().anyMatch(it -> it.id().equals(oe2.getId())));
    }

    private void mockEnheterAsHierarchy(List<OrgEnhet> toMock) {
        when(orgenhetService.getAllEnheter()).thenReturn(toMock.stream().map(it -> (IOrgenhet) it).toList());

        OrgEnhet prev = null;
        for(var oe : toMock){
            when(orgenhetService.getByFid(oe.getFid())).thenReturn(Optional.of(oe));
            when(orgenhetService.getById(oe.getFid())).thenReturn(Optional.of(oe));
            when(orgEnhetMapper.mapFrom(oe)).thenReturn(new no.nav.nom.apps.api.orgenhet.api.app.dto.OrgEnhet(oe.getId(),
                oe.getAgressoId(),
                "abc",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                prev != null ? prev.getId() : null,
                null,
                List.of(),
                null
            ));
            prev = oe;
        }
    }


}