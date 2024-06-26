package no.nav.nom.apps.api.authorize.email;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@ExtendWith({SpringExtension.class})
@ContextConfiguration(classes = {Configuration.class})
public class TilgangDelegertEmailServiceTest {
    Map<String, String> data = new HashMap<>();

    @Autowired
    private Configuration configuration;

    @Test
    void testFreemarkerTempalte() throws TemplateException, IOException {
        configuration.setClassForTemplateLoading(this.getClass(), "/templates/");
        data.put("lederDisplayname", "Nav Navesen");
        data.put("lederNavIdent", "A123456");
        data.put("delegertRessursDisplayname", "Delegert Navesen");
        data.put("delegertRessursNavIdent", "B123456");
        data.put("tomDato", "2024-12-31");
        data.put("orgenhetNavn", "My Orgenhet");

        Assertions.assertNotNull(configuration);

        Template template = configuration.getTemplate("delegert-tilgang.ftlh");

        Assertions.assertNotNull(template);

        var stringWriter = new StringWriter();
        template.process(data, stringWriter);
        System.err.println(stringWriter.getBuffer().toString());
    }

    @Test
    void test_appendAuthenticatedUserToRecipientsList() {
        List<String> recipientNavIdents = new ArrayList<>();
        recipientNavIdents.add("nav.navesen@nav.no");
        recipientNavIdents.add("delegert.navesen@nav.no");
        List<String> additionalRecipients = new ArrayList<>();
        additionalRecipients.add("delegert.navesen@nav.no");
        additionalRecipients.add("anonym.navesen@nav.no");
        Authentication authentication = Mockito.mock(Authentication.class);
        Mockito.when(authentication.getPrincipal()).thenReturn(getMockPrincipal());

        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        List<String> updatedList = Stream.concat(recipientNavIdents.stream(), additionalRecipients.stream()).distinct().toList();
        assertThat(recipientNavIdents.size()).isEqualTo(2);
        assertThat(updatedList.size()).isEqualTo(3);

        List<String> expectedEmails = Arrays.asList("nav.navesen@nav.no", "delegert.navesen@nav.no", "anonym.navesen@nav.no");
        assertThat(updatedList).containsExactlyInAnyOrderElementsOf(expectedEmails);
    }

    private Jwt getMockPrincipal() {
        Map<String, Object> claims = Map.of(
            "NAVident", "C123456"
        );
        return new Jwt(
            "AUTH0_TOKEN",
            Instant.now(),
            Instant.now().plusSeconds(30),
            Map.of("alg", "none"),
            claims
        );
    }
}