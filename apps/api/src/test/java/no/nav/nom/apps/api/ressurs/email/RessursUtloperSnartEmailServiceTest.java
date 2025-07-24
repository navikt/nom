package no.nav.nom.apps.api.ressurs.email;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import no.nav.nom.apps.api.common.DateUtils;
import no.nav.nom.apps.api.orgenhet.api.app.OrgEnhetMapper;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
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
import java.time.LocalDate;
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
public class RessursUtloperSnartEmailServiceTest {

    Map<String, String> data = new HashMap<>();

    @Autowired
    private Configuration configuration;

    @InjectMocks
    private RessursUtloperSnartEmailService ressursUtlopersnartEmailService;

    @Mock
    private OrgtilknytningStateRepository orgtilknytningStateRepository;

    @Mock
    private OrgEnhetStateRepository orgEnhetStateRepository;

    @Mock
    private RessursStateRepository ressursStateRepository;

    @Mock
    private OrgEnhetMapper orgEnhetMapper;

    @Test
    void testFreemarkerTemplate() throws TemplateException, IOException {
        configuration.setClassForTemplateLoading(this.getClass(), "/templates/");
        // setup data
        data.put("orgenhetNavn", "orgenhetNavn");
        data.put("ressurNavnOgNavIdent", "ressurNavnOgNavIdent");
        data.put("ressursNavn", "ressursNavn");
        data.put("ressursSluttdato", "ressursSluttdato");
        data.put("ressursSluttdatoPlus1dag", "ressursSluttdatoPlus1dag");

        // check if autowiring works
        Assertions.assertNotNull(configuration);

        // test if loading template works
        Template template = configuration.getTemplate("epostvarsel-utlopsdato.ftlh");    // <-- load the template
        // check if template is loaded
        Assertions.assertNotNull(template);

        // dump the generated output to System.err
        var stringWriter = new StringWriter();
        template.process(data, stringWriter);
        System.err.println(stringWriter.getBuffer().toString());
    }

    @Test
    public void testGetUtlopendeRessurser_OT_has_same_gyldigTom_then_ressurs_sluttdato() {
        // setup data
        String ressusNavIdent = "ressurNavnOgNavIdent";
        String ressursForNavn = "ressursForNavn";
        String ressursEtterNavn = "ressursForNavn";
        Visningsnavn vis1 = new Visningsnavn(ressursForNavn, Optional.empty(), ressursEtterNavn);
        LocalDate ressursSluttdato = LocalDate.now().plusWeeks(2);

        String ressursNavIdent2 = "ressurNavnOgNavIdent2";
        String ressursForNavn2 = "ressursForNavn2";
        String ressursEtterNavn2 = "ressursEtterNavn2";
        Visningsnavn vis2 = new Visningsnavn(ressursForNavn2, Optional.empty(), ressursEtterNavn2);
        LocalDate ressursSluttdato2 = LocalDate.now().plusWeeks(5);

        String orgEnhet = "orgenhet123456";
        String orgEnhetNavn = "orgenhetNavn";
        String agressoId = "orgEnhetAgressoId123456";
        String orgNiva = "Nivaa";
        String orgenhetLederNavIdent = "L123456";

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

        when(orgEnhetMapper.hentOrgEnhetLedereNavident(any(), any(), any())).thenReturn(List.of("L123456"));

        when(orgtilknytningStateRepository.findByNavident(ressusNavIdent)).thenReturn(
            List.of(
                OrgtilknytningState.builder()
                    .erDagligOppfolging(true)
                    .navident(ressusNavIdent)
                    .orgEnhetId(orgEnhet)
                    .gyldigFom(ressursSluttdato.minusYears(5))
                    .gyldigTom(Optional.of(ressursSluttdato))
                    .build()
            )
        );
        when(orgtilknytningStateRepository.findByNavident(ressursNavIdent2)).thenReturn(
            List.of(
                OrgtilknytningState.builder()
                    .erDagligOppfolging(true)
                    .navident(ressursNavIdent2)
                    .orgEnhetId(orgEnhet)
                    .gyldigFom(ressursSluttdato2.minusYears(5))
                    .gyldigTom(Optional.of(ressursSluttdato2))
                    .build()
            )
        );

        when(ressursStateRepository.findAllActiveRessurser()).thenReturn(
            Map.of(
                "ressurs1", RessursState.builder()
                    .navident(ressusNavIdent)
                    .visningsnavn(vis1)
                    .sluttdato(ressursSluttdato)
                    .build(),
                "ressurs2", RessursState.builder()
                    .navident(ressursNavIdent2)
                    .visningsnavn(vis2)
                    .sluttdato(ressursSluttdato2)
                    .build()
            )
        );

        // test
        var result = ressursUtlopersnartEmailService.getUtlopendeRessurser();

        // verify
        var ressursEn = RessursUtloperData.builder()
            .ressurNavn(vis1.getFormattedVisningsnavn())
            .ressursNavident(ressusNavIdent)
            .ressursSluttdato(ressursSluttdato)
            .orgenhetNavn(orgEnhetNavn)
            .orgenhetLederNavident(orgenhetLederNavIdent)
            .build();
        var ressursTwo = RessursUtloperData.builder()
            .ressurNavn(vis2.getFormattedVisningsnavn())
            .ressursNavident(ressursNavIdent2)
            .ressursSluttdato(ressursSluttdato2)
            .orgenhetNavn(orgEnhetNavn)
            .orgenhetLederNavident(orgenhetLederNavIdent)
            .build();
        var expected = List.of(ressursEn, ressursTwo);


        assertEquals(expected.size(), result.size());
        assertTrue(expected.containsAll(result));
        assertTrue(result.containsAll(expected));
    }

    @Test
    public void testGetUtlopendeRessurser_OT_has_different_gyldigTom_then_ressurs_sluttdato() {
        // setup data
        String ressusNavIdent = "ressurNavnOgNavIdent";
        String ressursForNavn = "ressursForNavn";
        String ressursEtterNavn = "ressursForNavn";
        Visningsnavn vis1 = new Visningsnavn(ressursForNavn, Optional.empty(), ressursEtterNavn);
        LocalDate ressursSluttdato = LocalDate.now().plusWeeks(2);

        String ressursNavIdent2 = "ressurNavnOgNavIdent2";
        String ressursForNavn2 = "ressursForNavn2";
        String ressursEtterNavn2 = "ressursEtterNavn2";
        Visningsnavn vis2 = new Visningsnavn(ressursForNavn2, Optional.empty(), ressursEtterNavn2);
        LocalDate ressursSluttdato2 = LocalDate.now().plusWeeks(5);

        String orgEnhet = "orgenhet123456";
        String orgEnhetNavn = "orgenhetNavn";
        String agressoId = "orgEnhetAgressoId123456";
        String orgNiva = "Nivaa";

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

        when(orgEnhetMapper.hentOrgEnhetLedere(any(), any(), any())).thenReturn(List.of(
                RessursState.builder()
                    .visningsnavn(new Visningsnavn("Leder", Optional.empty(), "Ledersen"))
                    .navident("L123456")
                    .build()
            )
        );

        when(orgtilknytningStateRepository.findByNavident(ressusNavIdent)).thenReturn(
            List.of(
                OrgtilknytningState.builder()
                    .erDagligOppfolging(true)
                    .navident(ressusNavIdent)
                    .orgEnhetId(orgEnhet)
                    .gyldigFom(ressursSluttdato.minusYears(5))
                    .gyldigTom(Optional.of(ressursSluttdato.minusDays(7)))
                    .build()
            )
        );
        when(orgtilknytningStateRepository.findByNavident(ressursNavIdent2)).thenReturn(
            List.of(
                OrgtilknytningState.builder()
                    .erDagligOppfolging(true)
                    .navident(ressursNavIdent2)
                    .orgEnhetId(orgEnhet)
                    .gyldigFom(ressursSluttdato2.minusYears(5))
                    .gyldigTom(Optional.of(ressursSluttdato2.plusDays(8)))
                    .build()
            )
        );

        when(ressursStateRepository.findAllActiveRessurser()).thenReturn(
            Map.of(
                "ressurs1", RessursState.builder()
                    .navident(ressusNavIdent)
                    .visningsnavn(vis1)
                    .sluttdato(ressursSluttdato)
                    .build(),
                "ressurs2", RessursState.builder()
                    .navident(ressursNavIdent2)
                    .visningsnavn(vis2)
                    .sluttdato(ressursSluttdato2)
                    .build()
            )
        );

        // test
        var result = ressursUtlopersnartEmailService.getUtlopendeRessurser();

        // verify
        List<RessursUtloperData> expected = List.of(
            RessursUtloperData.builder()
                .ressurNavn(vis1.getFormattedVisningsnavn())
                .ressursNavident(ressusNavIdent)
                .ressursSluttdato(ressursSluttdato)
                .build(),
            RessursUtloperData.builder()
                .ressurNavn(vis2.getFormattedVisningsnavn())
                .ressursNavident(ressursNavIdent2)
                .ressursSluttdato(ressursSluttdato2)
                .build()
        );

        assertEquals(2, expected.size());
        assertTrue(result.containsAll(expected));
        assertTrue(expected.containsAll(result));
    }

    @Test
    public void testUtloperDato() {
        String utloperDato = "2021-09-01";
        String formattedUtloperDato = DateUtils.formatDateTime(LocalDate.parse(utloperDato));
        assertEquals("01.09.2021", formattedUtloperDato);

        String utloperDato2 = "2024-12-31";
        String formattedUtloperDato2 = DateUtils.formatDateTime(LocalDate.parse(utloperDato2));
        assertEquals("31.12.2024", formattedUtloperDato2);
    }
}

