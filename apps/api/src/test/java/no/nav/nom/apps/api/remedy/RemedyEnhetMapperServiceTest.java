package no.nav.nom.apps.api.remedy;

import no.nav.nom.apps.api.orgenhet.implementations.OrgEnhet;
import no.nav.nom.apps.api.orgenhet.implementations.OrgenhetService;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RemedyEnhetMapperServiceTest {

    RemedyEnhetMapperService remedyEnhetMapperService;

    @Mock
    OrgenhetService orgenhetService;


    @BeforeEach
    public void init(){
        Resource dataFile = new ClassPathResource("remedy/data.csv");
        remedyEnhetMapperService = new RemedyEnhetMapperService(dataFile, orgenhetService);
    }

    @Test
    public void should_map_to_nomid_from_remedyenhetid(){
        var result = remedyEnhetMapperService.findByRemedyEnhetId("4415");
        assertTrue(result.isPresent());
        assertEquals("ORGNIV3", result.get().orgNivaa());
        assertEquals("443150", result.get().agressoId());
        assertEquals("my788g", result.get().nomId());
        verify(orgenhetService, never()).getEnhetByAgressoIdAndOrgniv(anyString(), anyString());

        String orgniv = "ORGNIV3";
        String agressoId = "202700";
        String nomid = "TEST_NOMID1";
        when(orgenhetService.getEnhetByAgressoIdAndOrgniv(eq(agressoId), eq(orgniv)))
            .thenReturn(Optional.of(new OrgEnhet(createOrgEnhet(nomid, null, null, null))));

        result = remedyEnhetMapperService.findByRemedyEnhetId("2027");
        assertTrue(result.isPresent());
        assertEquals(orgniv, result.get().orgNivaa());
        assertEquals(agressoId, result.get().agressoId());
        assertEquals(nomid, result.get().nomId());

    }

    @Test
    public void should_return_empty_when_nomid_mapping_fails() {
        //existing but not mapped
        var result = remedyEnhetMapperService.findByRemedyEnhetId("0233");
        assertFalse(result.isPresent());

        // non existing
        result = remedyEnhetMapperService.findByRemedyEnhetId("XYZ");
        assertFalse(result.isPresent());
    }

    @Test
    public void should_map_to_remedyenhetid_from_nomid(){
        var result = remedyEnhetMapperService.findByNomId("my788g");
        assertTrue(result.isPresent());
        assertEquals("ORGNIV3", result.get().orgNivaa());
        assertEquals("443150", result.get().agressoId());
        assertEquals("my788g", result.get().nomId());
        verify(orgenhetService, never()).getEnhetByAgressoIdAndOrgniv(anyString(), anyString());

        String orgniv = "ORGNIV3";
        String agressoId = "202700";
        String nomid = "TEST_NOMID1";
        when(orgenhetService.getById(eq(nomid)))
            .thenReturn(Optional.of(new OrgEnhet(createOrgEnhet(nomid, agressoId, orgniv, null))));

        result = remedyEnhetMapperService.findByNomId(nomid);
        assertTrue(result.isPresent());
        assertEquals(orgniv, result.get().orgNivaa());
        assertEquals(agressoId, result.get().agressoId());
        assertEquals("2027", result.get().remedyEnhetId());
    }

    @Test
    public void should_map_to_remedyenhetid_in_parent_from_nomid(){
        var orgenhetState1 = createOrgEnhet("NOMID1", "202700", "ORGNIV3", null);
        when(orgenhetService.getByFid(eq(orgenhetState1.fid())))
            .thenReturn(Optional.of(new OrgEnhet(orgenhetState1)));

        var orgenhetState2 = createOrgEnhet("NOMID2", "2027000", "ORGNIV4", orgenhetState1.fid());
        when(orgenhetService.getById(eq(orgenhetState2.id())))
            .thenReturn(Optional.of(new OrgEnhet(orgenhetState2)));

        var result = remedyEnhetMapperService.findByNomId(orgenhetState2.id());
        assertTrue(result.isPresent());
        assertEquals(orgenhetState1.agressoOrgNivaa().get(), result.get().orgNivaa());
        assertEquals(orgenhetState1.agressoId().get(), result.get().agressoId());
        assertEquals("2027", result.get().remedyEnhetId());
    }

    @Test
    public void should_return_empty_when_remedyenhetid_mapping_fails() {
        //existing but not mapped
        String nomid = "TEST_NOMID1";
        when(orgenhetService.getById(eq(nomid)))
            .thenReturn(Optional.of(new OrgEnhet(createOrgEnhet(nomid, "agressoId", "orgniv", null))));
        var result = remedyEnhetMapperService.findByNomId(nomid);
        assertFalse(result.isPresent());

        // non existing
        result = remedyEnhetMapperService.findByNomId("XYZ");
        assertFalse(result.isPresent());
    }

    private OrgEnhetState createOrgEnhet(String id, String agressoId, String orgnivaa, String overordnetFid) {
        return new OrgEnhetState(
            "fid", id,
            "navn",
            LocalDate.now(),
            Optional.empty(),
            Optional.ofNullable(overordnetFid),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.ofNullable(agressoId),
            Optional.ofNullable(orgnivaa),
            Optional.empty()
        );
    }
}
