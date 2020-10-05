package no.nav.nom.apps.nom.ressurser.api.v1;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.nom.ressurser.common.TokenUtil;
import no.nav.nom.apps.nom.ressurser.ressurser.Ressurs;
import no.nav.nom.apps.nom.ressurser.ressurser.RessursRepository;
import no.nav.security.token.support.core.api.ProtectedWithClaims;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Apiet er ment til skjerming-ui på nav.no.
 */

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/skjerming-ui/ressurs", produces = MediaType.APPLICATION_JSON_VALUE)
@ProtectedWithClaims(issuer = "selvbetjening", claimMap = { "acr=Level4" })
public class SkjermingUiController {

    public static final String UNAUTHORIZED_MESSAGE = "Tilgang nektet";

    private final RessursRepository ressursRepository;
    private final TokenUtil tokenUtil;


    @GetMapping("siste")
    public Map<String, LocalDate> sisteAnsattforhold(@RequestHeader String personident,
                                                     @RequestParam(required = false) List<String> ressursType) {
        validerTilgang();
        if (personident == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Oppgi personident i header.");
        }

        ressursType.replaceAll(String::toUpperCase);
        val ressursList = ressursRepository.findAllByPersonident(personident)
                .stream()
                .filter(ressursen -> ressursType.contains(ressursen.getRessurstype().toString()))
                .collect(Collectors.toList());

        val ressurs = ressursList.stream()
                .filter(r -> r.getSluttdato() == null)
                .findAny()
                .orElseGet(() -> ressursList.stream().max(Comparator.comparing(Ressurs::getSluttdato)).orElse(null));

        // Map støtter ikke null keys eller values, må derfor brukes  hashmap:
        val response = ressursList.stream()
                .filter(r -> (ressurs.getSluttdato() == null) ? r.getSluttdato() == null : r.getSluttdato().isEqual(ressurs.getSluttdato()))
                .min(Comparator.comparing(Ressurs::getStartdato));

        if(response.isPresent()){
            HashMap<String, LocalDate> hashMap = new HashMap<>();
            hashMap.put("startdato", response.get().getStartdato());
            hashMap.put("sluttdato", response.get().getSluttdato());
         return hashMap;
        }else {
            return null;
        }
    }

    private void validerTilgang() {
        if (!tokenUtil.isAuthorized()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, UNAUTHORIZED_MESSAGE);
        }
    }
}
