package no.nav.nom.apps.api.ressurs.email;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.Configuration;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ActiveProfiles("test")
@ExtendWith({SpringExtension.class})
@ContextConfiguration(classes = {Configuration.class})
public class RessursOpprettetEmailServiceTest {

    Map<String,String> data = new HashMap<>();

    @Autowired
    private Configuration configuration;

    @InjectMocks
    private RessursOpprettetEmailService ressursOpprettetEmailService;

    @Test
    void testFreemarkerTemplate() throws TemplateException, IOException {
        configuration.setClassForTemplateLoading(this.getClass(), "/templates/");
        // setup data
        data.put("resursName", "Resurs Name");
        data.put("navIdent", "A123456");
        data.put("orgenhetName", "My Orgenhet");

        // check if autowiring works
        Assertions.assertNotNull(configuration);

        // test if loading template works
        Template template = configuration.getTemplate("opprettelse-varsel.ftlh");    // <-- load the template
        // check if template is loaded
        Assertions.assertNotNull(template);

        // dump the generated output to System.err
        var stringWriter = new StringWriter();
        template.process(data, stringWriter);
        System.err.println(stringWriter.getBuffer().toString());
    }

    @Test
    void test_appendAdditionalRecipients(){
        List<String> recipients = new ArrayList<>();
        recipients.add("a@b.com");
        recipients.add("a1@b.com");

        List<String> additionalRecipients = new ArrayList<>();
        additionalRecipients.add("b@c.com");
        additionalRecipients.add("a1@b.com");
        additionalRecipients.add("A1@b.com");

        List<String> updatedList = ressursOpprettetEmailService.appendAdditionalRecipientsEmails(recipients, additionalRecipients);
        assertThat(recipients.size()).isEqualTo(2);
        assertThat(additionalRecipients.size()).isEqualTo(3);
        assertThat(updatedList.size()).isEqualTo(3);

        assertThat(updatedList.containsAll(recipients)).isTrue();
    }

    @Test
    void test_appendAuthenticatedUserToRecipientsList() {
        List<String> recipientNavIdents = new ArrayList<>();
        recipientNavIdents.add("A123456");
        recipientNavIdents.add("B123456");
        Authentication authentication = Mockito.mock(Authentication.class);
        Mockito.when(authentication.getPrincipal()).thenReturn(getMockPrincipal());

        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        Set<String> updatedList = ressursOpprettetEmailService.appendAuthenticatedUserToRecipientNavIdents(Set.copyOf(recipientNavIdents));
        assertThat(recipientNavIdents.size()).isEqualTo(2);
        assertThat(updatedList.size()).isEqualTo(3);
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

