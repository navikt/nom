package no.nav.nom.libs.gql.projections;


import java.util.Objects;

public class OrgEnhetSearch {

    private final String agressoId;
    private final String orgNiv;
    private final String navn;
    private final String id;

    public OrgEnhetSearch(String agressoId, String orgNiv, String navn, String id) {
        this.agressoId = agressoId;
        this.orgNiv = orgNiv;
        this.navn = navn;
        this.id = id;
    }

    public String getAgressoId(){
        return this.agressoId;
    }

    public String getOrgNiv(){
        return this.orgNiv;
    }

    public String getNavn(){
        return this.navn;
    }

    public String getId(){
        return this.id;
    }

    public String getUniqueAgressoId(){
        return this.getAgressoId()+"-"+this.getNavn()+"-"+this.getOrgNiv();
    }

    public String getAgressoIdDashOrgnivId(){
        return this.getAgressoId()+"-"+this.getOrgNiv();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        OrgEnhetSearch that = (OrgEnhetSearch) o;
        return Objects.equals(agressoId, that.agressoId) && Objects.equals(orgNiv, that.orgNiv) && Objects.equals(navn, that.navn) && Objects.equals(
            id,
            that.id
        );
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(agressoId);
        result = 31 * result + Objects.hashCode(orgNiv);
        result = 31 * result + Objects.hashCode(navn);
        result = 31 * result + Objects.hashCode(id);
        return result;
    }
}
