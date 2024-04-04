package no.nav.nom.apps.api.graphql.security;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.MockOAuth2ServerInitializer;
import no.nav.nom.apps.api.graphql.GraphqlTestConfig;
import no.nav.security.mock.oauth2.MockOAuth2Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureHttpGraphQlTester;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@Slf4j
@ActiveProfiles("test")
//@ConfigurationPropertiesScan
//@SpringBootTest(classes = ServletWebServerFactoryAutoConfiguration.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes= {GraphqlTestConfig.class, SecurityTestConfig.class}, initializers = MockOAuth2ServerInitializer.class)
//@WebMvcTest(controllers = {
//    GraphQLOrgEnhetController.class
//    , GraphQLPersonController.class
//    , GraphQLRessursController.class
//    , GraphQLSearchController.class })
@WebMvcTest
@AutoConfigureHttpGraphQlTester
//@ExtendWith(SpringExtension.class)
//@EnableReactiveMethodSecurity(useAuthorizationManager=true)
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = false, jsr250Enabled = true)
abstract class SecurityTestBase {
    @Autowired
    MockOAuth2Server mockOAuth2Server;
//    @Autowired
//    MockMvc mockMvc;
    @Autowired
    HttpGraphQlTester httpGraphQlTester;
//    @Autowired
//    GraphQlTester graphQlTester;
}
