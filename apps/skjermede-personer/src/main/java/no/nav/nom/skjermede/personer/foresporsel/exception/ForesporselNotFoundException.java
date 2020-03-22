package no.nav.nom.skjermede.personer.foresporsel.exception;

import lombok.Getter;
import no.nav.nom.skjermede.personer.common.exception.SkjermedePersonerException;
import org.springframework.http.HttpStatus;

@Getter
public class ForesporselNotFoundException extends SkjermedePersonerException {

    public ForesporselNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
