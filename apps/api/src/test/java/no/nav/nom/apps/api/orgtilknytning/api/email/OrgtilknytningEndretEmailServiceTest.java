package no.nav.nom.apps.api.orgtilknytning.api.email;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import no.nav.nom.apps.api.graphql.organisasjonsenhet.LederResolver;
import no.nav.nom.contract.nom.api.graphql.OrgEnhetsLeder;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
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
    private LederResolver lederResolver;

    @Mock
    private OrgtilknytningStateRepository orgtilknytningStateRepository;

    @Mock
    private OrgEnhetStateRepository orgEnhetStateRepository;

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
        System.err.println(stringWriter.getBuffer().toString());
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
                .gyldigTom(Optional.of(LocalDate.now().plusDays(10)))
                .build()
            )
        );

        // test
        List<OTEndretData> otEndringerData = List.of(
            OTEndretData.builder()
                .fid(fid)
                .changedBy(navidentChangedBy)
                .timestamp(timestamp)
                .eventType(OTEndretData.OTEndretDataType.OT_NY)
                .build()
        );

        // verify
        String expected = MessageFormat.format(orgtilknytningEndretEmailService.epost_body_item_template,orgEnhet, LocalDate.now().minusDays(10), LocalDate.now().plusDays(10), navidentChangedBy, timestamp);
        var result = orgtilknytningEndretEmailService.composeOTChangesMessage(otEndringerData);
        assertEquals(expected, result.trim());
    }

    @Test
    public void testGetReceiptionNavidents() {
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
                .gyldigTom(Optional.of(LocalDate.now().plusDays(10)))
                .build()
            )
        );

        when(orgEnhetStateRepository.findById(orgEnhet)).thenReturn(
            Optional.of(OrgEnhetState.builder()
                .id(orgEnhet)
                .navn(orgEnhetNavn)
                .agressoId(Optional.of(agressoId))
                .agressoOrgNivaa(Optional.of(orgNiva))
                .gyldigFom(LocalDate.now().minusDays(10))
                .gyldigTom(Optional.of(LocalDate.now().plusDays(10)))
                .build()
            )
        );

        when(lederResolver.leder(any())).thenReturn(List.of(
            OrgEnhetsLeder.builder()
                .ressurs(Ressurs.builder()
                    .navIdent(orgenhetLederNavIdent)
                    .build()
                )
                .build()
            )
        );

        // test
        var result = orgtilknytningEndretEmailService.getReceiptionNavidents(
            List.of(
                OTEndretData.builder()
                    .fid(fid)
                    .changedBy(navidentChangedBy)
                    .timestamp(timestamp)
                    .eventType(OTEndretData.OTEndretDataType.OT_ENDRET)
                    .build()
            )
        );

        assertEquals(result.size(), 2);
        assertTrue(result.contains(navidentChangedBy));
        assertTrue(result.contains(orgenhetLederNavIdent));
    }
}
