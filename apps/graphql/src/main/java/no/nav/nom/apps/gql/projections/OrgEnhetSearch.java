package no.nav.nom.apps.gql.projections;

import org.springframework.data.web.ProjectedPayload;

@ProjectedPayload
public interface OrgEnhetSearch {
    String getAgressoId();

    String getOrgNiv();

    String getNavn();

    String getId();

    default String getUniqueAgressoId(){
        return this.getAgressoId()+"-"+this.getNavn()+"-"+this.getOrgNiv();
    }

    default String getAgressoIdDashOrgnivId(){
        return this.getAgressoId()+"-"+this.getOrgNiv();
    }
}
