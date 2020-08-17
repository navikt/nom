package no.nav.nom.skjermede.personer.pip;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;

@Api(tags = "admin")
@RestController
@RequestMapping("/admin")
@AllArgsConstructor
public class AdminController {
    private final Cache cache;

    @ApiResponses({
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 403, message = "Feil api nøkkel"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @ApiOperation(value = "Henter cache størrelsen - antall skjerminger i cachen")
    @GetMapping(value = "/size")
    public int isSkjermet(@ApiParam(value = "Api nøkkel", required = true) @RequestParam String apiKey) {
        validate(apiKey);
        return cache.size();
    }

    @ApiResponses({
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 403, message = "Feil api nøkkel"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @ApiOperation(value = "Henter cachen - alle skjerminger")
    @GetMapping(value = "/get", produces = MediaType.APPLICATION_JSON_VALUE)
    public HashMap<String, Skjermingsperiode> getAll(@ApiParam(value = "Api nøkkel", required = true) @RequestParam String apiKey) {
        validate(apiKey);
        return cache.getAll();
    }

    private void validate(String apiKey) throws RuntimeException {
        if(!apiKey.equals("key")) { throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong API key"); }
    }
}
