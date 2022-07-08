package no.nav.nom.apps.api.orgenhet.implementations;

import lombok.RequiredArgsConstructor;
import no.nav.nom.apps.api.orgenhet.contract.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.contract.IOrgenhetService;
import no.nav.nom.apps.api.orgenhet.implementations.dvh.DvhInterface;
import no.nav.nom.contract.nom.api.graphql.Kode;
import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrgenhetService implements IOrgenhetService {

    private final DvhInterface orgenhetDvhInterface;


    @Override
    public Optional<IOrgenhet> getEnhetByAgressoIdNivaAndName(String agressoId, String orgNiv, String name) {
        return orgenhetDvhInterface.getEnhetByAgressoIdNivaAndName(agressoId, orgNiv, name);
    }

    @Override
    public List<IOrgenhet> findEnheterByAgressoIdIn(List<String> agressoIds) {
        return orgenhetDvhInterface.findEnheterByAgressoIdIn(agressoIds);
    }

    @Override
    public Collection<IOrgenhet> getAllEnheter() {
        return orgenhetDvhInterface.getAllEnheter();
    }

    @Override
    public List<IOrgenhet> matchByAgressoId(String agressoId) {
        return orgenhetDvhInterface.matchByAgressoId(agressoId);
    }

    @Override
    public List<IOrgenhet> matchByNavn(String navn) {
        return orgenhetDvhInterface.matchByNavn(navn);
    }

    @Override
    public OrgEnhet convertToGraphQlResponse(IOrgenhet orgenhet) {
        return OrgEnhet.builder()
                .agressoId(orgenhet.getAgressoId())
                .orgNiv(orgenhet.getOrgNiv())
                .navn(orgenhet.getNavn())
                .gyldigFom(orgenhet.getGyldigFom())
                .gyldigTom(orgenhet.getGyldigTom().orElse(null))
                .type(orgenhet.getOrgenhetType() != null ? Kode.builder()
                        .kode(orgenhet.getOrgenhetType().name())
                        .navn(orgenhet.getOrgenhetType().beskrivelse)
                        .gyldigFom(null)
                        .gyldigTom(null)
                        .build() : null
                )
                .build();
    }
}
