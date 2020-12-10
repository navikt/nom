package no.nav.nom.skjermede.personer.skjerming.api.v1;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.skjermede.personer.skjerming.SkjermetPersonRepository;
import no.nav.nom.skjermede.personer.skjerming.api.v1.dto.SkjermetPersonResponseDTO;
import no.nav.security.token.support.core.api.ProtectedWithClaims;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@Api(tags = "skjerming")
@RequiredArgsConstructor
@RequestMapping(value = SkjermedePersonerController.BASE_URL, produces = MediaType.APPLICATION_JSON_VALUE)
@ProtectedWithClaims(issuer = "reststs")
public class SkjermedePersonerController {

    public static final String BASE_URL = "/api/v1/skjerming";

    private final SkjermetPersonRepository skjermetPersonRepository;

    @GetMapping("alle")
    @ApiResponses({
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @ApiImplicitParam(
            paramType = "header", name = "Authorization",
            value = "\"Bearer \" +  sts utstedt access token", required = true)
    @ApiOperation(value = "Henter en alle aktive skjerminger", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<SkjermetPersonResponseDTO> hentAlleSkjerminger() {
        return skjermetPersonRepository
                .findAllActive()
                .stream()
                .map(SkjermetPersonResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

}
