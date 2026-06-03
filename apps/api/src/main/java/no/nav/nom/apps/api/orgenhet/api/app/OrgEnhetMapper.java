package no.nav.nom.apps.api.orgenhet.api.app;

import no.nav.nom.apps.api.common.security.NomApiAccessScope;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgEnhetOverordnetResolver;
import no.nav.nom.apps.api.orgenhet.OrgEnhetUnderordnetResolver;
import no.nav.nom.apps.api.orgenhet.api.app.dto.OrgEnhet;
import no.nav.nom.apps.api.orgenhet.api.app.dto.OrgEnhetMedLedernavn;
import no.nav.nom.apps.api.orgenhetleder.OrgEnhetLederService;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.apps.api.remedy.RemedyEnhet;
import no.nav.nom.apps.api.remedy.RemedyEnhetMapperService;
import no.nav.nom.apps.api.ressurs.RessursService;
import no.nav.nom.apps.api.ressurs.api.dto.MinimalRessurs;
import no.nav.nom.contract.nom.state.OrgEnhetLederState;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.RessursState;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.text.Collator;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Component
public class OrgEnhetMapper {
    private final RemedyEnhetMapperService remedyEnhetMapperService;
    private final RessursService ressursService;
    private final OrgEnhetLederService orgEnhetLederService;
    private final OrgtilknytningService orgtilknytningService;

    private final OrgEnhetOverordnetResolver overordnetResolver;

    private final OrgEnhetUnderordnetResolver underordnetResolver;

    public OrgEnhetMapper(
        RemedyEnhetMapperService remedyEnhetMapperService, RessursService ressursService,
        OrgEnhetLederService orgEnhetLederService,
        OrgtilknytningService orgtilknytningService,
        OrgEnhetOverordnetResolver overordnetResolver,
        OrgEnhetUnderordnetResolver underordnetResolver
    ) {
        this.remedyEnhetMapperService = remedyEnhetMapperService;
        this.ressursService = ressursService;
        this.orgEnhetLederService = orgEnhetLederService;
        this.orgtilknytningService = orgtilknytningService;
        this.overordnetResolver = overordnetResolver;
        this.underordnetResolver = underordnetResolver;
    }

    public String getRemedyEnhetId(String orgEnhetId) {
        return remedyEnhetMapperService.findByNomId(orgEnhetId)
            .map(RemedyEnhet::remedyEnhetId)
            .orElse(null);
    }

    public OrgEnhet mapToOrgEnhet(IOrgenhet orgEnhet) {

        var remedyEnhetId = remedyEnhetMapperService.findByNomId(orgEnhet.getId()).map(RemedyEnhet::remedyEnhetId).orElse(null);
        var overordnetId = overordnetResolver.findOverordnetOrgEnhet(orgEnhet.getFid()).map(OrgEnhetState::id).orElse(null);
        var orgEnhetLeder = orgEnhetLederService.getAktivLederForOrgEnhet(orgEnhet.getId())
            .flatMap(orgEnhetLederState -> ressursService.findByNavident(orgEnhetLederState.navident()))
            .map(ressursState -> new MinimalRessurs(ressursState.navident(), ressursState.visningsnavn()
                .getFormattedVisningsnavn())).orElse(null);


        Collator norwegianCollator = Collator.getInstance(new Locale.Builder().setLanguage("no").setRegion("NO").build());
        var underordnedeOrgEnhetIds = underordnetResolver.findUnderordnedeOrgEnheter(orgEnhet.getFid())
            .stream()
            .sorted(Comparator.comparing(OrgEnhetState::navn, norwegianCollator))
            .map(OrgEnhetState::id)
            .toList();

        return new OrgEnhet(
            orgEnhet.getId(),
            orgEnhet.getFid(),
            orgEnhet.getAgressoId(),
            orgEnhet.getOrgNiv(),
            orgEnhet.getAgressoOrgenhetType(),
            orgEnhet.getNavn(),
            orgEnhet.getGyldigFom(),
            orgEnhet.getGyldigTom(),
            orgEnhet.getNomNivaa(),
            orgEnhet.getOrgEnhetsType(),
            remedyEnhetId,
            orgEnhet.getTilgangsenhetId(),
            overordnetId,
            underordnedeOrgEnhetIds,
            hentOrgEnhetLedere(orgEnhet),
            orgEnhetLeder,
            orgtilknytningService.getByEnhet(orgEnhet.getId())
        );
    }

    public OrgEnhet mapToReducedOrgEnhet(IOrgenhet orgEnhet) {

        var remedyEnhetId = remedyEnhetMapperService.findByNomId(orgEnhet.getId()).map(RemedyEnhet::remedyEnhetId).orElse(null);
        var overordnetId = overordnetResolver.findOverordnetOrgEnhet(orgEnhet.getFid()).map(OrgEnhetState::id).orElse(null);
        var orgEnhetLeder = orgEnhetLederService.getAktivLederForOrgEnhet(orgEnhet.getId())
            .flatMap(orgEnhetLederState -> ressursService.findByNavident(orgEnhetLederState.navident()))
            .map(ressursState -> new MinimalRessurs(ressursState.navident(), ressursState.visningsnavn()
                .getFormattedVisningsnavn())).orElse(null);

        return new OrgEnhet(
            orgEnhet.getId(),
            orgEnhet.getFid(),
            orgEnhet.getAgressoId(),
            orgEnhet.getOrgNiv(),
            orgEnhet.getAgressoOrgenhetType(),
            orgEnhet.getNavn(),
            orgEnhet.getGyldigFom(),
            orgEnhet.getGyldigTom(),
            orgEnhet.getNomNivaa(),
            orgEnhet.getOrgEnhetsType(),
            remedyEnhetId,
            orgEnhet.getTilgangsenhetId(),
            overordnetId,
            Collections.emptyList(),
            Collections.emptyList(),
            orgEnhetLeder,
            Collections.emptyList()
        );
    }

    public OrgEnhetMedLedernavn mapToOrgEnhetMedLedernavn(IOrgenhet orgEnhet) {
        var remedyEnhetId = remedyEnhetMapperService.findByNomId(orgEnhet.getId()).map(RemedyEnhet::remedyEnhetId);

        return new OrgEnhetMedLedernavn(
                orgEnhet.getId(),
                orgEnhet.getNavn(),
                remedyEnhetId.orElse(null),
                getLedernavn(orgEnhet),
                orgEnhet.getGyldigFom()
        );
    }

    private List<RessursState> hentOrgEnhetLedere(IOrgenhet orgEnhet) {
        return orgEnhetLederService.getLedereForOrgEnhet(orgEnhet.getId()).stream()
            .map(this::ressursWithObscuredPersonident)
            .toList();
    }

    private RessursState ressursWithObscuredPersonident(OrgEnhetLederState leder) {
        var ressursState = ressursService.findByNavident(leder.navident()).orElse(null);
        var auth = SecurityContextHolder.getContext().getAuthentication();
        var loggedInUserAuthorities = auth != null ? auth.getAuthorities() : List.of();

        if (ressursState != null && !loggedInUserAuthorities.contains(NomApiAccessScope.ADMIN.asGrantedAuthority())) {
            ressursState = ressursState.toBuilder()
                .personident(ressursState.personident().replaceFirst(".{5}$", "*****"))
                .build();
        }

        return ressursState;
    }

    private List<String> getLedernavn(IOrgenhet orgEnhet) {
        return orgEnhetLederService.getAktivLederForOrgEnhet(orgEnhet.getId())
            .stream()
            .map(OrgEnhetLederState::navident)
            .map(this::getNavnForNavident)
            .filter(Objects::nonNull)
            .toList();
    }

    private String getNavnForNavident(String navident) {
        return ressursService.findByNavident(navident)
                .map(r -> r.visningsnavn().getFormattedVisningsnavn())
                .orElse(null);
    }
}
