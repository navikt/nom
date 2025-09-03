package no.nav.nom.apps.admin;

import lombok.experimental.UtilityClass;
import no.nav.nom.apps.admin.sanitycheck.dto.orgenhet.OrgEnhetArbeidsomraadeInfo;
import no.nav.nom.apps.admin.sanitycheck.dto.orgenhet.OrgEnhetOrganisering;
import no.nav.nom.apps.admin.sanitycheck.dto.orgtilknytning.OverlappendeOrgtilknytning;
import no.nav.nom.apps.admin.sanitycheck.dto.ressurs.RessursMedEnhet;
import no.nav.nom.apps.admin.sanitycheck.dto.ressurs.RessursOrgtilknytningUlikSluttdato;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNullElse;

@UtilityClass
public class SanityCheckMappingUtils {

    public List<Map<String, Object>> orgEnhetOrganiseringToMapList(List<OrgEnhetOrganisering> orgEnhetOrganisering) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (OrgEnhetOrganisering orgEnhet : orgEnhetOrganisering) {
            LinkedHashMap<String, Object> map = new LinkedHashMap<>();
            map.put("Organisasjonsenhet ID", orgEnhet.orgEnhetId());
            map.put("Unique Agresso ID", orgEnhet.uniqueAgressoId());
            map.put("Gyldig FOM", orgEnhet.gyldigFom());
            map.put("Gyldig TOM", nonNull(orgEnhet.gyldigTom()) ? orgEnhet.gyldigTom() : "");
            result.add(map);
        }
        return result;
    }

    public List<Map<String, Object>> orgEnhetStateToMapList(List<OrgEnhetState> orgEnhetStates) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (OrgEnhetState orgEnhetState : orgEnhetStates) {
            LinkedHashMap<String, Object> map = new LinkedHashMap<>();
            map.put("OrgEnhet FID", orgEnhetState.fid());
            map.put("OrgEnhet ID", orgEnhetState.id());
            map.put("OrgEnhet Navn", orgEnhetState.navn());
            map.put("Gyldig FOM", orgEnhetState.gyldigFom());
            map.put("Gyldig TOM", nonNull(orgEnhetState.gyldigTom()) ? orgEnhetState.gyldigTom() : "");
            map.put("Unik Agresso ID", requireNonNullElse(orgEnhetState.uniqueAgressoId(), ""));
            result.add(map);
        }
        return result;
    }

    public List<Map<String, Object>> orgEnhetArbeidsomraadeInfoToMapList(List<OrgEnhetArbeidsomraadeInfo.LoevnodeMedFeilAntallArbOmr> infos) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (OrgEnhetArbeidsomraadeInfo.LoevnodeMedFeilAntallArbOmr info : infos) {
            LinkedHashMap<String, Object> map = new LinkedHashMap<>();
            map.put("OrgEnhet ID", info.orgenhetId());
            map.put("OrgEnhet Navn", info.orgenhetNavn());
            map.put("Antall Arbeidsområder", info.antallArbOmr());
            result.add(map);
        }
        return result;
    }

    public List<Map<String, Object>> ressursMedEnhetToMapList(List<RessursMedEnhet> ressursMedEnhetList) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (RessursMedEnhet ressurs : ressursMedEnhetList) {
            LinkedHashMap<String, Object> map = new LinkedHashMap<>();
            map.put("Navident", ressurs.navident());
            map.put("Ressurs FID", ressurs.ressursFid());
            map.put("Visningsnavn", ressurs.visningsnavn());
            map.put("Ressurs Startdato", ressurs.startdato());
            map.put("Ressurs Sluttdato", nonNull(ressurs.sluttdato()) ? ressurs.sluttdato() : "");
            map.put("Sektor", ressurs.sektor());
            map.put("OrgEnhet ID", ressurs.orgEnhetId());
            map.put("OrgEnhet Navn", ressurs.orgEnhetNavn());
            map.put("Gyldig FOM", ressurs.gyldigFom());
            map.put("Gyldig TOM", requireNonNullElse(ressurs.gyldigTom(), ""));
            map.put("Unik Agresso ID", nonNull(ressurs.uniqueAgressoId()) ? ressurs.uniqueAgressoId() : "");
            result.add(map);
        }
        return result;
    }

    public List<Map<String, Object>> ressursStateToMapList(List<RessursState> ressursStates) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (RessursState ressursState : ressursStates) {
            LinkedHashMap<String, Object> map = new LinkedHashMap<>();
            map.put("Ressurs FID", ressursState.fid());
            map.put("Navident", ressursState.navident());
            map.put("Visningsnavn", ressursState.visningsnavn());
            map.put("Ressurs Startdato", nonNull(ressursState.startdato()) ? ressursState.startdato() : "");
            map.put("Ressurs Sluttdato", nonNull(ressursState.sluttdato()) ? ressursState.sluttdato() : "");
            map.put("Sektor", ressursState.sektor());
            map.put("Primær Telefonnummer", nonNull(ressursState.primaryTelefonnummer()) ? ressursState.primaryTelefonnummer() : "");
            map.put("Jobbtelefon", nonNull(ressursState.jobbtelefon()) ? ressursState.jobbtelefon().nummer() : "");
            map.put("Primær Fysisk Brukersted", nonNull(ressursState.primaryFysiskBrukersted()) ? ressursState.primaryFysiskBrukersted() : "");
            map.put("Fysiske Brukersteder", String.join(", ", ressursState.fysiskeBrukersteder()));
            map.put("Ident Type", ressursState.identType());
            map.put("Sist Endret Av", nonNull(ressursState.sistEndretAv()) ? ressursState.sistEndretAv() : "");
            result.add(map);
        }
        return result;
    }

    public List<Map<String, Object>> ressursOrgtilknytningUlikSluttdatoToMapList(List<RessursOrgtilknytningUlikSluttdato> ressursOrgtilknytningUlikSluttdato) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (RessursOrgtilknytningUlikSluttdato ressurs : ressursOrgtilknytningUlikSluttdato) {
            LinkedHashMap<String, Object> map = new LinkedHashMap<>();
            map.put("Navident", ressurs.navident());
            map.put("OrgEnhet ID", ressurs.orgEnhetId());
            map.put("Ressurs Sluttdato", ressurs.ressursSluttdato());
            map.put("Orgtilknytning Sluttdato", ressurs.orgtilknytningSluttdato());
            result.add(map);
        }
        return result;
    }

    public List<Map<String, Object>> orgtilknytningStateToMapList(List<OrgtilknytningState> orgtilknytningStates) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (OrgtilknytningState ressurs : orgtilknytningStates) {
            LinkedHashMap<String, Object> map = new LinkedHashMap<>();
            map.put("Ressurs FID", ressurs.fid());
            map.put("Navident", ressurs.navident());
            map.put("OrgEnhet ID", ressurs.orgEnhetId());
            map.put("Sektor", nonNull(ressurs.sektor()) ? ressurs.sektor() : "");
            map.put("Daglig Oppfølging", ressurs.erDagligOppfolging());
            map.put("Gyldig FOM", ressurs.gyldigFom());
            map.put("Gyldig TOM", nonNull(ressurs.gyldigTom()) ? ressurs.gyldigTom() : "");
            map.put("Tilknytningstype", nonNull(ressurs.orgtilknytningType()) ? ressurs.orgtilknytningType() : "");
            map.put("Masseflyttet Fra FID", nonNull(ressurs.masseflyttetFraFid()) ? ressurs.masseflyttetFraFid() : "");
            map.put("Sist Endret Av", nonNull(ressurs.sistEndretAv()) ? ressurs.sistEndretAv() : "");
            result.add(map);
        }
        return result;
    }

    public List<Map<String, Object>> overlappendeOrgtilknytningToMapList(List<OverlappendeOrgtilknytning> overlappendeOrgtilknytninger) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (OverlappendeOrgtilknytning overlappendeOrgtilknytning : overlappendeOrgtilknytninger) {
            LinkedHashMap<String, Object> map = new LinkedHashMap<>();
            map.put("Navident", overlappendeOrgtilknytning.navident1());
            map.put("OrgEnhet ID 1", overlappendeOrgtilknytning.orgEnhetId1());
            map.put("Orgtilknytning FID 1", overlappendeOrgtilknytning.fid1());
            map.put("Gyldig FOM 1", overlappendeOrgtilknytning.gyldigFom1());
            map.put("Gyldig TOM 1", nonNull(overlappendeOrgtilknytning.gyldigTom1()) ? overlappendeOrgtilknytning.gyldigTom1() : "");
            map.put("OrgEnhet ID 2", overlappendeOrgtilknytning.orgEnhetId2());
            map.put("Orgtilknytning FID 2", overlappendeOrgtilknytning.fid2());
            map.put("Gyldig FOM 2", overlappendeOrgtilknytning.gyldigFom2());
            map.put("Gyldig TOM 2", nonNull(overlappendeOrgtilknytning.gyldigTom2()) ? overlappendeOrgtilknytning.gyldigTom2() : "");
            result.add(map);
        }
        return result;
    }
}