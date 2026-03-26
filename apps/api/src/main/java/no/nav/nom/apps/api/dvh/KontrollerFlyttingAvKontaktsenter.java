package no.nav.nom.apps.api.dvh;

import io.getunleash.Unleash;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.dvh.dto.DvhOrganisering;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

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

    public boolean blokkerEndringAvOrganisering(DvhOrganisering dvhOrganisering, IOrgenhet orgEnhet, IOrgenhet orgEnhetOver) {
        if (!unleash.isEnabled(UNLEASH_BLOCK_UNWANTED_CHANGE_TO_KONTAKTSENTER)) {
            return false;
        }

        var ksEnhet = getKontaktsenter();
        var nyOverenhet = getNyOverEnhet();
        var eksisterendeOverenhet = getEksisterendeOverEnhet();

        if (Objects.equals(ksEnhet.fid(), orgEnhet.getFid()) && !(Objects.equals(nyOverenhet.fid(), orgEnhetOver.getFid()) || Objects.equals(eksisterendeOverenhet.fid(), orgEnhetOver.getFid()))) {
            log.error("ALERT - KontrollerFlyttingAvKontaktsenter: Forsøk på å endre organisering av kontaktsenter enhet til annen enhet enn forventet:\n", organiseringToString(dvhOrganisering, orgEnhet, orgEnhetOver));
            return true;
        }
        return false;
    }

    public boolean blokkerAvslutningAvOrganisering(String id) {
        if (!unleash.isEnabled(UNLEASH_BLOCK_UNWANTED_CHANGE_TO_KONTAKTSENTER)) {
            return false;
        }

        var ksEnhet = getKontaktsenter();
        if (Objects.equals(ksEnhet.id(), id)) {
            log.error("ALERT - KontrollerFlyttingAvKontaktsenter: Forsøk på å avslutte organisering av kontaktsenter enhet {}.", id);
            return true;
        }
        return false;
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

    private String organiseringToString(DvhOrganisering dvhOrganisering, IOrgenhet orgEnhet, IOrgenhet orgEnhetOver) {
        return "UnderEnhet: " + orgEnhet.getNavn() + " (" + orgEnhet.getId() + "), OverEnhet: " + orgEnhetOver.getNavn() + " (" + orgEnhetOver.getId() + ")\n" +
            "DvhOrganisering underEnhet: " + dvhOrganisering.uniqueAgressoIdUnder() + ", OverEnhet: " + dvhOrganisering.uniqueAgressoIdOver();
    }

}
