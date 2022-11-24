package no.nav.nom.libs.lucenewrapper.search;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;

public interface IEntityIndexer<T> {

    String FIELD_NAME_NGRAM = "name_ngram";
    String FIELD_NAME_METAPHONE = "name_metaphone";
    String FIELD_NAME_EXACT = "name";
    String FIELD_TYPE = "type";

    Document toDoc(T entity);

    Query toQuery(String terms) throws ParseException;

    String idField();

    String typeStr();

    Analyzer analyzer();

    /**
     *
     * @param ent
     * @return
     */
    Query toLookupQry(T ent);

    Class<T> clazz();
}
