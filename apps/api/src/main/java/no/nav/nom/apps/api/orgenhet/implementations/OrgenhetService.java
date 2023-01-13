package no.nav.nom.apps.api.orgenhet.implementations;

import io.getunleash.Unleash;
import no.nav.nom.apps.api.hendelse.contract.HendelseDataType;
import no.nav.nom.apps.api.hendelse.contract.IHendelse;
import no.nav.nom.apps.api.orgenhet.contract.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.contract.IOrgenhetService;
import no.nav.nom.apps.api.orgenhet.implementations.postgresql.EventBasedOrgenhet;
import no.nav.nom.apps.api.postgresql.GenericStorageElement;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;

@Service
public class OrgenhetService implements IOrgenhetService {
    private final OrgenhetServiceOld orgenhetServiceOld;
    private final OrgenhetServiceNewFromEvtsrc orgenhetServiceNewFromEvtsrc;

    public static final String UNLEASH_TOGGLE = "nom.orgenhet.eventsourcing";
    private final Unleash unleash;

    public OrgenhetService(OrgenhetServiceOld orgenhetServiceOld, OrgenhetServiceNewFromEvtsrc orgenhetServiceNewFromEvtsrc, Unleash unleash) {
        this.orgenhetServiceOld = orgenhetServiceOld;
        this.orgenhetServiceNewFromEvtsrc = orgenhetServiceNewFromEvtsrc;
        this.unleash = unleash;
    }

    @Override
    public Optional<IOrgenhet> getById(String id) {
        return unleash.isEnabled(UNLEASH_TOGGLE) ?
            orgenhetServiceNewFromEvtsrc.getById(id) :
            orgenhetServiceOld.getById(id);
    }

    @Override
    public Optional<IOrgenhet> getByFid(String fid) {
        return unleash.isEnabled(UNLEASH_TOGGLE) ?
            orgenhetServiceNewFromEvtsrc.getByFid(fid) :
            orgenhetServiceOld.getByFid(fid);
    }

    @Override
    public Optional<IOrgenhet> getEnhetByAgressoIdNivaAndName(String agressoId, String orgNiv, String name) {
        return unleash.isEnabled(UNLEASH_TOGGLE) ?
            orgenhetServiceNewFromEvtsrc.getEnhetByAgressoIdNivaAndName(agressoId, orgNiv, name) :
            orgenhetServiceOld.getEnhetByAgressoIdNivaAndName(agressoId, orgNiv, name);
    }

    @Override
    public Optional<IOrgenhet> getEnhetByAgressoIdAndOrgniv(String agressoId, String orgNiv) {
        return unleash.isEnabled(UNLEASH_TOGGLE) ?
            orgenhetServiceNewFromEvtsrc.getEnhetByAgressoIdAndOrgniv(agressoId, orgNiv) :
            orgenhetServiceOld.getEnhetByAgressoIdAndOrgniv(agressoId, orgNiv);
    }

    @Override
    public List<IOrgenhet> findEnheterByAgressoIdIn(Set<String> agressoIds) {
        return unleash.isEnabled(UNLEASH_TOGGLE) ?
            orgenhetServiceNewFromEvtsrc.findEnheterByAgressoIdIn(agressoIds) :
            orgenhetServiceOld.findEnheterByAgressoIdIn(agressoIds);
    }

    @Override
    public List<IOrgenhet> findEnheterByAgressoIdConcatOrgnivIn(Set<IOrgenhet.AgressoIdCombo> agressoIdConcatOrgnivSet) {
        return unleash.isEnabled(UNLEASH_TOGGLE) ?
            orgenhetServiceNewFromEvtsrc.findEnheterByAgressoIdConcatOrgnivIn(agressoIdConcatOrgnivSet) :
            orgenhetServiceOld.findEnheterByAgressoIdConcatOrgnivIn(agressoIdConcatOrgnivSet);
    }

    @Override
    public Collection<IOrgenhet> getAllEnheter() {
        return unleash.isEnabled(UNLEASH_TOGGLE) ?
            orgenhetServiceNewFromEvtsrc.getAllEnheter() :
            orgenhetServiceOld.getAllEnheter();
    }

    @Override
    public List<IOrgenhet> matchByAgressoId(String agressoId) {
        return unleash.isEnabled(UNLEASH_TOGGLE) ?
            orgenhetServiceNewFromEvtsrc.matchByAgressoId(agressoId) :
            orgenhetServiceOld.matchByAgressoId(agressoId);
    }

    @Override
    public List<IOrgenhet> matchByNavn(String navn) {
        return unleash.isEnabled(UNLEASH_TOGGLE) ?
            orgenhetServiceNewFromEvtsrc.matchByNavn(navn) :
            orgenhetServiceOld.matchByNavn(navn);
    }

    @Override
    public Map<String, IOrgenhet> getByIdBulk(List<String> idList) {
        return unleash.isEnabled(UNLEASH_TOGGLE) ?
            orgenhetServiceNewFromEvtsrc.getByIdBulk(idList) :
            orgenhetServiceOld.getByIdBulk(idList);
    }

    @Override
    public Map<String, IOrgenhet> getByFidBulk(List<String> fidList) {
        return unleash.isEnabled(UNLEASH_TOGGLE) ?
            orgenhetServiceNewFromEvtsrc.getByFidBulk(fidList) :
            orgenhetServiceOld.getByFidBulk(fidList);
    }
}
