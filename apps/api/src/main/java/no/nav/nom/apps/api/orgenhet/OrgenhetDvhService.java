package no.nav.nom.apps.api.orgenhet;

import lombok.RequiredArgsConstructor;
import no.nav.nom.apps.api.domain.OrgType;
import no.nav.nom.apps.api.domain.OrgTyper;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.orgenhet.contract.Orgenhet;
import no.nav.nom.apps.api.orgenhet.contract.OrgenhetService;
import no.nav.nom.contract.nom.api.graphql.Kode;
import no.nav.nom.contract.nom.api.graphql.Organisasjonsenhet;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Primary
@Service
@RequiredArgsConstructor
public class OrgenhetDvhService implements OrgenhetService {

    private final DvhRepository dvhRepository;

    @Override
    public Optional<Orgenhet> getEnhetByAgressoIdNivaAndName(String agressoId, String orgNiv, String name) {
        return dvhRepository.getEnhetByAgressoIdAndNiva(agressoId, orgNiv);
    }

    @Override
    public List<Orgenhet> findEnheterByAgressoIdIn(List<String> agressoIds) {
        return dvhRepository.findEnheterByAgressoIdIn(agressoIds);
    }

    @Override
    public Collection<Orgenhet> getAllEnheter() {
        return dvhRepository.getAllEnheter();
    }

    @Override
    public List<Orgenhet> matchByAgressoId(String agressoId) {
        return dvhRepository.getAllEnheter().stream()
                .filter(e -> e.getAgressoId().toUpperCase().startsWith(agressoId.toUpperCase()))
                .toList();
    }

    @Override
    public List<Orgenhet> matchByNavn(String navn) {
        return dvhRepository.getAllEnheter().stream()
                .filter(e -> e.getNavn().toUpperCase().startsWith(navn.toUpperCase()))
                .toList();    }

    @Override
    public Organisasjonsenhet convertToGraphQlResponse(Orgenhet orgenhet) {
        return Organisasjonsenhet.builder()
                .agressoId(orgenhet.getAgressoId())
                .orgNiv(orgenhet.getOrgNiv())
                .navn(orgenhet.getNavn())
                .gyldigFom(orgenhet.getGyldigFom())
                .gyldigTom(orgenhet.getGyldigTom().orElse(null))
                .type(kodeFra(OrgTyper.forAgressoOrgType((((DvhOrganisasjonsenhet) orgenhet).getAgressoOrgType()))))
                .build();
    }

    private Kode kodeFra(OrgType orgType) {
        return orgType == null ? null :
                Kode.builder()
                        .kode(orgType.getKode())
                        .navn(orgType.getNavn())
                        .gyldigFom(orgType.getGyldigFom())
                        .gyldigTom(orgType.getGyldigTom())
                        .build();
    }
}
