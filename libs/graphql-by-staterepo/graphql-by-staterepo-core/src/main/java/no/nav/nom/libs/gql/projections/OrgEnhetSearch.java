package no.nav.nom.libs.gql.projections;

import java.util.Objects;

public class OrgEnhetSearch {

    private final String agressoId;
    private final String orgNiv;  // Deprekert Jan 2026
    private final String agressoOrgNiv;
    private final String navn;
    private final String id;

    public OrgEnhetSearch(String agressoId, String orgNiv, String agressoOrgNiv, String navn, String id) {
        this.agressoId = agressoId;
        this.orgNiv = orgNiv;
        this.agressoOrgNiv = agressoOrgNiv;
        this.navn = navn;
        this.id = id;
    }

    public String getAgressoId(){
        return this.agressoId;
    }

    public String getOrgNiv(){
        return this.orgNiv;
    }

    public String agressoOrgNiv(){
        return this.agressoOrgNiv;
    }

    public String getNavn(){
        return this.navn;
    }

    public String getId(){
        return this.id;
    }

    public String getUniqueAgressoId(){
        return this.getAgressoId()+"-"+this.getNavn()+"-"+this.agressoOrgNiv();
    }

    public String getAgressoIdDashOrgnivId(){
        return this.getAgressoId()+"-"+this.agressoOrgNiv();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        OrgEnhetSearch that = (OrgEnhetSearch) o;
        return Objects.equals(agressoId, that.agressoId) && Objects.equals(agressoOrgNiv, that.agressoOrgNiv) && Objects.equals(navn, that.navn) && Objects.equals(
            id,
            that.id
        );
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(agressoId);
        result = 31 * result + Objects.hashCode(agressoOrgNiv);
        result = 31 * result + Objects.hashCode(navn);
        result = 31 * result + Objects.hashCode(id);
        return result;
    }
}
