package no.nav.nom.apps.api.orgenhet.implementations.postgresql;

import lombok.RequiredArgsConstructor;
import no.nav.nom.apps.api.hendelse.contract.HendelseDataType;
import no.nav.nom.apps.api.hendelse.contract.IHendelse;
import no.nav.nom.apps.api.hendelse.contract.IHendelseService;
import no.nav.nom.apps.api.orgenhet.contract.IOrgenhet;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventBasedOrgenhetService {

    private final OrgenhetHendelseRepository orgenhetHendelseRepository;
    private final IHendelseService hendelseService;

    public Optional<IOrgenhet> getByFid(UUID fid) {
        return convert(orgenhetHendelseRepository.findAllByFunksjonellId(fid).stream().map(hendelseService::convert).toList());
    }

    public Optional<IOrgenhet> getEnhetByAgressoIdNivaAndName(String agressoId, String orgNiv, String name) {
        return orgenhetHendelseRepository.findEnhetByAgressoIdNivaAndName(agressoId, orgNiv, name).stream()
                .map(hendelseService::convert)
                .collect(Collectors.groupingBy(IHendelse::getFid)).values().stream()
                .map(this::convert)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .max(Comparator.comparing(IOrgenhet::getGyldigFom));
    }

    public Optional<IOrgenhet> getEnhetByAgressoIdAndOrgniv(String agressoId, String orgNiv) {
        return orgenhetHendelseRepository.findEnhetByAgressoIdNiva(agressoId, orgNiv).stream()
                .map(hendelseService::convert)
                .collect(Collectors.groupingBy(IHendelse::getFid)).values().stream()
                .map(this::convert)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .max(Comparator.comparing(IOrgenhet::getGyldigFom));
    }

    public List<IOrgenhet> findEnheterByAgressoIdIn(List<String> agressoIds) {
        return orgenhetHendelseRepository.findByAgressoIdIn(agressoIds).stream()
                .map(hendelseService::convert)
                .collect(Collectors.groupingBy(IHendelse::getFid)).values().stream()
                .map(this::convert)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    public Collection<IOrgenhet> getAllEnheter() {
        return orgenhetHendelseRepository.findAllByDataType(HendelseDataType.ORGENHET_V1.getDataType()).stream()
                .map(hendelseService::convert)
                .collect(Collectors.groupingBy(IHendelse::getFid)).values().stream()
                .map(this::convert)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    public List<IOrgenhet> matchByAgressoId(String agressoId) {
        return orgenhetHendelseRepository.findHendelserByJsonMatch(EventBasedOrgenhet.AGRESSO_ID_FIELD, agressoId, HendelseDataType.ORGENHET_V1.getDataType()).stream()
                .map(hendelseService::convert)
                .collect(Collectors.groupingBy(IHendelse::getFid)).values().stream()
                .map(this::convert)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    public List<IOrgenhet> matchByNavn(String navn) {
        return orgenhetHendelseRepository.findHendelserByJsonMatch(EventBasedOrgenhet.NAVN_FIELD, navn, HendelseDataType.ORGENHET_V1.getDataType()).stream()
                .map(hendelseService::convert)
                .collect(Collectors.groupingBy(IHendelse::getFid)).values().stream()
                .map(this::convert)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    private Optional<IOrgenhet> convert(List<IHendelse> hendelser){
        return hendelser == null || hendelser.isEmpty() ? Optional.empty() : Optional.of(new EventBasedOrgenhet(hendelser));
    }
}
