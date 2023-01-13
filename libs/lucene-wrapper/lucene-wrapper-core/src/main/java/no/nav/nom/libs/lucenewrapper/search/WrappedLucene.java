package no.nav.nom.libs.lucenewrapper.search;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WrappedLucene {

    Logger logger = LoggerFactory.getLogger(this.getClass());

    private ByteBuffersDirectory bbdir;
    private final Map<Class<?>, IEntityIndexer<?, ?>> indexers;

    private final Map<String,Class<?>> mapTypestrToEntclass = new HashMap<>();
    public static final int DEFAULT_RESULT_SIZE = 30;

    public WrappedLucene(List<IEntityIndexer<?, ?>> indexers) {
        this.bbdir = new ByteBuffersDirectory();
        validateIndexerInterplay(indexers);
        this.indexers = indexers.stream().collect(Collectors.toMap(IEntityIndexer::entityClass, Function.identity()));
        indexers.forEach(ind -> mapTypestrToEntclass.put(ind.typeString(),ind.entityClass()));
    }

    private void validateIndexerInterplay(List<IEntityIndexer<?, ?>> indexers) {
        Set<String> typeNames = new HashSet<>();
        Set<String> classNames = new HashSet<>();
        for (var indexer : indexers) {
            var typestr = indexer.typeString();
            var addedTypestr = typeNames.add(typestr);
            if (!addedTypestr) {
                throw new IllegalStateException("More than one indexer shared the same typestring: " + typestr);
            }
            var className = indexer.entityClass().getName();
            var addedClassName = classNames.add(className);
            if (!addedClassName) {
                throw new IllegalStateException("More than one indexer shared the same entity class: " + className);
            }
        }
    }

    public <T> void upsertEntity(T ent, IEntityIndexer<T, ?> indexEntity) {
        this.upsertEntities(Stream.of(ent), indexEntity);
    }

    public <T> void upsertEntity(T ent) {
        var indexer = this.indexers.get(ent.getClass());
        if (indexer == null) {
            throw new IllegalStateException("No indexer registered for type " + ent.getClass().getName());
        }
        this.upsertEntity(ent, (IEntityIndexer<T, ?>) indexer);
    }

    public <T> void upsertEntities(Stream<T> entStream, Class<T> clazz) {
        var indexer = this.indexers.get(clazz);
        if (indexer == null) {
            throw new IllegalStateException("No indexer registered for type " + clazz.getName());
        }
        this.upsertEntities(entStream, (IEntityIndexer<T, ?>) indexer);
    }

    public <T> void upsertEntities(Stream<T> entStream, IEntityIndexer<T, ?> entityIndexer) {
        verifyPreRegistered(entityIndexer);
        var iwConf = new IndexWriterConfig(entityIndexer.analyzer());
        try {
            IndexWriter iw = new IndexWriter(bbdir, iwConf);
            entStream.forEach(ent -> {
                var doc = entityIndexer.toDoc(ent);
                try {
                    var idQuery = entityIndexer.toLookupQry(ent);
                    iw.deleteDocuments(idQuery);
                    iw.addDocument(doc);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            iw.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public List<SearchResult> search(String searchterms) {
        return search(searchterms, new QueryFilterContext());
    }

    public List<SearchResult> search(String searchterms, QueryFilterContext qryFilterCtx) {
        return search(searchterms, qryFilterCtx, DEFAULT_RESULT_SIZE);
    }

    public List<SearchResult> search(String searchterms, QueryFilterContext qryFilterCtx, Integer resultLimit) {
        verifyFilterCompatability(qryFilterCtx);
        var out = new ArrayList<SearchResult>();
        try {
            IndexReader ir = DirectoryReader.open(bbdir);
            IndexSearcher is = new IndexSearcher(ir);
            var docLimit = resultLimit != null ? resultLimit : ir.maxDoc();

            out = new ArrayList<>(docLimit);

            var bqb = new BooleanQuery.Builder();

            for (var indexer : this.indexers.values()) {
                var typeRestrictQry = new TermQuery(new Term(IEntityIndexer.FIELD_TYPE, indexer.typeString()));
                var actualQry = indexer.toQuery(searchterms, qryFilterCtx);
                var combinedQry = new BooleanQuery.Builder()
                        .add(actualQry, BooleanClause.Occur.MUST)
                        .add(new BoostQuery(typeRestrictQry, 0.0f), BooleanClause.Occur.MUST)
                        .build();
                bqb.add(new BooleanClause(combinedQry, BooleanClause.Occur.SHOULD));
            }

            var bq = bqb.build();

            var res = is.search(bq, docLimit);
            for (var d : res.scoreDocs) {
                SearchResult searchRes = getSearchResult(ir, d);
                out.add(searchRes);
            }

        } catch (IOException ignored) {
            logger.warn("Possible no indexes initiated yet", ignored);
            return List.of();
        } catch (ParseException e) {
            logger.warn(e.getMessage());
            return out;
        }


        return out;
    }



    private void verifyFilterCompatability(QueryFilterContext ctx) {
        var filters = ctx.all();
        for (var filter : filters) {
            var entityClass = filter.getEntityClass();
            var indexer = indexers.get(entityClass);
            if (indexer == null) {
                throw new IllegalStateException("Filter context contains filter for entity class that is not recognized: " + entityClass.getName());
            } else {
                var desiredFilterClass = indexer.filterClass();
                if (!desiredFilterClass.equals(filter.getClass())) {
                    throw new IllegalStateException("Indexer for entity class " + entityClass.getName() + " expects filter to be of class " + desiredFilterClass + ", but found" + filter.getClass()
                            .getName());
                }
            }
        }
    }


    private <T> void verifyPreRegistered(IEntityIndexer<T, ?> entityIndexer) {
        var indexerIsNotRecognized = !this.indexers.containsKey(entityIndexer.entityClass());
        if (indexerIsNotRecognized) {
            throw new IllegalStateException("Preregistration missing for " + entityIndexer);
        }
    }

    private SearchResult getSearchResult(IndexReader ir, ScoreDoc d) throws IOException {
        var doc = ir.document(d.doc);
        var entType = doc.getField(IEntityIndexer.FIELD_TYPE).stringValue();
        var nameFromDoc = doc.get(IEntityIndexer.FIELD_NAME_EXACT);
        for (var indexer : indexers.values()) {
            if (indexer.typeString().equals(entType)) {
                var id = doc.getField(indexer.idField()).stringValue();
                return new SearchResult(id, d.score, entType, nameFromDoc, indexer.entityClass());
            }
        }
        throw new IllegalStateException("Encountered invalid type: " + entType);
    }

    /**
     * Mainly intended for test frameworks.
     * Call between tests to make each test case independent
     */
    public synchronized void dropPersistedIndex() {
        try {
            this.bbdir.close();
            this.bbdir = new ByteBuffersDirectory();
        } catch (IOException e) {
            throw new RuntimeException("Could not drop persisted lucene index");
        }
    }
}
