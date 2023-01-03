package no.nav.nom.libs.lucenewrapper.search;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import java.util.HashMap;
import java.util.function.Function;

public class GenericEntityIndexer<T, U extends QueryFilter<T>> implements IEntityIndexer<T, U> {
    private final Analyzer analyzer;
    private final Class<T> entityClass;
    private final Function<T, String> extractId;
    private final Function<T, String> extractName;

    private final String typename;
    private final Class<U> filterClass;
    private final Utils utils;

    public GenericEntityIndexer(Class<T> entityClass, Function<T, String> extractId, Function<T, String> extractName, String typename, Class<U> filterClass) {
        this.entityClass = entityClass;
        this.extractId = extractId;
        this.extractName = extractName;
        this.typename = typename;
        this.filterClass = filterClass;
        this.analyzer = genericAnalyzer();
        this.utils = createUtils();
    }

    @Override
    public Document toDoc(T entity) {
        var doc = utils.createDocumentWithCommonFields(this.extractId.apply(entity), this.extractName.apply(entity));
        doc.add(new Field("enhettype", this.typename, TextField.TYPE_NOT_STORED));
        return doc;
    }

    @Override
    public Query toQuery(String strterms, U ignored) throws ParseException {
        //            var phr = new PhraseQuery.Builder();
        //            phr.setSlop(4);
        //            var strTermArr = strterms.split(" +");
        //            for (String s : strTermArr) {
        //                var t = new Term(IndexEntity.FIELD_NAME_NGRAM, s);
        //                phr.add(t);
        //            }
        //            return phr.build();

        return this.utils.nameQuery(strterms);

    }

    @Override
    public String idField() {
        return "orgenhetid";
    }

    @Override
    public String typeString() {
        return "t:orgenhet";
    }

    @Override
    public Analyzer analyzer() {
        return this.analyzer;
    }

    @Override
    public Query toLookupQry(T ent) {
        return new BooleanQuery.Builder().add(new TermQuery(new Term(this.idField(), this.extractId.apply(ent))), BooleanClause.Occur.MUST)
            .add(new TermQuery(new Term(IEntityIndexer.FIELD_TYPE, this.typeString())), BooleanClause.Occur.MUST)
            .build();
    }


    @Override
    public Class<T> entityClass() {
        return this.entityClass;
    }

    @Override
    public Class<U> filterClass() {
        return this.filterClass;
    }

    @Override
    public U defaultFilter() {
        return null;
    }

    private Analyzer genericAnalyzer() {
        var analyzerPerField = new HashMap<String, Analyzer>();
        analyzerPerField.put(this.idField(), Analyzers.createSimpleIgnoreCaseAnalyzer());
        analyzerPerField.put(IEntityIndexer.FIELD_NAME_METAPHONE, Analyzers.createMetaphoneAnalyzer());
        analyzerPerField.put(IEntityIndexer.FIELD_NAME_NGRAM, Analyzers.createNGramAnalyzer());
        return new PerFieldAnalyzerWrapper(Analyzers.createSimpleIgnoreCaseAnalyzer(), analyzerPerField);
    }


}
