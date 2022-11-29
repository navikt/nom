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

public class OrgEnhetIndexer implements IEntityIndexer<LuceneOrgEnhet> {

    private final Analyzer analyzer = orgenhetAnalyzer();

    @Override
    public Document toDoc(LuceneOrgEnhet entity) {
        var doc = new Document();
        doc.add(new Field(IEntityIndexer.FIELD_TYPE, this.typeStr(), TextField.TYPE_STORED));

        doc.add(new Field(IEntityIndexer.FIELD_NAME_NGRAM, entity.navn(), TextField.TYPE_NOT_STORED));
        doc.add(new Field(IEntityIndexer.FIELD_NAME_METAPHONE, entity.navn(), TextField.TYPE_NOT_STORED));
        doc.add(new Field(IEntityIndexer.FIELD_NAME_EXACT, entity.navn(), TextField.TYPE_STORED));
        doc.add(new Field(this.idField(), entity.id(), TextField.TYPE_STORED));
        doc.add(new Field("enhettype", entity.enhetType(), TextField.TYPE_NOT_STORED));
        return doc;
    }

    @Override
    public Query toQuery(String strterms) throws ParseException {
//            var phr = new PhraseQuery.Builder();
//            phr.setSlop(4);
//            var strTermArr = strterms.split(" +");
//            for (String s : strTermArr) {
//                var t = new Term(IndexEntity.FIELD_NAME_NGRAM, s);
//                phr.add(t);
//            }
//            return phr.build();

        var qp1 = localQueryParser(IEntityIndexer.FIELD_NAME_NGRAM);
        var qp2 = localQueryParser(IEntityIndexer.FIELD_NAME_METAPHONE);
        var qp3 = localQueryParser(IEntityIndexer.FIELD_NAME_EXACT);

        return new BooleanQuery.Builder()
                .add(new BooleanClause(qp1.parse(strterms), BooleanClause.Occur.SHOULD))
                .add(new BooleanClause(new BoostQuery(qp2.parse(strterms),0.5f), BooleanClause.Occur.SHOULD))
                .add(new BooleanClause(new BoostQuery(qp3.parse(strterms), 5.0f), BooleanClause.Occur.SHOULD))
                .build();

    }

    @Override
    public String idField() {
        return "orgenhetid";
    }

    @Override
    public String typeStr() {
        return "t:orgenhet";
    }

    @Override
    public Analyzer analyzer() {
        return this.analyzer;
    }

    @Override
    public Query toLookupQry(LuceneOrgEnhet ent) {
        return new BooleanQuery.Builder()
                .add(new TermQuery(new Term(this.idField(), ent.id().toLowerCase())), BooleanClause.Occur.MUST)
                .add(new TermQuery(new Term(IEntityIndexer.FIELD_TYPE, this.typeStr())), BooleanClause.Occur.MUST)
                .build();
    }

    @Override
    public Class<LuceneOrgEnhet> clazz() {
        return LuceneOrgEnhet.class;
    }

    private Analyzer orgenhetAnalyzer() {
        var analyzerPerField = new HashMap<String, Analyzer>();
        analyzerPerField.put(this.idField(), Analyzers.createSimpleIgnoreCaseAnalyzer());
        analyzerPerField.put(IEntityIndexer.FIELD_NAME_METAPHONE, Analyzers.createMetaphoneAnalyzer());
        analyzerPerField.put(IEntityIndexer.FIELD_NAME_NGRAM, Analyzers.createNGramAnalyzer());
        return new PerFieldAnalyzerWrapper(Analyzers.createSimpleIgnoreCaseAnalyzer(), analyzerPerField);
    }

    private QueryParser localQueryParser(String fieldname){
        var out = new QueryParser(fieldname,this.analyzer);
        out.setDefaultOperator(QueryParser.Operator.AND);
        return out;
    }


}
