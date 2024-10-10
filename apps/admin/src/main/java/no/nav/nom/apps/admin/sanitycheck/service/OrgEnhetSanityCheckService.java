package no.nav.nom.apps.admin.sanitycheck.service;

import no.nav.nom.apps.admin.sanitycheck.dto.OrgEnhetStartOgSluttdato;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrgEnhetSanityCheckService {

    private final OrgEnhetStateRepository orgEnhetStateRepository;

    public OrgEnhetSanityCheckService(OrgEnhetStateRepository orgEnhetStateRepository) {
        this.orgEnhetStateRepository = orgEnhetStateRepository;
    }

    public OrgEnhetStartOgSluttdato sjekkOrgEnhetStartSluttDato() {
        List<OrgEnhetState> alleEnheter = orgEnhetStateRepository.getAll().stream().toList();

        return new OrgEnhetStartOgSluttdato(
            alleEnheter.size(),
            enheterMedStartdato(alleEnheter).size(),
            enheterMedSluttdato(alleEnheter).size(),
            enheterMedSluttdatoFoerStartdato(enheterMedSluttdato(alleEnheter)).size()
        );
    }

    private List<OrgEnhetState> enheterMedStartdato(List<OrgEnhetState> enheter) {
        return enheter.stream().filter(oe -> oe.gyldigFom() != null).toList();
    }

    private List<OrgEnhetState> enheterMedSluttdato(List<OrgEnhetState> enheter) {
        return enheter.stream().filter(oe -> oe.gyldigTom().isPresent()).toList();
    }

    private List<OrgEnhetState> enheterMedSluttdatoFoerStartdato(List<OrgEnhetState> enheter) {
        return enheter.stream().filter(oe -> oe.gyldigTom().isPresent()).filter(oe -> oe.gyldigTom().get().isBefore(oe.gyldigFom())).toList();
    }
}
