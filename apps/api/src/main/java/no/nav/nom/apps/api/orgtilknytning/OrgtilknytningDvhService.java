package no.nav.nom.apps.api.orgtilknytning;

import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgtilknytning.contract.Orgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.contract.OrgtilknytningService;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrgtilknytningDvhService implements OrgtilknytningService {

    private final DvhRepository dvhRepository;

    private final Map<String, List<Orgtilknytning>> navidentKoblingMap;

    private final Map<IOrgenhet.AgressoIdCombo,List<Orgtilknytning>> agressoIdComboKoblingMap;

    public OrgtilknytningDvhService(DvhRepository dvhRepository) {
        this.dvhRepository = dvhRepository;
        dvhRepository.addUpdateListener(this::updateSelf);
        this.navidentKoblingMap = new HashMap<>();
        this.agressoIdComboKoblingMap = new HashMap<>();
    }

    @Override
    public Collection<OrgtilknytningState> getAllKoblinger() {
        throw new NotImplementedException("Gammel service klasse implementerer ikke nye metoder");
    }

    @Override
    public Collection<OrgtilknytningState> getKoblingerByNavIdent(String navIdent) {
        throw new NotImplementedException("Gammel service klasse implementerer ikke nye metoder");
    }

    @Override
    public <T> Map<String, List<T>> getKoblingerByNavidenter(Set<String> navidenter, Function<OrgtilknytningState, T> transformer) {
        throw new NotImplementedException("Gammel service klasse implementerer ikke nye metoder");
    }

    @Override
    public Collection<Orgtilknytning> getAllKoblingerOld() {
        return this.navidentKoblingMap.values().stream().flatMap(Collection::stream).toList();
    }

    @Override
    public List<Orgtilknytning> getKoblingerByNavIdentOld(String navIdent) {
        var hit = this.navidentKoblingMap.get(navIdent);
        return hit == null ? Collections.emptyList() : hit;
    }

    @Override
    public <T> Map<String, List<T>> getKoblingerByNavidenterOld(Set<String> navidenter, Function<Orgtilknytning, T> transformer) {
        return navidenter.stream().collect(Collectors.toMap(navident -> navident, navident -> {
            var value = this.navidentKoblingMap.get(navident);
            return value == null ? Collections.emptyList() : value.stream().map(transformer).toList();
        }));
    }

    private boolean updateSelf() {
        this.navidentKoblingMap.clear();

        for (var kobling : dvhRepository.getKoblinger()) {
            addKoblingToNavidentMap(kobling);
            addKoblingToAgressoIdComboMap(kobling);
        }
        return true;
    }

    private void addKoblingToNavidentMap(Orgtilknytning orgtilknytning) {
        var key = orgtilknytning.getNavIdent();
        var coll = this.navidentKoblingMap.get(key);
        if (coll != null) {
            coll.add(orgtilknytning);
        } else {
            var lstnew = new ArrayList<Orgtilknytning>();
            lstnew.add(orgtilknytning);
            this.navidentKoblingMap.put(key, lstnew);
        }
    }

    private void addKoblingToAgressoIdComboMap(Orgtilknytning orgtilknytning) {
        var key = new IOrgenhet.AgressoIdCombo(orgtilknytning.getAgressoId(), orgtilknytning.getOrgNiv());
        var coll = this.agressoIdComboKoblingMap.get(key);
        if (coll != null) {
            coll.add(orgtilknytning);
        } else {
            var newList = new ArrayList<Orgtilknytning>();
            newList.add(orgtilknytning);
            this.agressoIdComboKoblingMap.put(key, newList);
        }
    }
}
