package no.nav.nom.apps.api.dvh;

import io.getunleash.Unleash;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Component
public class KontrollerFlyttingAvKontaktsenter {
    private final Unleash unleash;
    private final OrgEnhetStateRepository orgenhetStateRepository;

    @Value("${app.kontrollerFlyttingAvKontaktsenter.kontaktsenterId}") private String kontaktsenterId;
    @Value("${app.kontrollerFlyttingAvKontaktsenter.eksisterendeOverId}") private String eksisterendeOverId;
    @Value("${app.kontrollerFlyttingAvKontaktsenter.nyOverId}") private String nyOverId;

    private static final String UNLEASH_BLOCK_UNWANTED_CHANGE_TO_KONTAKTSENTER = "nom-api.dvh_last.block-unwanted-change-to-kontaktsenter";

    public KontrollerFlyttingAvKontaktsenter(Unleash unleash, OrgEnhetStateRepository orgenhetStateRepository) {
        this.unleash = unleash;
        this.orgenhetStateRepository = orgenhetStateRepository;
    }

    public boolean blokkerOpprettAvEnhet(String navn, String agressoId, String orgNiv) {
        if (!unleash.isEnabled(UNLEASH_BLOCK_UNWANTED_CHANGE_TO_KONTAKTSENTER)) {
            return false;
        }

        var ksEnhet = getKontaktsenter();

        var sammeNavnOgAgressoId = Objects.equals(ksEnhet.navn(), navn) && Objects.equals(ksEnhet.agressoId(), agressoId);
        if (sammeNavnOgAgressoId) {
            log.error("ALERT - KontrollerFlyttingAvKontaktsenter: Forsøk på å opprette enhet med navn og AgressoId som kontaktsenteret. {}-{}-{}", agressoId, navn, orgNiv);
        }
        return sammeNavnOgAgressoId;
    }

    public boolean blokkerEndringAvEnhet(String fid) {
        if (!unleash.isEnabled(UNLEASH_BLOCK_UNWANTED_CHANGE_TO_KONTAKTSENTER)) {
            return false;
        }

        var ksEnhet = getKontaktsenter();
        var sammeFid = ksEnhet.fid().equals(fid);
        if (sammeFid) {
            log.error("ALERT - KontrollerFlyttingAvKontaktsenter: Forsøk på å endre enhet med samme fid som kontaktsenteret. FID: {}", fid);
        }
        return sammeFid;
    }

    public boolean blokkerAvslutningAvEnhet(String fid) {
        if (!unleash.isEnabled(UNLEASH_BLOCK_UNWANTED_CHANGE_TO_KONTAKTSENTER)) {
            return false;
        }

        var ksEnhet = getKontaktsenter();
        var sammeFid = ksEnhet.fid().equals(fid);
        if (sammeFid) {
            log.error("ALERT - KontrollerFlyttingAvKontaktsenter: Forsøk på å avslutte enhet med samme fid som kontaktsenteret. FID: {}", fid);
        }
        return sammeFid;
    }



    private OrgEnhetState kontaktsenter;
    public OrgEnhetState getKontaktsenter() {
        if (kontaktsenter == null) {
            kontaktsenter = orgenhetStateRepository.findById(kontaktsenterId)
                .orElseThrow(() -> new RuntimeException("ALERT - KontrollerFlyttingAvKontaktsenter: Kontaktsenter OrgEnhet med id " + kontaktsenterId + " ikke funnet"));
        }
        return kontaktsenter;
    }

    private OrgEnhetState eksisterendeOverEnhet;
    public OrgEnhetState getEksisterendeOverEnhet() {
        if (eksisterendeOverEnhet == null) {
            eksisterendeOverEnhet = orgenhetStateRepository.findById(eksisterendeOverId)
                .orElseThrow(() -> new RuntimeException("ALERT - KontrollerFlyttingAvKontaktsenter: Kontaktsenter eksisterende over-OrgEnhet med id " + eksisterendeOverId + " ikke funnet"));
        }
        return eksisterendeOverEnhet;
    }

    private OrgEnhetState nyOverEnhet;
    public OrgEnhetState getNyOverEnhet() {
        if (nyOverEnhet == null) {
            nyOverEnhet = orgenhetStateRepository.findById(nyOverId)
                .orElseThrow(() -> new RuntimeException("ALERT - KontrollerFlyttingAvKontaktsenter: Kontaktsenter ny over-OrgEnhet med id " + nyOverId + " ikke funnet"));
        }
        return nyOverEnhet;
    }
}
