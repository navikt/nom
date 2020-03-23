package no.nav.nom.skjermede.personer.skjerming.api.v1.exception;

import lombok.Getter;
import no.nav.nom.skjermede.personer.common.exception.SkjermedePersonerException;
import org.springframework.http.HttpStatus;

@Getter
public class DuplicateSkjermingException extends SkjermedePersonerException {

    public DuplicateSkjermingException(final String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
