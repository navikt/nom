package no.nav.nom.skjermede.personer.pip;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

public class Skjermingsperiode {

    private final Long fra;
    private final Long til;

    public Skjermingsperiode(LocalDateTime fra) {
        this.fra = fra.toEpochSecond(ZoneOffset.UTC);
        this.til = null;
    }

    public Skjermingsperiode(LocalDateTime fra, LocalDateTime til) {
        this.fra = fra.toEpochSecond(ZoneOffset.UTC);
        if(til == null) {
            this.til = null;
        } else {
            this.til = til.toEpochSecond(ZoneOffset.UTC);
        }
    }

    public LocalDateTime fra()  {
        return LocalDateTime.ofEpochSecond(this.fra, 0, ZoneOffset.UTC);
    }

    public LocalDateTime til() {
        if(this.til == null) {
            return null;
        } else {
            return LocalDateTime.ofEpochSecond(this.til, 0, ZoneOffset.UTC);
        }
    }
}
