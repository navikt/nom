package no.nav.nom.skjermede.personer.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.service.Tag;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

@EnableWebMvc
@Configuration(proxyBeanMethods = false)
public class SwaggerConfig {

    @Bean
    public Docket skjermedePersonerApiDoc() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.basePackage("no.nav.nom.skjermede.personer.skjerming.api.v1"))
                .build()
                .useDefaultResponseMessages(false)
                .groupName("Skjermede personer API")
                .tags(
                        new Tag("skjerming", "Tilrettelegger skjermingsopplysninger for b2b konsumenter"),
                        new Tag("skjerming-ui", "Tilrettelegger forvaltning og oppslag av skjermingsopplysninger for selvbetjent b2c løsning på nav.no"),
                        new Tag("skjermingdata", "Tilrettelegger forvaltning av testdata fra dolly"))
                .apiInfo(skjermedePersonerApiInfo());
    }

    @Bean
    public Docket foresporselApiDoc() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.basePackage("no.nav.nom.skjermede.personer.foresporsel.api.v1"))
                .build()
                .useDefaultResponseMessages(false)
                .groupName("Forespørsel API")
                .tags(new Tag("Forespørsel", "Tilrettelegger oppreting, oppslag og sletting av skjermingsforespørsler"))
                .apiInfo(skjermedePersonerApiInfo());
    }

    private ApiInfo skjermedePersonerApiInfo() {
        return new ApiInfoBuilder()
                .title("Skjermede personer API")
                .description("Beskrivelse kommer ...")
                .version("v1")
                .contact(new Contact(
                        "Team Org",
                        "https://teamkatalog.nais.adeo.no/team/7cb86192-a6e9-42ed-be45-421807c96618",
                        ""
                )).build();
    }

    private ApiInfo foresporselApiInfo() {
        return new ApiInfoBuilder()
                .title("Foresporsel API")
                .description("Beskrivelse kommer ...")
                .version("v1")
                .contact(new Contact(
                        "Team Org",
                        "https://teamkatalog.nais.adeo.no/team/7cb86192-a6e9-42ed-be45-421807c96618",
                        ""
                )).build();
    }
}
