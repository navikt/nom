package no.nav.nom.skjermede.personer.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class UnauthorizedOperationException extends SkjermedePersonerException {

    public UnauthorizedOperationException(final String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
