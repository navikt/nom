package no.nav.nom.skjermede.personer.pip.config;

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
    public Docket skjermedePersonerPipApiDoc() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.basePackage("no.nav.nom.skjermede.personer.pip"))
                .build()
                .useDefaultResponseMessages(false)
                .groupName("Skjermede personer pip API")
                .tags(new Tag("skjerming-pip", "PIP tjeneste for skjermede personer"),
                        new Tag("admin", "Admin tjenester"))
                .apiInfo(skjermedePersonerPipApiInfo());
    }

    private ApiInfo skjermedePersonerPipApiInfo() {
        return new ApiInfoBuilder()
                .title("Skjermede personer pip API")
                .description("Beskrivelse kommer ...")
                .version("v1")
                .contact(new Contact(
                        "Team Org",
                        "https://teamkatalog.nais.adeo.no/team/7cb86192-a6e9-42ed-be45-421807c96618",
                        ""
                )).build();
    }
}
