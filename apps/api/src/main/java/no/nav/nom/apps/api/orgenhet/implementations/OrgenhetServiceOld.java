package no.nav.nom.apps.api.orgenhet.implementations;

import no.nav.nom.apps.api.hendelse.contract.HendelseDataType;
import no.nav.nom.apps.api.hendelse.contract.IHendelse;
import no.nav.nom.apps.api.hendelse.contract.IHendelseService;
import no.nav.nom.apps.api.orgenhet.contract.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.contract.IOrgenhetService;
import no.nav.nom.apps.api.orgenhet.implementations.postgresql.EventBasedOrgenhet;
import no.nav.nom.apps.api.orgenhet.implementations.postgresql.OrgenhetHendelseRepository;
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
public class OrgenhetServiceOld implements IOrgenhetService {
    private final OrgenhetHendelseRepository orgenhetHendelseRepository;
    private final IHendelseService hendelseService;

    public OrgenhetServiceOld(OrgenhetHendelseRepository orgenhetHendelseRepository, IHendelseService hendelseService) {
        this.orgenhetHendelseRepository = orgenhetHendelseRepository;
        this.hendelseService = hendelseService;
    }

    @Override
    public Optional<IOrgenhet> getById(String id) {
        return getByFid(id);
    }

    @Override
    public Optional<IOrgenhet> getByFid(String fid) {
        return convert(orgenhetHendelseRepository.findAllByFunksjonellId(UUID.fromString(fid)).stream().map(hendelseService::convert).toList());
    }

    @Override
    public Optional<IOrgenhet> getEnhetByAgressoIdNivaAndName(String agressoId, String orgNiv, String name) {
        if (name != null) {
            return toIOrgEnhetStream(orgenhetHendelseRepository.findEnhetByAgressoIdNivaAndName(agressoId, orgNiv, name)
                                             .stream()).max(comparing(IOrgenhet::getGyldigFom));
        }
        return getEnhetByAgressoIdAndOrgniv(agressoId, orgNiv);
    }

    @Override
    public Optional<IOrgenhet> getEnhetByAgressoIdAndOrgniv(String agressoId, String orgNiv) {
        return toIOrgEnhetStream(orgenhetHendelseRepository.findEnhetByAgressoIdNiva(agressoId, orgNiv).stream()).max(comparing(IOrgenhet::getGyldigFom));
    }

    @Override
    public List<IOrgenhet> findEnheterByAgressoIdIn(Set<String> agressoIds) {
        return toIOrgEnhetStream(orgenhetHendelseRepository.findByAgressoIdIn(agressoIds).stream()).toList();
    }

    @Override
    public List<IOrgenhet> findEnheterByAgressoIdConcatOrgnivIn(Set<IOrgenhet.AgressoIdCombo> agressoIdConcatOrgnivSet) {
        return toIOrgEnhetStream(
            orgenhetHendelseRepository.findByAgressoIdConcatOrgnivIn(agressoIdConcatOrgnivSet.stream()
                .map(it -> it.orgNiv() + "--" + it.agressoId())
                .collect(Collectors.toSet())
            )
        ).toList();
    }

    @Override
    public Collection<IOrgenhet> getAllEnheter() {
        return toIOrgEnhetStream(orgenhetHendelseRepository.findAllByDataType(HendelseDataType.ORGENHET_V1.getDataType()).stream()).toList();
    }

    @Override
    public List<IOrgenhet> matchByAgressoId(String agressoId) {
        return toIOrgEnhetStream(orgenhetHendelseRepository.findHendelserByJsonMatch(
                EventBasedOrgenhet.AGRESSO_ID_FIELD,
                agressoId,
                HendelseDataType.ORGENHET_V1.getDataType()
        ).stream()).toList();
    }

    @Override
    public List<IOrgenhet> matchByNavn(String navn) {
        return toIOrgEnhetStream(orgenhetHendelseRepository.findHendelserByJsonMatch(
                EventBasedOrgenhet.NAVN_FIELD,
                navn,
                HendelseDataType.ORGENHET_V1.getDataType()
        ).stream()).toList();
    }

    @Override
    public Map<String, IOrgenhet> getByIdBulk(List<String> idList) {
        return getByIdBulk(idList);
    }

    @Override
    public Map<String, IOrgenhet> getByFidBulk(List<String> fidList) {
        var tmp = orgenhetHendelseRepository.findByFidIn(new HashSet<>(fidList));

        var out = new HashMap<String, IOrgenhet>(tmp.size());
        var orgenhetStream = toIOrgEnhetStream(tmp.stream());
        orgenhetStream.forEach(oe -> {
            out.put(oe.getFid().toString(), oe);
        });
        return out;
    }

    private Stream<IOrgenhet> toIOrgEnhetStream(Collection<GenericStorageElement> storageElements) {
        return toIOrgEnhetStream(storageElements.stream());
    }

    private Stream<IOrgenhet> toIOrgEnhetStream(Stream<GenericStorageElement> elementStream) {
        return elementStream.map(hendelseService::convert)
                .collect(Collectors.groupingBy(IHendelse::getFid))
                .values()
                .stream()
                .map(this::convert)
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    private Optional<IOrgenhet> convert(List<IHendelse> hendelser) {
        return Optional.ofNullable(hendelser).map(EventBasedOrgenhet::new);
    }
}
