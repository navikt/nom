package no.nav.nom.libs.lucenewrapper.search;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import java.util.HashMap;
import java.util.Map;

import static java.util.Map.entry;
import static no.nav.nom.libs.lucenewrapper.search.Analyzers.createMetaphoneAnalyzer;
import static no.nav.nom.libs.lucenewrapper.search.Analyzers.createNGramAnalyzer;
import static no.nav.nom.libs.lucenewrapper.search.Analyzers.createSimpleIgnoreCaseAnalyzer;

public interface IEntityIndexer<T, U extends QueryFilter<T>> {

    String FIELD_NAME_NGRAM = "name_ngram";
    String FIELD_NAME_METAPHONE = "name_metaphone";
    String FIELD_NAME_EXACT = "name";
    String FIELD_TYPE = "type";

    Document toDoc(T entity);

    Query toQuery(String terms, U queryFilter) throws ParseException;

    default Query toQuery(String terms, QueryFilterContext context) throws ParseException {
        var filter = context.getByEntityClass(this.entityClass());
        if (filter == null) {
            throw new IllegalStateException("Filter context lacks filter for type " + this.entityClass().getName());
        }

        return switch (filter.mode()) {
            case EXCLUDE -> new MatchNoDocsQuery();
            case INCLUDE -> toQuery(terms, this.defaultFilter());
            case CUSTOM -> toQuery(terms, (U) filter);
        };
    }

    String idField();

    String typeString();

    Analyzer analyzer();

    /**
     * @param ent
     * @return a query that can be used to lookup exactly the lucene document corresponding to this entity
     */
    Query toLookupQry(T ent);

    Class<T> entityClass();

    Class<U> filterClass();

    U defaultFilter();


    default Utils createUtils() {
        return new Utils(this);
    }

    class Utils {

        private final IEntityIndexer<?, ?> indexer;


        private Utils(IEntityIndexer<?, ?> indexer) {
            this.indexer = indexer;
        }

        public Query nameQuery(String terms) throws ParseException {

            var qp1 = defaultToAndQueryParser(FIELD_NAME_NGRAM, indexer.analyzer());
            var qp2 = defaultToAndQueryParser(FIELD_NAME_METAPHONE, indexer.analyzer());
            var qp3 = defaultToAndQueryParser(FIELD_NAME_EXACT, indexer.analyzer());
            var qp4 = defaultToAndQueryParser(indexer.idField(), indexer.analyzer());

            return new BooleanQuery.Builder()
                .add(qp1.parse(terms), BooleanClause.Occur.SHOULD)
                .add(new BoostQuery(qp2.parse(terms), 0.5f), BooleanClause.Occur.SHOULD)
                .add(new BoostQuery(qp3.parse(terms), 5.0f), BooleanClause.Occur.SHOULD)
                .add(new BoostQuery(qp4.parse(terms), 40.0f), BooleanClause.Occur.SHOULD)
                .build();
        }


        public Document createDocumentWithCommonFields(String id, String name) {
            var doc = new Document();
            doc.add(new Field(FIELD_TYPE, this.indexer.typeString(), TextField.TYPE_STORED));
            doc.add(new Field(this.indexer.idField(), id, TextField.TYPE_STORED));

            doc.add(new Field(IEntityIndexer.FIELD_NAME_NGRAM, name, TextField.TYPE_NOT_STORED));
            doc.add(new Field(IEntityIndexer.FIELD_NAME_METAPHONE, name, TextField.TYPE_NOT_STORED));
            doc.add(new Field(IEntityIndexer.FIELD_NAME_EXACT, name, TextField.TYPE_STORED));

            return doc;
        }

        public static Map<String, Analyzer> analyzerPerFieldCommon(String idField) {
            var ignoreCaseAnalyzer = createSimpleIgnoreCaseAnalyzer();

            return new HashMap<>(Map.ofEntries(
                entry(idField, ignoreCaseAnalyzer),
                entry(FIELD_TYPE, ignoreCaseAnalyzer),
                entry(FIELD_NAME_METAPHONE, createMetaphoneAnalyzer()),
                entry(FIELD_NAME_NGRAM, createNGramAnalyzer()),
                entry(FIELD_NAME_EXACT, ignoreCaseAnalyzer)
            ));
        }


        public Query termCategoryQuery(String field, String requiredValue) {
            //            var g = indexer.analyzer().tokenStream(field,requiredValue);
            return new TermQuery(new Term(field, requiredValue));
        }

        public Query termCategoryExcludeQuery(String field, String requiredValue) {
            var t = termCategoryQuery(field, requiredValue);
            return new BooleanQuery.Builder()
                .add(t, BooleanClause.Occur.MUST_NOT)
                .build();
        }

        public Query weightedCombination(Query lookupQuery, Query categorySeparatorQuery, float boost) {
            return new BoostQuery(requireAllOf(lookupQuery, categorySeparatorQuery), boost);
        }

        public Query requireAllOf(Query... queries) {
            var b = new BooleanQuery.Builder();
            for (var q : queries) {
                addPlainOrWrappedQueryToBuilder(b, q, BooleanClause.Occur.MUST);
            }
            return b.build();
        }

        public Query requireOneOf(Query... queries) {
            var b = new BooleanQuery.Builder();
            for (var q : queries) {
                addPlainOrWrappedQueryToBuilder(b, q, BooleanClause.Occur.SHOULD);
            }
            return b.build();
        }

        private void addPlainOrWrappedQueryToBuilder(BooleanQuery.Builder b, Query q, BooleanClause.Occur defaultOccur) {
            if (q instanceof BooleanQuery && ((BooleanQuery) q).clauses().size() == 1) {
                var clause = ((BooleanQuery) q).clauses().getFirst();
                var occur = clause.occur();
                var subqry = clause.query();
                b.add(subqry, occur);
            } else {
                b.add(q, defaultOccur);
            }
        }

        private QueryParser defaultToAndQueryParser(String fieldname, Analyzer analyzer) {
            var out = new QueryParser(fieldname, analyzer);
            out.setDefaultOperator(QueryParser.Operator.AND);
            return out;
        }
    }
}
