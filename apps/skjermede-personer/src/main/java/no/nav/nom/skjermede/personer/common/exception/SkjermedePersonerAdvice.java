package no.nav.nom.skjermede.personer.common.exception;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.skjermede.personer.common.exception.ErrorResponseDTO;
import no.nav.nom.skjermede.personer.common.exception.SkjermedePersonerException;
import no.nav.nom.skjermede.personer.common.exception.UnauthorizedOperationException;
import no.nav.nom.skjermede.personer.foresporsel.exception.DuplicateForesporselException;
import no.nav.nom.skjermede.personer.skjerming.api.v1.exception.DuplicateSkjermingException;
import no.nav.nom.skjermede.personer.skjerming.api.v1.exception.SkjermingNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
public class SkjermedePersonerAdvice {
    @ExceptionHandler({DuplicateForesporselException.class, SkjermingNotFoundException.class, DuplicateSkjermingException.class, UnauthorizedOperationException.class})
    public <E extends SkjermedePersonerException> ResponseEntity<ErrorResponseDTO> handleException(final E e) {
        log.info(e.getMessage());

        return ResponseEntity.status(e.getStatus()).body(new ErrorResponseDTO(e.getMessage(), e.getStatus().value()));
    }
}
