package no.nav.nom.libs.lucenewrapper.search;

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
import java.util.function.Function;

public class GenericEntityIndexer<T> implements IEntityIndexer<T> {
    private final Analyzer analyzer = genericAnalyzer();
    private final Class<T> clazz;
    private final Function<T, String> extractId;
    private final Function<T, String> extractName;
    
    private final String typename;

    public GenericEntityIndexer(Class<T> clazz, Function<T,String> extractId, Function<T,String> extractName, String typename){
        this.clazz = clazz;
        this.extractId = extractId;
        this.extractName = extractName;
        this.typename = typename;
    }

    @Override
    public Document toDoc(T entity) {
        var doc = new Document();
        doc.add(new Field(IEntityIndexer.FIELD_TYPE, this.typeStr(), TextField.TYPE_STORED));

        doc.add(new Field(IEntityIndexer.FIELD_NAME_NGRAM, this.extractName.apply(entity), TextField.TYPE_NOT_STORED));
        doc.add(new Field(IEntityIndexer.FIELD_NAME_METAPHONE, this.extractName.apply(entity), TextField.TYPE_NOT_STORED));
        doc.add(new Field(IEntityIndexer.FIELD_NAME_EXACT, this.extractName.apply(entity), TextField.TYPE_STORED));
        doc.add(new Field(this.idField(), this.extractId.apply(entity), TextField.TYPE_STORED));
        doc.add(new Field("enhettype", this.typename, TextField.TYPE_NOT_STORED));
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

        var qp1 = new QueryParser(IEntityIndexer.FIELD_NAME_NGRAM, this.analyzer);
        var qp2 = new QueryParser(IEntityIndexer.FIELD_NAME_METAPHONE, this.analyzer);
        var qp3 = new QueryParser(IEntityIndexer.FIELD_NAME_EXACT, this.analyzer);

        return new BooleanQuery.Builder()
                .add(new BooleanClause(qp1.parse(strterms), BooleanClause.Occur.SHOULD))
                .add(new BooleanClause(qp2.parse(strterms), BooleanClause.Occur.SHOULD))
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
    public Query toLookupQry(T ent) {
        return new BooleanQuery.Builder()
                .add(new TermQuery(new Term(this.idField(), this.extractId.apply(ent))), BooleanClause.Occur.MUST)
                .add(new TermQuery(new Term(IEntityIndexer.FIELD_TYPE, this.typeStr())), BooleanClause.Occur.MUST)
                .build();
    }

    @Override
    public Class<T> clazz() {
        return this.clazz;
    }

    private Analyzer genericAnalyzer() {
        var analyzerPerField = new HashMap<String, Analyzer>();
        analyzerPerField.put(this.idField(), Analyzers.createSimpleIgnoreCaseAnalyzer());
        analyzerPerField.put(IEntityIndexer.FIELD_NAME_METAPHONE, Analyzers.createMetaphoneAnalyzer());
        analyzerPerField.put(IEntityIndexer.FIELD_NAME_NGRAM, Analyzers.createNGramAnalyzer());
        return new PerFieldAnalyzerWrapper(Analyzers.createSimpleIgnoreCaseAnalyzer(), analyzerPerField);
    }

}
