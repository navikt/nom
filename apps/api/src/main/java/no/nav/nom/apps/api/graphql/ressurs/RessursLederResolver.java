package no.nav.nom.apps.api.graphql.ressurs;

import graphql.execution.DataFetcherResult;
import graphql.kickstart.tools.GraphQLResolver;
import lombok.RequiredArgsConstructor;
import no.nav.nom.apps.api.domain.RessursRepository;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.dvh.dto.DvhKobling;
import no.nav.nom.apps.api.dvh.dto.DvhLeder;
import no.nav.nom.apps.api.dvh.dto.DvhOrganisering;
import no.nav.nom.apps.api.graphql.common.ConverterUtils;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.api.graphql.RessursLeder;
import no.nav.nom.libs.utils.common.util.DateUtil;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static no.nav.nom.libs.utils.common.util.StreamUtils.distinctByKey;

@Component
@RequiredArgsConstructor
public class RessursLederResolver implements GraphQLResolver<Ressurs> {

    private final DvhRepository dvhRepository;
    private final RessursRepository ressursRepository;

    public DataFetcherResult<List<RessursLeder>> ledere(Ressurs ressurs) {
        final List<RessursLeder> result = dvhRepository.getKoblingerByNavIdent(ressurs.getNavIdent())
                .stream()
                .filter(dto -> DateUtil.isNow(dto.getGyldigFom(), dto.getGyldigTom()))
                .filter(distinctByKey(DvhKobling::getAgressoId))
                .flatMap(kobling -> {
                    String agressoId = kobling.getAgressoId();
                    var ledere = lederFor(agressoId, ressurs.getNavIdent());

                    if (!ledere.isEmpty()) {
                        return ledere.stream();
                    }
                    // Om ressurs er leder for sin enhet henter vi leder fra nivå over
                    return lederForRecursive(agressoId, ressurs.getNavIdent()).stream();
                })
                .map(RessursLeder::new)
                .collect(Collectors.toList());

        return DataFetcherResult.<List<RessursLeder>>newResult()
                .data(result)
                .build();
    }

    private List<Ressurs> lederForRecursive(String agressoId, String excludeNavIdent) {
        return dvhRepository.getOrganiseringerByAgressoId(agressoId).stream()
                .map(DvhOrganisering::getOverenhetNummer)
                .map(dvhRepository::getEnhetByAgressoId)
                .filter(Optional::isPresent)
                .map(Optional::get).findFirst()
                .map(o -> {
                    var ledere = lederFor(o.getAgressoId(), excludeNavIdent);
                    if (!ledere.isEmpty()) {
                        return ledere;
                    }
                    return lederForRecursive(o.getAgressoId(), excludeNavIdent);
                })
                .orElse(List.of());
    }

    private List<Ressurs> lederFor(String agressoId, String excludeNavIdent) {
        return dvhRepository.getLederByAgressoId(agressoId)
                .stream()
                .map(DvhLeder::getNavIdent)
                .map(ressursRepository::findByNavIdent)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(r -> !r.getNavIdent().equals(excludeNavIdent))
                .map(ConverterUtils::convert)
                .collect(Collectors.toList());
    }

}
