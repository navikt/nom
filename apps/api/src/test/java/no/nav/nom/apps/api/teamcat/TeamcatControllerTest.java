package no.nav.nom.apps.api.teamcat;

import lombok.SneakyThrows;
import no.nav.nom.apps.api.clients.ad.AdClient;
import no.nav.nom.apps.api.common.PrefixedUlidCreator;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhet.implementations.OrgEnhet;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.apps.api.ressurs.api.app.RessursLedereResolver;
import no.nav.nom.apps.api.ressurs.api.dto.RessursLeder;
import no.nav.nom.apps.api.teamcat.TeamcatRessursDTO;
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

    private IOrgenhet createOes(String oeId, String name, String overOeId) {
        return new OrgEnhet(OrgEnhetState.builder().navn(name)
            .fid(oeId)
            .id(oeId)
            .overordnetOrgEnhetFid(Optional.ofNullable(overOeId))
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
        Assertions.assertEquals(oeNavn, z.avdelingnavn());
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
        when(orgenhetService.getByFid(oe1.getFid())).thenReturn(Optional.of(oe1));
        when(orgenhetService.getByFid(oe2.getFid())).thenReturn(Optional.of(oe2));
        when(orgenhetService.getByFid(oe3.getFid())).thenReturn(Optional.of(oe3));
        when(orgenhetService.getById(oe4.getId())).thenReturn(Optional.of(oe4));
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
        Assertions.assertEquals(oe3.getNavn(), z.avdelingnavn());
    }


    @Test
    @SneakyThrows
    public void kanHenteMange() {

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
        when(orgenhetService.getByFid(oe1.getFid())).thenReturn(Optional.of(oe1));
        when(orgenhetService.getByFid(oe2.getFid())).thenReturn(Optional.of(oe2));
        when(orgenhetService.getByFid(oe3.getFid())).thenReturn(Optional.of(oe3));
        when(orgenhetService.getByFid(oe4.getFid())).thenReturn(Optional.of(oe4));

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
        Assertions.assertEquals(2,z.size());
    }


}