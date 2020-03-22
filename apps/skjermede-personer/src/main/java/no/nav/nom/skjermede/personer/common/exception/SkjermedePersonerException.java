package no.nav.nom.skjermede.personer.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class SkjermedePersonerException extends RuntimeException {
    private HttpStatus status;

    public SkjermedePersonerException(final String message, final HttpStatus status) {
        super(message);
        this.status = status;
    }
}
