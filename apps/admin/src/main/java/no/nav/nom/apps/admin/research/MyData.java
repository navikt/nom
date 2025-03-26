package no.nav.nom.apps.admin.research;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(fluent = true, chain = true)
@Builder(toBuilder = true)
@Getter
@Setter
public class MyData {
    private int id;
    private String name;

    private static final Hello hello = Hello.bean();

    public String hello() {
        return hello.hello(name);
    }
}
