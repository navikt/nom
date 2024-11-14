package no.nav.nom.libs.lucenewrapper.domain;

import no.nav.nom.libs.lucenewrapper.search.Analyzers;
import no.nav.nom.libs.lucenewrapper.search.IEntityIndexer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

public class RessursIndexer implements IEntityIndexer<LuceneRessurs, LuceneRessursFilter> {

    private final Analyzer analyzer = ressursAnalyzer();
    private final Utils helper = this.createUtils();


    @Override
    public Document toDoc(LuceneRessurs entity) {
        var doc = helper.createDocumentWithCommonFields(entity.navident(), entity.navn());
        doc.add(new Field("epost", entity.epost(), TextField.TYPE_NOT_STORED));
        doc.add(new Field("tlf", entity.tlf(), TextField.TYPE_NOT_STORED));
        doc.add(new Field("active", String.valueOf(entity.active()), TextField.TYPE_NOT_STORED));
        doc.add(new Field("sektor",String.valueOf(entity.sektor()),TextField.TYPE_NOT_STORED));
        return doc;
    }

    @Override
    public Query toQuery(String terms, LuceneRessursFilter ressursQueryFilter) throws ParseException {
        if(terms.isBlank()){
            return new MatchNoDocsQuery();
        }

        Query coreQry;
        if(terms.matches("^\\d{6}$")){
            var letters = "abcdefghijklmnopqrstuvwxyz";
            var boolQryBuilder = new BooleanQuery.Builder();
            for(var l : letters.toCharArray()){
                var letterQry = new TermQuery(new Term(this.idField(),l + terms));
                boolQryBuilder.add(letterQry, BooleanClause.Occur.SHOULD);
            }
            coreQry = boolQryBuilder.build();
        }else{
            coreQry = helper.nameQuery(terms);
        }

        var activeQry = helper.termCategoryQuery("active", String.valueOf(true));
        var inactiveQry = helper.termCategoryQuery("active", String.valueOf(false));

        var sektorQry = switch (ressursQueryFilter.sektor()) {
            case EKSTERN -> helper.termCategoryQuery("sektor", "EKSTERN");
            case STATLIG -> helper.termCategoryQuery("sektor", "NAV_STATLIG");
            case KOMMUNAL -> helper.termCategoryQuery("sektor", "NAV_KOMMUNAL");
            case IKKE_EKSTERN -> helper.termCategoryExcludeQuery("sektor", "EKSTERN");
            case ALLE -> new MatchAllDocsQuery();
        };

        var statusQry = switch (ressursQueryFilter.status()) {
            case AKTIV -> helper.requireAllOf(coreQry,activeQry);
            case INAKTIV -> helper.requireAllOf(coreQry,inactiveQry);
            case ALLE -> helper.requireOneOf(
                    helper.weightedCombination(coreQry, activeQry, 1.0f),
                    helper.weightedCombination(coreQry, inactiveQry, 0.5f)
            );
        };

        return helper.requireAllOf(sektorQry, statusQry);
    }

    @Override
    public String idField() {
        return "navident";
    }

    @Override
    public String typeString() {
        return "t:ressurs";
    }

    @Override
    public Analyzer analyzer() {
        return this.analyzer;
    }

    @Override
    public Query toLookupQry(LuceneRessurs ent) {
        return new BooleanQuery.Builder()
                .add(new TermQuery(new Term(this.idField(), ent.navident().toLowerCase())), BooleanClause.Occur.MUST)
                .add(new TermQuery(new Term(FIELD_TYPE, this.typeString())), BooleanClause.Occur.MUST)
                .build();
    }

    @Override
    public Class<LuceneRessurs> entityClass() {
        return LuceneRessurs.class;
    }

    private Analyzer ressursAnalyzer() {
        var analyzerPerField = Utils.analyzerPerFieldCommon(this.idField());
        analyzerPerField.put(this.idField(), Analyzers.createSimpleIgnoreCaseAnalyzer());
        analyzerPerField.put("epost", Analyzers.createNGramAnalyzer());
        analyzerPerField.put("tlf", Analyzers.createNGramAnalyzer());
        return new PerFieldAnalyzerWrapper(Analyzers.createTrimAnalyzer(), analyzerPerField);
    }

    @Override
    public Class<LuceneRessursFilter> filterClass() {
        return LuceneRessursFilter.class;
    }

    @Override
    public LuceneRessursFilter defaultFilter() {
        return new LuceneRessursFilter(LuceneRessursFilter.StatusSelection.ALLE, LuceneRessursFilter.SektorSelection.ALLE);
    }
}
