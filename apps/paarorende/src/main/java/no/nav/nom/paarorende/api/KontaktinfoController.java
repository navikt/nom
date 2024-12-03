package no.nav.nom.paarorende.api;


import no.nav.nom.paarorende.common.SecurityUtil;
import no.nav.nom.paarorende.jpa.KontaktinfoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;


@RestController
@RequestMapping(KontaktinfoController.BASE_URL)
public class KontaktinfoController {

    public final static String BASE_URL = "/api/kontaktinfo";

    private final KontaktinfoRepository kontaktInfoRepository;
    private final SecurityUtil securityUtil;

    public KontaktinfoController(KontaktinfoRepository kontaktInfoRepository, SecurityUtil securityUtils) {
        this.kontaktInfoRepository = kontaktInfoRepository;
        this.securityUtil = securityUtils;
    }

    @GetMapping
    public List<KontaktinfoDTO> getAllKontaktinfo() {
        var navident = securityUtil.getNavIdent();

        return KontaktinfoDTO.fromEntityList(kontaktInfoRepository.findAllByNavident(navident));
    }

    @PutMapping
    public KontaktinfoDTO oppdaterKontakinfo(@RequestBody KontaktinfoDTO kontaktInfoDTO) {
        var kontaktInfo = kontaktInfoRepository.findDistinctByKontakId(kontaktInfoDTO.kontaktId())
            .orElseThrow(() -> new AccessDeniedException("Unauthorized"));
        kontaktInfo.setKontakId(kontaktInfoDTO.kontaktId());

        securityUtil.assertAuthorized(kontaktInfo.getNavident());

        return KontaktinfoDTO.fromEntity(kontaktInfoRepository.save(kontaktInfo));
    }

    @PostMapping
    public ResponseEntity<KontaktinfoDTO> opprettPaarorende(@RequestBody KontaktinfoDTO kontaktInfoDTO) {
        securityUtil.assertAuthorized();

        var kontakInfo = KontaktinfoDTO.toEntity(kontaktInfoDTO);
        kontakInfo.setNavident(securityUtil.getNavIdent());

        var response = KontaktinfoDTO.fromEntity(kontaktInfoRepository.save(kontakInfo));

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{kontaktId}")
    public void markForDeletion(@PathVariable String kontaktId) throws AccessDeniedException {
        var kontaktinfo = kontaktInfoRepository.findDistinctByKontakId(kontaktId)
            .orElseThrow(() -> new AccessDeniedException("Unauthorized"));

        securityUtil.assertAuthorized(kontaktinfo.getNavident());

        kontaktinfo.setDatoSlettet(LocalDate.now());

        kontaktInfoRepository.save(kontaktinfo);
    }
}
