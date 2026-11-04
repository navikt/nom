package no.nav.nom.apps.api.orgtilknytning.api.email;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import io.getunleash.Unleash;
import no.nav.nom.apps.api.common.cache.CacheQueueService;
import no.nav.nom.apps.api.common.cache.CacheType;
import no.nav.nom.apps.api.dvh.DvhStartupLoadMonitor;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhetleder.OrgEnhetLederService;
import no.nav.nom.apps.api.ressurs.FysiskBrukerstedService;
import no.nav.nom.apps.api.ressurs.NomMastered;
import no.nav.nom.apps.api.ressurs.model.FysiskBrukersted;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.OrgEnhetLederState;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import no.nav.nom.libs.utils.common.util.DateUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith({SpringExtension.class, MockitoExtension.class})
@ContextConfiguration(classes = {Configuration.class})
public class OrgtilknytningEndretEmailServiceTest {

    Map<String, String> data = new HashMap<>();

    @Autowired
    private Configuration configuration;

    @InjectMocks
    private OrgtilknytningEndretEmailService orgtilknytningEndretEmailService;

    @Mock
    private OrgtilknytningStateRepository orgtilknytningStateRepository;

    @Mock
    private OrgEnhetStateRepository orgEnhetStateRepository;

    @Mock
    private RessursStateRepository ressursStateRepository;

    @Mock
    private OrgenhetService orgenhetService;

    @Mock
    private OrgEnhetLederService orgEnhetLederService;

    @Mock
    private FysiskBrukerstedService fysiskBrukerstedService;

    @Mock
    private CacheQueueService cacheQueueService;

    @Mock
    private LeaderElection leaderElection;

    @Mock
    private DvhStartupLoadMonitor dvhStartupLoadMonitor;

    @Mock
    private NomMastered nomMastered;

    @Mock
    private Unleash unleash;

    @Test
    public void tessFreemarkerTemplate() throws TemplateException, IOException {
        configuration.setClassForTemplateLoading(this.getClass(), "/templates/");
        // setup data
        data.put("resursName", "Resurs Name");
        data.put("navIdent", "A123456");
        data.put("otEndringer", "My endringer");

        // check if autowiring works
        Assertions.assertNotNull(configuration);

        // test if loading template works
        Template template = configuration.getTemplate("ot-endringer-varsel.ftlh");    // <-- load the template
        // check if template is loaded
        Assertions.assertNotNull(template);

        // dump the generated output to System.err
        var stringWriter = new StringWriter();
        template.process(data, stringWriter);
        System.err.println(stringWriter.getBuffer());
    }


    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testComposeOTChangesMessage(boolean detaljertEpost) {
        when(unleash.isEnabled(OrgtilknytningEndretEmailService.UNLEASH_DETALJERT_OT_EPOST)).thenReturn(detaljertEpost);

        // setup data
        String fid = "fid123456";
        String navident = "A123456";
        String navidentChangedBy = "C123456";
        String orgEnhet = "orgenhet123456";
        String timestamp = LocalDateTime.now().toString();
        when(orgtilknytningStateRepository.findByFid(fid)).thenReturn(
            Optional.of(OrgtilknytningState.builder()
                .fid(fid)
                .navident(navident)
                .orgEnhetId(orgEnhet)
                .gyldigFom(LocalDate.now().minusDays(10))
                .gyldigTom(LocalDate.now().plusDays(10))
                .primaryFysiskBrukerstedNummer("2700")
                .build()
            )
        );


        String navidentChangedForNavn = "Ressurs";
        String navidentChangedEtterNavn = "Navn";
        var navidentChangedByVisningsnavn = new Visningsnavn(navidentChangedForNavn, Optional.empty(), navidentChangedEtterNavn);
        when(ressursStateRepository.findByNavident(navidentChangedBy)).thenReturn(
            Optional.of(RessursState.builder()
                .navident(navidentChangedBy)
                .visningsnavn(navidentChangedByVisningsnavn)
                .build()
            )
        );

        when(fysiskBrukerstedService.findFysiskBrukersted("2700"))
            .thenReturn(Optional.of(
                new FysiskBrukersted("2700", "Match avenue 1", null, null, null, null, null)));

        // test
        List<OTEndretData> otEndringerData = List.of(
            OTEndretData.builder()
                .fid(fid)
                .changedByNavIdent(navidentChangedBy)
                .timestamp(timestamp)
                .eventType(OTEndretData.OTEndretDataType.OT_NY)
                .build()
        );

        // verify
        String expected = MessageFormat.format(
            orgtilknytningEndretEmailService.epost_body_with_gyldig_tom_item_template,
            "Ukjent",
            orgEnhet,
            DateUtils.formatDateTime(LocalDate.now().minusDays(10)),
            DateUtils.formatDateTime(LocalDate.now().plusDays(10)),
            "Match avenue 1",
            "2700",
            navidentChangedByVisningsnavn.getFormattedVisningsnavn(),
            timestamp
        );
        if (detaljertEpost) {
            expected += MessageFormat.format(
                orgtilknytningEndretEmailService.epost_body_endringer_utfort_template,
                OTEndretData.OTEndretDataType.OT_NY.beskrivelse
            );
        }
        var result = orgtilknytningEndretEmailService.composeOTChangesMessage(otEndringerData);
        assertEquals(expected, result.trim());
    }

    @Test
    public void testGetRecipientNavidents() {
        // setup data
        String fid = "fid123456";
        String navident = "A123456";
        String navidentChangedBy = "C123456";

        String orgEnhet = "orgenhet123456";
        String orgEnhetNavn = "orgenhetNavn";
        String agressoId = "orgEnhetAgressoId123456";
        String orgNiva = "Nivaa";

        String orgenhetLederNavIdent = "L123456";

        String timestamp = LocalDateTime.now().toString();
        when(orgtilknytningStateRepository.findByFid(fid)).thenReturn(
            Optional.of(OrgtilknytningState.builder()
                .fid(fid)
                .navident(navident)
                .orgEnhetId(orgEnhet)
                .gyldigFom(LocalDate.now().minusDays(10))
                .gyldigTom(LocalDate.now().plusDays(10))
                .build()
            )
        );

        when(orgEnhetStateRepository.findById(orgEnhet)).thenReturn(
            Optional.of(OrgEnhetState.builder()
                .id(orgEnhet)
                .navn(orgEnhetNavn)
                .agressoId(agressoId)
                .agressoOrgNivaa(orgNiva)
                .gyldigFom(LocalDate.now().minusDays(10))
                .gyldigTom(LocalDate.now().plusDays(10))
                .build()
            )
        );

        when(orgEnhetLederService.getAktivLederForOrgEnhet(any())).thenReturn(Optional.of(
            OrgEnhetLederState.builder()
                .fid("FID_1")
                .navident(orgenhetLederNavIdent)
                .orgEnhetId(orgEnhet)
                .startdato(LocalDate.now().minusYears(1))
                .build()
        ));

        // test
        var result = orgtilknytningEndretEmailService.getRecipientNavidents(
            List.of(
                OTEndretData.builder()
                    .fid(fid)
                    .changedByNavIdent(navidentChangedBy)
                    .timestamp(timestamp)
                    .eventType(OTEndretData.OTEndretDataType.OT_ENDRET)
                    .build()
            )
        );

        assertEquals(2, result.size());
        assertTrue(result.contains(navidentChangedBy));
        assertTrue(result.contains(orgenhetLederNavIdent));
    }

    @Test
    public void testComposeOTChangesMessageForSlettetOT() {
        String fid = "fid123456";
        String orgEnhet = "orgenhet123456";
        String timestamp = "02.11.2026";
        when(orgEnhetStateRepository.findById(orgEnhet)).thenReturn(Optional.of(
            OrgEnhetState.builder().id(orgEnhet).navn("Enhet A").build()
        ));

        // Opprettet og slettet samme dag: OT_NY-oppføringen mangler orgEnhetId
        var opprettet = OTEndretData.builder()
            .fid(fid)
            .changedByNavIdent("C123456")
            .timestamp(timestamp)
            .eventType(OTEndretData.OTEndretDataType.OT_NY)
            .build();
        var slettet = opprettet.toBuilder()
            .navident("A123456")
            .orgEnhetId(orgEnhet)
            .eventType(OTEndretData.OTEndretDataType.OT_FJERNET)
            .build();

        var result = orgtilknytningEndretEmailService.composeOTChangesMessage(List.of(opprettet, slettet));

        String expected = MessageFormat.format(
            orgtilknytningEndretEmailService.epost_body_with_fjernet_event_template,
            "Enhet A", orgEnhet, "Ukjent", timestamp
        );
        assertEquals(expected, result.trim());
    }

    @Test
    public void testGetRecipientNavidentsForSlettetOT() {
        String orgEnhet = "orgenhet123456";
        String lederNavident = "L123456";
        when(orgEnhetStateRepository.findById(orgEnhet)).thenReturn(Optional.of(
            OrgEnhetState.builder().id(orgEnhet).navn("Enhet A").build()
        ));
        when(orgEnhetLederService.getAktivLederForOrgEnhet(orgEnhet)).thenReturn(Optional.of(
            OrgEnhetLederState.builder().fid("FID_1").navident(lederNavident).orgEnhetId(orgEnhet).build()
        ));

        // OT-en finnes ikke i state, så findByFid gir Optional.empty()
        var result = orgtilknytningEndretEmailService.getRecipientNavidents(List.of(
            OTEndretData.builder()
                .fid("fid123456")
                .navident("A123456")
                .orgEnhetId(orgEnhet)
                .changedByNavIdent("C123456")
                .eventType(OTEndretData.OTEndretDataType.OT_FJERNET)
                .build()
        ));

        assertEquals(Set.of("C123456", lederNavident), result);
    }

    @Test
    public void testSendOTEndretEmailFjernerEndretOgSlettetOTFraKoen() {
        String navident = "A123456";
        String endretFid = "fid-endret";
        String slettetFid = "fid-slettet";
        when(leaderElection.isLeader()).thenReturn(true);
        when(orgtilknytningStateRepository.findByFid(endretFid)).thenReturn(Optional.of(
            OrgtilknytningState.builder().fid(endretFid).navident(navident).orgEnhetId("orgenhet1").build()
        ));
        // Må stubbes eksplisitt, ellers kaster MockitoExtension PotentialStubbingProblem
        when(orgtilknytningStateRepository.findByFid(slettetFid)).thenReturn(Optional.empty());
        when(cacheQueueService.getOTEndringerFromCache()).thenReturn(Map.of(
            endretFid, List.of(OTEndretData.builder()
                .fid(endretFid)
                .changedByNavIdent("C123456")
                .eventType(OTEndretData.OTEndretDataType.OT_SEKTOR_ENDRET)
                .build()),
            slettetFid, List.of(OTEndretData.builder()
                .fid(slettetFid)
                .navident(navident)
                .orgEnhetId("orgenhet2")
                .changedByNavIdent("C123456")
                .eventType(OTEndretData.OTEndretDataType.OT_FJERNET)
                .build())
        ));
        when(ressursStateRepository.findByNavident(navident)).thenReturn(Optional.of(
            RessursState.builder().navident(navident).build()
        ));
        // Ikke mastered: ingen e-post sendes, men endringene skal likevel ut av køen
        when(nomMastered.isMastered(navident)).thenReturn(false);

        orgtilknytningEndretEmailService.sendOTEndretEmail();

        verify(cacheQueueService).delete(endretFid, CacheType.OTENDRINGER.toString());
        verify(cacheQueueService).delete(slettetFid, CacheType.OTENDRINGER.toString());
    }

    @Test
    public void testDateFormat() {
        // setup data
        String timestamp = DateTimeFormatter.ofPattern("dd.MM.yyyy").format(LocalDate.of(2021, 10, 1));
        assertEquals("01.10.2021", timestamp);
    }

}
