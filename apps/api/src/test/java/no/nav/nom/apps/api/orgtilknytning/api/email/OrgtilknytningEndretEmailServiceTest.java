package no.nav.nom.apps.api.orgtilknytning.api.email;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import no.nav.nom.apps.api.common.DateUtils;
import no.nav.nom.libs.gql.resolvers.LederResolver;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhetleder.OrgEnhetLederService;
import no.nav.nom.contract.nom.api.graphql.OrgEnhetsLeder;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.OrgEnhetLederState;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith({SpringExtension.class})
@ContextConfiguration(classes = {Configuration.class})
public class OrgtilknytningEndretEmailServiceTest {

    Map<String,String> data = new HashMap<>();

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


    @Test
    public void testComposeOTChangesMessage() {

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
        String expected = MessageFormat.format(orgtilknytningEndretEmailService.epost_body_item_template,"Ukjent", orgEnhet, DateUtils.formatDateTime(LocalDate.now().minusDays(10)), DateUtils.formatDateTime(LocalDate.now().plusDays(10)), navidentChangedByVisningsnavn.getFormattedVisningsnavn(), timestamp);
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
    public void testDateFormat() {
        // setup data
        String timestamp = DateTimeFormatter.ofPattern("dd.MM.YYYY").format(LocalDate.of(2021, 10, 1));
        assertEquals("01.10.2021", timestamp);

    }

    }
