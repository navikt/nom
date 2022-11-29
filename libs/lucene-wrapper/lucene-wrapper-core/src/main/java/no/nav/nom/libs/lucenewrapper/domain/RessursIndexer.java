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
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import java.util.HashMap;

public class RessursIndexer implements IEntityIndexer<LuceneRessurs> {

    private final Analyzer analyzer = ressursAnalyzer();

    @Override
    public Document toDoc(LuceneRessurs entity) {
        var doc = new Document();
        doc.add(new Field(FIELD_TYPE, this.typeStr(), TextField.TYPE_STORED));
        doc.add(new Field(FIELD_NAME_NGRAM, entity.navn(), TextField.TYPE_NOT_STORED));
        doc.add(new Field(FIELD_NAME_METAPHONE, entity.navn(), TextField.TYPE_NOT_STORED));
        doc.add(new Field(FIELD_NAME_EXACT, entity.navn(), TextField.TYPE_STORED));
        doc.add(new Field(this.idField(), entity.navident(), TextField.TYPE_STORED));

        doc.add(new Field("epost", entity.epost(), TextField.TYPE_NOT_STORED));
        doc.add(new Field("tlf", entity.tlf(), TextField.TYPE_NOT_STORED));
        doc.add(new Field("active", String.valueOf(entity.active()), TextField.TYPE_NOT_STORED));
        return doc;
    }

    @Override
    public Query toQuery(String terms) throws ParseException {

        var qp1 = localQueryParser(FIELD_NAME_NGRAM);
        var qp2 = localQueryParser(FIELD_NAME_METAPHONE);
        var qp3 = localQueryParser(FIELD_NAME_EXACT);
        var qp4 = localQueryParser(this.idField());

        var activeQry = new TermQuery(new Term("active", String.valueOf(true)));
        var inactiveQry = new TermQuery(new Term("active", String.valueOf(false)));

        var coreQry = new BooleanQuery.Builder()
                .add(qp1.parse(terms), BooleanClause.Occur.SHOULD)
                .add(new BoostQuery(qp2.parse(terms),0.5f), BooleanClause.Occur.SHOULD)
                .add(new BoostQuery(qp3.parse(terms), 5.0f), BooleanClause.Occur.SHOULD)
                .add(new BoostQuery(qp4.parse(terms), 40.0f), BooleanClause.Occur.SHOULD)
                .build();

        var normalWeightActiveQry = new BoostQuery(
                new BooleanQuery.Builder()
                        .add(activeQry, BooleanClause.Occur.MUST)
                        .add(coreQry, BooleanClause.Occur.MUST)
                        .build()
        ,1.0f);


        var reducedWeightInactiveQry = new BoostQuery(
                new BooleanQuery.Builder()
                        .add(inactiveQry, BooleanClause.Occur.MUST)
                        .add(coreQry, BooleanClause.Occur.MUST)
                        .build()
        ,0.5f);

        return new BooleanQuery.Builder()
                .add(normalWeightActiveQry, BooleanClause.Occur.SHOULD)
                .add(reducedWeightInactiveQry, BooleanClause.Occur.SHOULD)
                .build();
    }

    @Override
    public String idField() {
        return "navident";
    }

    @Override
    public String typeStr() {
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
                .add(new TermQuery(new Term(FIELD_TYPE, this.typeStr())), BooleanClause.Occur.MUST)
                .build();
    }

    @Override
    public Class<LuceneRessurs> clazz() {
        return LuceneRessurs.class;
    }

    private Analyzer ressursAnalyzer() {
        var analyzerPerField = new HashMap<String, Analyzer>();
        analyzerPerField.put(this.idField(), Analyzers.createSimpleIgnoreCaseAnalyzer());
        analyzerPerField.put("epost", Analyzers.createNGramAnalyzer());
        analyzerPerField.put("tlf", Analyzers.createNGramAnalyzer());
        analyzerPerField.put(FIELD_NAME_METAPHONE, Analyzers.createMetaphoneAnalyzer());
        analyzerPerField.put(FIELD_NAME_NGRAM, Analyzers.createNGramAnalyzer());
        return new PerFieldAnalyzerWrapper(Analyzers.createSimpleIgnoreCaseAnalyzer(), analyzerPerField);
    }

    private QueryParser localQueryParser(String fieldname){
        var out = new QueryParser(fieldname,this.analyzer);
        out.setDefaultOperator(QueryParser.Operator.AND);
        return out;
    }

}
