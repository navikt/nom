package no.nav.nom.apps.api.orgenhet.api.app;

import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgEnhetOverordnetResolver;
import no.nav.nom.apps.api.orgenhet.OrgEnhetUnderordnetResolver;
import no.nav.nom.apps.api.orgenhet.api.app.dto.OrgEnhet;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.apps.api.remedy.RemedyEnhet;
import no.nav.nom.apps.api.remedy.RemedyEnhetMapperService;
import no.nav.nom.apps.api.ressurs.eventsourcing.state.RessursStateRepository;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.RessursState;
import org.springframework.stereotype.Component;

import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Component
public class OrgEnhetMapper {
    private final RemedyEnhetMapperService remedyEnhetMapperService;
    private final DvhRepository dvhRepository;
    private final RessursStateRepository ressursStateRepository;
    private final no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService orgtilknytningService;

    private final OrgEnhetOverordnetResolver overordnetResolver;

    private final OrgEnhetUnderordnetResolver underordnetResolver;

    public OrgEnhetMapper(
        RemedyEnhetMapperService remedyEnhetMapperService,
        DvhRepository dvhRepository,
        RessursStateRepository ressursStateRepository,
        OrgtilknytningService orgtilknytningService,
        OrgEnhetOverordnetResolver overordnetResolver,
        OrgEnhetUnderordnetResolver underordnetResolver
    ) {
        this.remedyEnhetMapperService = remedyEnhetMapperService;
        this.dvhRepository = dvhRepository;
        this.ressursStateRepository = ressursStateRepository;
        this.orgtilknytningService = orgtilknytningService;
        this.overordnetResolver = overordnetResolver;
        this.underordnetResolver = underordnetResolver;
    }

    public OrgEnhet mapFrom(IOrgenhet orgEnhet) {

        var remedyEnhetId = remedyEnhetMapperService.findByNomId(orgEnhet.getId()).map(RemedyEnhet::remedyEnhetId);
        var overordnetId = overordnetResolver.findOverordnetOrgEnhet(orgEnhet.getFid()).map(OrgEnhetState::id);

        Collator norwegianCollator = Collator.getInstance(new Locale("no", "NO"));
        var underordnedeOrgEnhetIds = underordnetResolver.findUnderordnedeOrgEnheter(orgEnhet.getFid())
                .stream()
                .sorted(Comparator.comparing(OrgEnhetState::navn, norwegianCollator))
                .map(OrgEnhetState::id)
                .toList();

        return new OrgEnhet(
            orgEnhet.getId(),
            orgEnhet.getAgressoId(),
            orgEnhet.getOrgNiv(),
            orgEnhet.getAgressoOrgenhetType(),
            orgEnhet.getNavn(),
            orgEnhet.getGyldigFom(),
            orgEnhet.getGyldigTom().orElse(null),
            orgEnhet.getNomNivaa().orElse(null),
            orgEnhet.getOrgEnhetsType().orElse(null),
            remedyEnhetId.orElse(null),
            overordnetId.orElse(null),
            underordnedeOrgEnhetIds,
            hentOrgEnhetsLedere(orgEnhet),
            orgtilknytningService.getByEnhet(orgEnhet.getId())
        );
    }

    private List<RessursState> hentOrgEnhetsLedere(IOrgenhet orgEnhet) {
        return dvhRepository.getLederByAgressoIdAndNiv(orgEnhet.getAgressoId(), orgEnhet.getOrgNiv())
            .stream()
            .map(leder -> ressursStateRepository.findByNavident(leder.getNavIdent()).orElse(null))
            .filter(Objects::nonNull)
            .toList();
    }

}
