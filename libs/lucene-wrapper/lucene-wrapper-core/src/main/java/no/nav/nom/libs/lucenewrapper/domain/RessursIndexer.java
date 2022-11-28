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
        doc.add(new Field(IEntityIndexer.FIELD_TYPE, this.typeStr(), TextField.TYPE_STORED));
        doc.add(new Field(IEntityIndexer.FIELD_NAME_NGRAM, entity.navn(), TextField.TYPE_NOT_STORED));
        doc.add(new Field(IEntityIndexer.FIELD_NAME_METAPHONE, entity.navn(), TextField.TYPE_NOT_STORED));
        doc.add(new Field(IEntityIndexer.FIELD_NAME_EXACT, entity.navn(), TextField.TYPE_STORED));
        doc.add(new Field(this.idField(), entity.navident(), TextField.TYPE_STORED));

        doc.add(new Field("epost", entity.epost(), TextField.TYPE_NOT_STORED));
        doc.add(new Field("tlf", entity.tlf(), TextField.TYPE_NOT_STORED));
        doc.add(new Field("active", String.valueOf(entity.active()), TextField.TYPE_NOT_STORED));
        return doc;
    }

    @Override
    public Query toQuery(String terms) throws ParseException {
        var qp1 = new QueryParser(IEntityIndexer.FIELD_NAME_NGRAM, this.analyzer);
        var qp2 = new QueryParser(IEntityIndexer.FIELD_NAME_METAPHONE, this.analyzer);
        var qp3 = new QueryParser(IEntityIndexer.FIELD_NAME_EXACT, this.analyzer);
        var qp4 = new QueryParser(this.idField(), this.analyzer);
        var boostActiveQry = new BoostQuery(new TermQuery(new Term("active",String.valueOf(false))),0.1f);

        var coreQry = new BooleanQuery.Builder()
                .add(new BooleanClause(qp1.parse(terms), BooleanClause.Occur.SHOULD))
                .add(new BooleanClause(qp2.parse(terms), BooleanClause.Occur.SHOULD))
                .add(new BooleanClause(new BoostQuery(qp3.parse(terms), 5.0f), BooleanClause.Occur.SHOULD))
                .add(new BooleanClause(new BoostQuery(qp4.parse(terms), 40.0f), BooleanClause.Occur.SHOULD))
                .build();

        return new BooleanQuery.Builder()
                .add(new BooleanClause(coreQry, BooleanClause.Occur.MUST))
                .add(new BooleanClause(boostActiveQry, BooleanClause.Occur.SHOULD))
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
                .add(new TermQuery(new Term(IEntityIndexer.FIELD_TYPE,this.typeStr())), BooleanClause.Occur.MUST)
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
        analyzerPerField.put(IEntityIndexer.FIELD_NAME_METAPHONE, Analyzers.createMetaphoneAnalyzer());
        analyzerPerField.put(IEntityIndexer.FIELD_NAME_NGRAM, Analyzers.createNGramAnalyzer());
        return new PerFieldAnalyzerWrapper(Analyzers.createSimpleIgnoreCaseAnalyzer(), analyzerPerField);
    }

}
