package no.nav.nom.apps.admin.research;

import org.springframework.stereotype.Service;

@Service
public class Hello {
    public String hello(String name) {
        return "Hello, " + name;
    }

    public static Hello bean() {
        return ContextWrapper.getContext().getBean(Hello.class);
    }
}
