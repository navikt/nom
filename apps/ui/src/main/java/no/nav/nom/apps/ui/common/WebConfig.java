package no.nav.nom.apps.ui.common;

import lombok.val;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Component
class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // map / til /index.html
        registry.addViewController("/").setViewName("forward:/index.html");


        // map /**/x fire nivåer ned
        val pat1 = "/{x:[\\w\\-]+}";
        val pat2 = "/{y:[\\w\\-]+}";
        val pat3 = "/{z:[\\w\\-]+}";
        val pat4 = "/{t:[\\w\\-]+}";

        val viewName = "forward:/index.html";

        registry.addViewController(String.join("", List.of(pat1))).setViewName(viewName);
        registry.addViewController(String.join("", List.of(pat1,pat2))).setViewName(viewName);
        registry.addViewController(String.join("", List.of(pat1,pat2,pat3))).setViewName(viewName);
        registry.addViewController(String.join("", List.of(pat1,pat2,pat3,pat4))).setViewName(viewName);

    }
}
