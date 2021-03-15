package no.nav.nom.skjermede.personer.pip.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.AllArgsConstructor;
import lombok.val;
import no.nav.nom.skjermede.personer.pip.Cache;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Api(tags = "skjerming-pip")
@RestController
@AllArgsConstructor
public class PipController {
    private final String MISSING_PERSONIDENT = "Personident mangler";
    private final Cache cache;

    @Deprecated
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

    @ApiResponses({
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @ApiOperation(
            value = "Sjekker hvorvidt oppgitt person er skjermet",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PostMapping("/skjermet")
    public boolean isSkjermetPost(@ApiParam(value = "Fødselsnummeret til personen", required = true) @RequestBody final SkjermetDataRequestDTO requestDTO) {
        val personident = requestDTO.getPersonident();
        if(personident == null){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, MISSING_PERSONIDENT);
        }
        return cache.isSkjermet(personident);
    }
}
