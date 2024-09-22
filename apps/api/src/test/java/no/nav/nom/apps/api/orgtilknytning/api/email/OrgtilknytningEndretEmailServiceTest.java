package no.nav.nom.apps.api.orgtilknytning.api.email;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

@ActiveProfiles("test")
@ExtendWith({SpringExtension.class})
@ContextConfiguration(classes = {Configuration.class})
public class OrgtilknytningEndretEmailServiceTest {

    Map<String,String> data = new HashMap<>();

    @Autowired
    private Configuration configuration;

    @InjectMocks
    private OrgtilknytningEndretEmailService orgtilknytningEndretEmailService;

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
    }

    @Test
    public void testGetReceiptionNavidents() {
        // setup data
    }
}
