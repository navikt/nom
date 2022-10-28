package no.nav.nom.apps.api.ressurs.implementations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.val;
import no.nav.nom.apps.api.hendelse.contract.HendelseDataType;
import no.nav.nom.apps.api.hendelse.contract.HendelseMetadataSerialize;
import no.nav.nom.apps.api.hendelse.contract.IHendelse;
import no.nav.nom.apps.api.hendelse.contract.IHendelseService;
import no.nav.nom.apps.api.postgresql.NavIdentService;
import no.nav.nom.apps.api.ressurs.contract.IRessursService;
import no.nav.nom.apps.api.ressurs.contract.IRessurs;
import no.nav.nom.apps.api.ressurs.implementations.dvh.DvhRessursInterface;
import no.nav.nom.apps.api.ressurs.implementations.postgresql.EventBasedRessurs;
import no.nav.nom.apps.api.ressurs.implementations.postgresql.RessursHendelseRepository;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RessursService implements IRessursService {

    private final DvhRessursInterface dvhRessursInterface;
    private final RessursHendelseRepository repository;
    private final IHendelseService hendelseService;
    private final ObjectMapper objectMapper;
    private final NavIdentService navIdentService;

    @Override
    public Optional<IRessurs> findByFid(UUID fid){
        return Optional.ofNullable(
                convert(repository.findAllByFunksjonellId(fid).stream().map(hendelseService::convert).toList())
        );
    }

    @Override
    public <T> Optional<T> findByNavIdent(String navIdent, Function<IRessurs, T> converter) {
        return Optional.ofNullable(
                        convert(repository.findHendelseByJson(EventBasedRessurs.NAVIDENT_FIELD, navIdent).stream().map(hendelseService::convert).toList())
                )
                .map(converter);
    }

    @Override
    public List<IRessurs> matchByNavident(String navident) {
        return repository.findHendelserByJsonMatch(EventBasedRessurs.NAVIDENT_FIELD, navident.toLowerCase(), HendelseDataType.RESSURS_V1.getDataType())
                .stream()
                .map(hendelseService::convert)
                .collect(Collectors.groupingBy(IHendelse::getFid))
                .values().stream().map(this::convert)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<IRessurs> findByPersonIdent(String personIdent){
        return Optional.ofNullable(
                convert(repository.findHendelseByJson(EventBasedRessurs.PERSONIDENT_FIELD, personIdent).stream().map(hendelseService::convert).toList())
        );
    }

    @Override
    public Map<String, IRessurs> findByNavIdentBulk(List<String> navIdenter){
        return repository.findHendelseByJsonBulk(EventBasedRessurs.NAVIDENT_FIELD, navIdenter).stream()
                .map(hendelseService::convert)
                .collect(Collectors.groupingBy(IHendelse::getFid))
                .values().stream()
                .map(this::convert)
                .collect(Collectors.toUnmodifiableMap(IRessurs::getNavIdent, Function.identity()));
    }

    @Override
    public Map<String, IRessurs> findByPersonIdentBulk(List<String> personIdenter){
        return repository.findHendelseByJsonBulk(EventBasedRessurs.PERSONIDENT_FIELD, personIdenter).stream()
                .map(hendelseService::convert)
                .collect(Collectors.groupingBy(IHendelse::getFid))
                .values().stream()
                .map(this::convert)
                .collect(Collectors.toUnmodifiableMap(IRessurs::getPersonIdent, Function.identity()));
    }

    @Override
    public List<IRessurs> getAll(){
        return repository.findAllByDataType(HendelseDataType.RESSURS_V1.getDataType()).stream()
                .map(hendelseService::convert)
                .collect(Collectors.groupingBy(IHendelse::getFid))
                .values()
                .stream()
                .map(EventBasedRessurs::new)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public IRessurs opprettRessurs(String personIdent, String navIdent, String navIdentForbokstav, HendelseMetadataSerialize hendelseMetadata) {
        return findByPersonIdent(personIdent).orElseGet(() -> {
            var data = objectMapper.createObjectNode();

            data.put(EventBasedRessurs.NAVIDENT_FIELD, navIdent != null ? navIdent : navIdentService.generateNavIdent(navIdentForbokstav));
            data.put(EventBasedRessurs.PERSONIDENT_FIELD, personIdent);

            val event = hendelseService.createOpprettHendelse(data, hendelseMetadata, HendelseDataType.RESSURS_V1);
            repository.save(hendelseService.convert(event));

            return findByFid(event.getFid()).orElse(null);
        });
    }

    @Override
    @Transactional
    public IRessurs opprettRessurs(String personident, JsonNode data, HendelseMetadataSerialize hendelseMetadataSerialize) {
        return findByPersonIdent(personident).orElseGet(() -> {
            val event = hendelseService.createOpprettHendelse(data, hendelseMetadataSerialize, HendelseDataType.RESSURS_V1);
            repository.save(hendelseService.convert(event));

            return findByFid(event.getFid()).orElse(null);

        });
    }

    @Override
    @Transactional
    public IRessurs opprettRessurs(String personerident, Map<String, Object> data, HendelseMetadataSerialize metadata) {
        return opprettRessurs(personerident, (JsonNode) objectMapper.valueToTree(data), metadata);
    }

    @Override
    public Ressurs convertToGraphQlResponse(IRessurs ressurs) {
        var sektor = dvhRessursInterface.getRessurstypeGraphQlResponse(ressurs.getNavIdent());
        return Ressurs.builder()
                .nomId(ressurs.getId())
                .navIdent(ressurs.getNavIdent())
                .navident(ressurs.getNavIdent())
                .personIdent(ressurs.getPersonIdent())
                .personident(ressurs.getPersonIdent())
                .ressurstype(sektor)
                .sektor(sektor)
                .sluttdato(dvhRessursInterface.getSluttdatoForPersonident(ressurs.getPersonIdent()))
                .fornavn(ressurs.getVisningsmellomnavn().map(m -> ressurs.getVisningsfornavn().orElseThrow()+" "+m).orElse(ressurs.getVisningsfornavn().orElse(null)))
                .etternavn(ressurs.getVisningsetternavn().orElse(null))
                .build();
    }

    private IRessurs convert(List<IHendelse> hendelser){
        return hendelser == null || hendelser.isEmpty() ? null : new EventBasedRessurs(hendelser);
    }
}
