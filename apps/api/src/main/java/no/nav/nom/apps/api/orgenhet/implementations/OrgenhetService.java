package no.nav.nom.apps.api.orgenhet.implementations;

import lombok.RequiredArgsConstructor;
import no.nav.nom.apps.api.orgenhet.contract.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.contract.IOrgenhetService;
import no.nav.nom.apps.api.orgenhet.implementations.postgresql.EventBasedOrgenhetService;
import no.nav.nom.contract.nom.api.graphql.Organisasjonsenhet;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Primary
@Service
@RequiredArgsConstructor
public class OrgenhetService implements IOrgenhetService {

    private final EventBasedOrgenhetService orgenhetHendelseRepository;


    @Override
    public Optional<IOrgenhet> getByFid(UUID fid) {
        return orgenhetHendelseRepository.getByFid(fid);
    }

    @Override
    public Optional<IOrgenhet> getEnhetByAgressoIdNivaAndName(String agressoId, String orgNiv, String name) {
        return name != null ? orgenhetHendelseRepository.getEnhetByAgressoIdNivaAndName(agressoId, orgNiv, name)
                : getEnhetByAgressoIdAndOrgniv(agressoId, orgNiv);
    }

    @Override
    public Optional<IOrgenhet> getEnhetByAgressoIdAndOrgniv(String agressoId, String orgNiv) {
        return orgenhetHendelseRepository.getEnhetByAgressoIdAndOrgniv(agressoId, orgNiv);
    }

    @Override
    public List<IOrgenhet> findEnheterByAgressoIdIn(List<String> agressoIds) {
        return orgenhetHendelseRepository.findEnheterByAgressoIdIn(agressoIds);
    }

    @Override
    public Collection<IOrgenhet> getAllEnheter() {
        return orgenhetHendelseRepository.getAllEnheter();
    }

    @Override
    public List<IOrgenhet> matchByAgressoId(String agressoId) {
        return orgenhetHendelseRepository.matchByAgressoId(agressoId);
    }

    @Override
    public List<IOrgenhet> matchByNavn(String navn) {
        return orgenhetHendelseRepository.matchByNavn(navn);
    }

    @Override
    public Organisasjonsenhet convertToGraphQlResponse(IOrgenhet orgenhet) {
        return Organisasjonsenhet.builder()
                .id(orgenhet.getFid().toString())
                .agressoId(orgenhet.getAgressoId())
                .orgNiv(orgenhet.getOrgNiv())
                .navn(orgenhet.getNavn())
                .agressoOrgenhetType(orgenhet.getAgressoOrgenhetType())
                .gyldigFom(orgenhet.getGyldigFom())
                .gyldigTom(orgenhet.getGyldigTom().orElse(null))
                .build();
    }
}
