package no.nav.nom.apps.api.ressurs.email;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import no.nav.nom.apps.api.graphql.organisasjonsenhet.LederResolver;
import no.nav.nom.apps.api.orgtilknytning.api.email.OTEndretData;
import no.nav.nom.contract.nom.api.graphql.OrgEnhetsLeder;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
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
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith({SpringExtension.class})
@ContextConfiguration(classes = {Configuration.class})
public class RessursUtlopersnartEmailServiceTest {

    Map<String,String> data = new HashMap<>();

    @Autowired
    private Configuration configuration;

    @InjectMocks
    private RessursUtlopersnartEmailService ressursUtlopersnartEmailService;

    @Mock
    private OrgtilknytningStateRepository orgtilknytningStateRepository;

    @Mock
    private OrgEnhetStateRepository orgEnhetStateRepository;

    @Mock
    private RessursStateRepository ressursStateRepository;

    @Mock
    private LederResolver lederResolver;

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
    public void testGetUtlopendeRessurser() {
        // setup data
        String ressusNavIdent = "ressurNavnOgNavIdent";
        String ressursForNavn = "ressursForNavn";
        String ressursEtterNavn = "ressursForNavn";
        Visningsnavn vis1 = new Visningsnavn(ressursForNavn, Optional.empty(), ressursEtterNavn);
        Optional<LocalDate> ressursSluttdato = Optional.of(LocalDate.now().plusWeeks(2));
        Optional<LocalDate> ressursSluttdatoPlus1dag = Optional.of(LocalDate.now().plusWeeks(2).plusDays(1));

        String ressursNavIdent2 = "ressurNavnOgNavIdent2";
        String ressursForNavn2 = "ressursForNavn2";
        String ressursEtterNavn2 = "ressursEtterNavn2";
        Visningsnavn vis2 = new Visningsnavn(ressursForNavn2, Optional.empty(), ressursEtterNavn2);
        Optional<LocalDate> ressursSluttdato2 = Optional.of(LocalDate.now().plusWeeks(5));
        Optional<LocalDate> ressursSluttdato2Plus1dag = Optional.of(LocalDate.now().plusWeeks(5).plusDays(1));

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

        when(lederResolver.leder(any())).thenReturn(List.of(
                OrgEnhetsLeder.builder()
                    .ressurs(Ressurs.builder()
                        .navIdent(orgenhetLederNavIdent)
                        .build()
                    )
                    .build()
            )
        );

        when(orgtilknytningStateRepository.findByNavident(ressusNavIdent)).thenReturn(
            List.of(
                OrgtilknytningState.builder()
                    .navident(ressusNavIdent)
                    .orgEnhetId(orgEnhet)
                    .build()
            )
        );
        when(orgtilknytningStateRepository.findByNavident(ressursNavIdent2)).thenReturn(
            List.of(
                OrgtilknytningState.builder()
                    .navident(ressursNavIdent2)
                    .orgEnhetId(orgEnhet)
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
        List<UtloperRessursData> expected = List.of(
            UtloperRessursData.builder()
                .ressurNavn(vis1.getFormattedVisningsnavn())
                .ressursNavident(ressusNavIdent)
                .ressursSluttdato(ressursSluttdato.get())
                .orgenhetData(Map.of(orgEnhetNavn, orgenhetLederNavIdent))
                .build(),
            UtloperRessursData.builder()
                .ressurNavn(vis2.getFormattedVisningsnavn())
                .ressursNavident(ressursNavIdent2)
                .ressursSluttdato(ressursSluttdato2.get())
                .orgenhetData(Map.of(orgEnhetNavn, orgenhetLederNavIdent))
                .build()
        );

        assertEquals(expected.size(), result.size());
        assertTrue(expected.containsAll(result));
        assertTrue(result.containsAll(expected));
        assertEquals(expected, result);
    }

}

