package no.nav.nom.skjermede.personer.pip;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "skjerming-pip")
@RestController
@AllArgsConstructor
public class PipController {
    private final Cache cache;

    @ApiResponses({
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @ApiOperation(
            value = "Sjekker hvorvidt oppgitt person er skjermet",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @GetMapping("/skjermet")
    public boolean isSkjermet(@ApiParam(value = "Fødselsnummeret til personen", required = true) @RequestParam final String personident) {
        return cache.isSkjermet(personident);
    }
}
