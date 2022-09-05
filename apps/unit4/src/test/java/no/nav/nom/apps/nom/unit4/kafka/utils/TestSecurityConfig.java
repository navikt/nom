package no.nav.nom.apps.nom.unit4.kafka.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
@Profile("test")
public class TestSecurityConfig {

//    @Bean
//    @Profile("test")
//    JwtAuthenticationProvider jwtAuthenticationProvider(JwtDecoder jwtDecoder) {
//        return new JwtAuthenticationProvider(jwtDecoder);
//    }
//
//    @Bean
//    @Profile("test")
//    JwtDecoder jwtDecoder(Environment environment, OAuth2ResourceServerProperties oAuth2ResourceServerProperties){
//        val audience = new HashSet<String>(List.of(environment.getRequiredProperty("spring.security.oauth2.resourceserver.jwt.accepted-audience", String[].class)));
//        return JwtUtils.jwtDecoder(oAuth2ResourceServerProperties, audience);
//    }

//    @Bean
//    @Profile("test")
//    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
//        http.
//        return http.build();
//    }
}
