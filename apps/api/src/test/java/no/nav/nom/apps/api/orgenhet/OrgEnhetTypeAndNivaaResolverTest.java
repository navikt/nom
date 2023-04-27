package no.nav.nom.apps.api.orgenhet;

import lombok.val;
import no.nav.nom.apps.api.DvhOrgEnhetMock;
import no.nav.nom.apps.api.TestBase;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.dvh.dto.DvhOrganisering;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetCommandService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.EndreNomNivaa;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.EndreOrgEnhetsType;
import no.nav.nom.apps.api.orgenhet.eventsourcing.state.OrgEnhetStateRepository;
import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.OrgEnhetsType;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class OrgEnhetTypeAndNivaaResolverTest extends TestBase {

    @MockBean
    private DvhRepository dvhRepository;

    @Autowired
    private OrgEnhetStateRepository orgEnhetStateRepository;

    @Autowired
    private OrgEnhetTypeAndNivaaResolver orgEnhetTypeAndNivaaResolver;

    @MockBean
    private OrgEnhetCommandService orgEnhetCommandService;

    @BeforeEach
    public void init(){
        orgEnhetStateRepository.clear();
    }

    @Test
    public void sould_resolve_orgenhettype_for_all_nodes(){
        val mockedEnhet1 = DvhOrgEnhetMock.getBuilderWithDefault()
            .ORGENHET_KODE("testId1")
            .ORGENHET_NAVN("testNavn")
            .ORGNODE_NIV("ORGENHET")
            .ENHETTYPE_NAVN("Dir")
            .build();
        val mockedEnhet2 = DvhOrgEnhetMock.getBuilderWithDefault()
            .ORGENHET_KODE("testId2")
            .ORGENHET_NAVN("testNavn")
            .ORGNODE_NIV("ORGENHET")
            .ENHETTYPE_NAVN("Dir")
            .build();
        val mockedEnhet3 = DvhOrgEnhetMock.getBuilderWithDefault()
            .ORGENHET_KODE("testId3")
            .ORGENHET_NAVN("testNavn")
            .ORGNODE_NIV("ORGENHET")
            .build();
        val mockedEnhet4 = DvhOrgEnhetMock.getBuilderWithDefault()
            .ORGENHET_KODE("testId4")
            .ORGENHET_NAVN("testNavn")
            .ORGNODE_NIV("ORGNIV4")
            .build();
        val mockedEnhet5 = DvhOrgEnhetMock.getBuilderWithDefault()
            .ORGENHET_KODE("testId5")
            .ORGENHET_NAVN("testNavn")
            .ORGNODE_NIV("ORGNIV3")
            .build();

        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet1, 1, "202204", Optional.empty()));
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet2, 2, "202204", Optional.empty()));
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet3, 3, "202204", Optional.empty()));
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet4, 4, "202204", Optional.empty()));
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet5, 5, "202204", Optional.empty()));

        mockOrganisering(mockedEnhet1, mockedEnhet4);
        mockOrganisering(mockedEnhet2, mockedEnhet4);
        mockOrganisering(mockedEnhet3, mockedEnhet4);
        mockOrganisering(mockedEnhet4, mockedEnhet5);

        orgEnhetTypeAndNivaaResolver.resolve();

        final var endreType = new EndreOrgEnhetsType(
            Optional.of(OrgEnhetsType.DIR),
            new InputMetadata("nom-api BatchJob", "Datavarehus", null, new Origination("nom-api", "nom-api"))
        );

        verify(orgEnhetCommandService).endreOrgEnhetsType(eq("FID_1"), eq(endreType));
        verify(orgEnhetCommandService).endreOrgEnhetsType(eq("FID_2"), eq(endreType));
        verify(orgEnhetCommandService).endreOrgEnhetsType(eq("FID_4"), eq(endreType));
        verify(orgEnhetCommandService).endreOrgEnhetsType(eq("FID_5"), eq(endreType));
    }

    @Test
    public void sould_not_resolve_orgenhettype_if_less_than_half(){
        val mockedEnhet1 = DvhOrgEnhetMock.getBuilderWithDefault()
            .ORGENHET_KODE("testId1")
            .ORGENHET_NAVN("testNavn")
            .ORGNODE_NIV("ORGENHET")
            .ENHETTYPE_NAVN("Dir")
            .build();
        val mockedEnhet2 = DvhOrgEnhetMock.getBuilderWithDefault()
            .ORGENHET_KODE("testId2")
            .ORGENHET_NAVN("testNavn")
            .ORGNODE_NIV("ORGENHET")
            .build();
        val mockedEnhet3 = DvhOrgEnhetMock.getBuilderWithDefault()
            .ORGENHET_KODE("testId3")
            .ORGENHET_NAVN("testNavn")
            .ORGNODE_NIV("ORGENHET")
            .build();
        val mockedEnhet4 = DvhOrgEnhetMock.getBuilderWithDefault()
            .ORGENHET_KODE("testId4")
            .ORGENHET_NAVN("testNavn")
            .ORGNODE_NIV("ORGNIV4")
            .build();

        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet1, 1, "202204", Optional.empty()));
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet2, 2, "202204", Optional.empty()));
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet3, 3, "202204", Optional.empty()));
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet4, 4, "202204", Optional.empty()));

        mockOrganisering(mockedEnhet1, mockedEnhet4);
        mockOrganisering(mockedEnhet2, mockedEnhet4);
        mockOrganisering(mockedEnhet3, mockedEnhet4);

        orgEnhetTypeAndNivaaResolver.resolve();

        final var endreType = new EndreOrgEnhetsType(
            Optional.of(OrgEnhetsType.DIR),
            new InputMetadata("nom-api BatchJob", "Datavarehus", null, new Origination("nom-api", "nom-api"))
        );

        verify(orgEnhetCommandService).endreOrgEnhetsType(eq("FID_1"), eq(endreType));
    }

    @Test
    public void should_not_update_state_if_already_in_state(){
        val mockedEnhet1 = DvhOrgEnhetMock.getBuilderWithDefault()
            .ORGENHET_KODE("testId1")
            .ORGENHET_NAVN("testNavn")
            .ORGNODE_NIV("ORGENHET")
            .ENHETTYPE_NAVN("Dir")
            .ORGENHET_TYPE(OrgEnhetsType.DIR)
            .build();
        val mockedEnhet2 = DvhOrgEnhetMock.getBuilderWithDefault()
            .ORGENHET_KODE("testId2")
            .ORGENHET_NAVN("testNavn")
            .ENHETTYPE_NAVN("Dir")
            .ORGNODE_NIV("ORGENHET")
            .build();
        val mockedEnhet3 = DvhOrgEnhetMock.getBuilderWithDefault()
            .ORGENHET_KODE("testId3")
            .ORGENHET_NAVN("testNavn")
            .ORGNODE_NIV("ORGNIV4")
            .build();
        val mockedEnhet4 = DvhOrgEnhetMock.getBuilderWithDefault()
            .ORGENHET_KODE("testId4")
            .ORGENHET_NAVN("testNavn")
            .ORGNODE_NIV("ORGNIV3")
            .ORGENHET_TYPE(OrgEnhetsType.DIR)
            .build();

        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet1, 1, "202204", Optional.empty()));
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet2, 2, "202204", Optional.empty()));
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet3, 3, "202204", Optional.empty()));
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet4, 4, "202204", Optional.empty()));

        mockOrganisering(mockedEnhet1, mockedEnhet3);
        mockOrganisering(mockedEnhet2, mockedEnhet3);
        mockOrganisering(mockedEnhet3, mockedEnhet4);

        orgEnhetTypeAndNivaaResolver.resolve();

        final var endreType = new EndreOrgEnhetsType(
            Optional.of(OrgEnhetsType.DIR),
            new InputMetadata("nom-api BatchJob", "Datavarehus", null, new Origination("nom-api", "nom-api"))
        );

        verify(orgEnhetCommandService, times(2)).endreOrgEnhetsType(any(), any());
        verify(orgEnhetCommandService).endreOrgEnhetsType(eq("FID_2"), eq(endreType));
        verify(orgEnhetCommandService).endreOrgEnhetsType(eq("FID_3"), eq(endreType));
    }

    @Test
    public void should_remove_types_from_state(){
        val mockedEnhet1 = DvhOrgEnhetMock.getBuilderWithDefault()
            .ORGENHET_KODE("testId1")
            .ORGENHET_NAVN("testNavn")
            .ORGNODE_NIV("ORGENHET")
            .ORGENHET_TYPE(OrgEnhetsType.DIR)
            .build();
        val mockedEnhet2 = DvhOrgEnhetMock.getBuilderWithDefault()
            .ORGENHET_KODE("testId2")
            .ORGENHET_NAVN("testNavn")
            .ORGNODE_NIV("ORGENHET")
            .ORGENHET_TYPE(OrgEnhetsType.DIR)
            .build();
        val mockedEnhet3 = DvhOrgEnhetMock.getBuilderWithDefault()
            .ORGENHET_KODE("testId3")
            .ORGENHET_NAVN("testNavn")
            .ORGNODE_NIV("ORGNIV4")
            .ORGENHET_TYPE(OrgEnhetsType.DIR)
            .build();
        val mockedEnhet4 = DvhOrgEnhetMock.getBuilderWithDefault()
            .ORGENHET_KODE("testId4")
            .ORGENHET_NAVN("testNavn")
            .ORGNODE_NIV("ORGNIV3")
            .ORGENHET_TYPE(OrgEnhetsType.DIR)
            .build();

        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet1, 1, "202204", Optional.empty()));
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet2, 2, "202204", Optional.empty()));
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet3, 3, "202204", Optional.empty()));
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet4, 4, "202204", Optional.empty()));

        mockOrganisering(mockedEnhet1, mockedEnhet3);
        mockOrganisering(mockedEnhet2, mockedEnhet3);
        mockOrganisering(mockedEnhet3, mockedEnhet4);

        orgEnhetTypeAndNivaaResolver.resolve();

        final var endreType = new EndreOrgEnhetsType(
            Optional.empty(),
            new InputMetadata("nom-api BatchJob", "Datavarehus", null, new Origination("nom-api", "nom-api"))
        );

        verify(orgEnhetCommandService, times(4)).endreOrgEnhetsType(any(), any());
        verify(orgEnhetCommandService).endreOrgEnhetsType(eq("FID_1"), eq(endreType));
        verify(orgEnhetCommandService).endreOrgEnhetsType(eq("FID_2"), eq(endreType));
        verify(orgEnhetCommandService).endreOrgEnhetsType(eq("FID_3"), eq(endreType));
        verify(orgEnhetCommandService).endreOrgEnhetsType(eq("FID_4"), eq(endreType));
    }

    private void mockOrganisering(DvhOrgEnhetMock current, DvhOrgEnhetMock overordnet) {
        when(dvhRepository.getOrganiseringer(
            eq(current.getORGENHET_KODE()), eq(current.getORGNODE_NIV()), eq(current.getORGENHET_NAVN())))
            .thenReturn(List.of(createOrganiseringOverordnet(current, overordnet)));
    }

    private DvhOrganisering createOrganiseringOverordnet(DvhOrgEnhetMock current, DvhOrgEnhetMock overordnet) {
        return new DvhOrganisering(
            overordnet.getORGENHET_KODE(),
            overordnet.getORGNODE_NIV(),
            overordnet.getORGENHET_NAVN(),
            current.getORGENHET_KODE(),
            current.getORGNODE_NIV(),
            current.getORGENHET_NAVN(),
            LocalDate.now().minusDays(5),
            LocalDate.now().plusDays(5)
        );
    }

    @Test
    public void sould_resolve_nomnivaa_for_all_nodes_when_dir(){
        val mockedEnhet1 = DvhOrgEnhetMock.getBuilderWithDefault()
            .ORGENHET_KODE("testId1")
            .ORGENHET_NAVN("testNavn")
            .ORGNODE_NIV("ORGENHET")
            .ENHETTYPE_NAVN("Dir")
            .build();
        val mockedEnhet2 = DvhOrgEnhetMock.getBuilderWithDefault()  //ARBEIDSOMRAADE
            .ORGENHET_KODE("testId2")
            .ORGENHET_NAVN("testNavn")
            .ORGNODE_NIV("ORGNIV2")
            .build();
        val mockedEnhet3 = DvhOrgEnhetMock.getBuilderWithDefault()  //DRIFTSENHET
            .ORGENHET_KODE("testId3")
            .ORGENHET_NAVN("testNavn")
            .ORGNODE_NIV("ORGNIV1")
            .build();

        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet1, 1, "202204", Optional.empty()));
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet2, 2, "202204", Optional.empty()));
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet3, 3, "202204", Optional.empty()));

        mockOrganisering(mockedEnhet1, mockedEnhet2);
        mockOrganisering(mockedEnhet2, mockedEnhet3);

        orgEnhetTypeAndNivaaResolver.resolve();

        verify(orgEnhetCommandService, times(2)).endreNomNivaa(any(), any());
        verify(orgEnhetCommandService).endreNomNivaa(eq("FID_2"), eq(createMockEndreNomNivaaCommand(NomNivaa.ARBEIDSOMRAADE)));
        verify(orgEnhetCommandService).endreNomNivaa(eq("FID_3"), eq(createMockEndreNomNivaaCommand(NomNivaa.DRIFTSENHET)));
    }

    @Test
    public void sould_resolve_nomnivaa_for_all_nodes(){
        val mockedEnhet1 = DvhOrgEnhetMock.getBuilderWithDefault()
            .ORGENHET_KODE("testId1")
            .ORGENHET_NAVN("testNavn")
            .ORGNODE_NIV("ORGENHET")
            .build();
        val mockedEnhet2 = DvhOrgEnhetMock.getBuilderWithDefault()  //ARBEIDSOMRAADE
            .ORGENHET_KODE("testId2")
            .ORGENHET_NAVN("testNavn")
            .ORGNODE_NIV("ORGNIV3")
            .build();
        val mockedEnhet3 = DvhOrgEnhetMock.getBuilderWithDefault()  //DRIFTSENHET
            .ORGENHET_KODE("testId3")
            .ORGENHET_NAVN("testNavn")
            .ORGNODE_NIV("ORGNIV2")
            .build();
        val mockedEnhet4 = DvhOrgEnhetMock.getBuilderWithDefault()  //LINJEENHET
            .ORGENHET_KODE("testId4")
            .ORGENHET_NAVN("testNavn")
            .ORGNODE_NIV("ORGNIV1")
            .build();

        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet1, 1, "202204", Optional.empty()));
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet2, 2, "202204", Optional.empty()));
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet3, 3, "202204", Optional.empty()));
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet4, 4, "202204", Optional.empty()));

        mockOrganisering(mockedEnhet1, mockedEnhet2);
        mockOrganisering(mockedEnhet2, mockedEnhet3);
        mockOrganisering(mockedEnhet3, mockedEnhet4);

        orgEnhetTypeAndNivaaResolver.resolve();

        verify(orgEnhetCommandService, times(3)).endreNomNivaa(any(), any());
        verify(orgEnhetCommandService).endreNomNivaa(eq("FID_2"), eq(createMockEndreNomNivaaCommand(NomNivaa.ARBEIDSOMRAADE)));
        verify(orgEnhetCommandService).endreNomNivaa(eq("FID_3"), eq(createMockEndreNomNivaaCommand(NomNivaa.DRIFTSENHET)));
        verify(orgEnhetCommandService).endreNomNivaa(eq("FID_4"), eq(createMockEndreNomNivaaCommand(NomNivaa.LINJEENHET)));
    }

    private EndreNomNivaa createMockEndreNomNivaaCommand(NomNivaa nomNivaa) {
        return new EndreNomNivaa(
            Optional.ofNullable(nomNivaa),
            new InputMetadata("nom-api BatchJob", "Datavarehus", null, new Origination("nom-api", "nom-api"))
        );
    }
}
