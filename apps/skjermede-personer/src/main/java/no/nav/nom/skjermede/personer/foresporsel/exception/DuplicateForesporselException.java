package no.nav.nom.skjermede.personer.foresporsel.exception;


import lombok.Getter;
import no.nav.nom.skjermede.personer.common.exception.SkjermedePersonerException;
import org.springframework.http.HttpStatus;

@Getter
public class DuplicateForesporselException extends SkjermedePersonerException {

    public DuplicateForesporselException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
