package no.nav.nom.apps.ui.common;

import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Component
class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // map / til /index.html
        registry.addViewController("/").setViewName("forward:/index.html");
        // map /xxxxx osv til /index.html, altså single directory
        registry.addViewController("/{x:[\\w\\-]+}").setViewName("forward:/index.html");
        // map /xxxxx/xxxx o.l. ekskludert api og alle . extensions slik som .js, .css o.l..
        registry.addViewController("{x:^(?!api$).*$}/**/{y:[\\w\\-]+}").setViewName("forward:/index.html");//
    }
}
