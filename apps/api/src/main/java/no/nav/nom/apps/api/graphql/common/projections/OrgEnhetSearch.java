package no.nav.nom.apps.api.graphql.common.projections;

import org.springframework.data.web.ProjectedPayload;

@ProjectedPayload
public interface OrgEnhetSearch {
    String getAgressoId();
    String getOrgNiv();
    String getNavn();
}
