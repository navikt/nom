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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WrappedLucene {

    private final ByteBuffersDirectory bbdir = new ByteBuffersDirectory();
    private final Map<Class<?>, IEntityIndexer<?>> indexers;

    public WrappedLucene(List<IEntityIndexer<?>> indexers) {
        this.indexers = indexers.stream().collect(Collectors.toMap(IEntityIndexer::clazz, Function.identity()));
    }

    public <T> void upsertEntity(T ent, IEntityIndexer<T> indexEntity) {
        this.upsertEntities(Stream.of(ent), indexEntity);
    }

    public <T> void upsertEntity(T ent) {
        var indexer = this.indexers.get(ent.getClass());
        if (indexer == null) {
            throw new IllegalStateException("No indexer registered for type " + ent.getClass().getName());
        }
        this.upsertEntity(ent, (IEntityIndexer<T>) indexer);
    }

    public <T> void upsertEntities(Stream<T> entStream, Class<T> clazz) {
        var indexer = this.indexers.get(clazz);
        if (indexer == null) {
            throw new IllegalStateException("No indexer registered for type " + clazz.getName());
        }
        this.upsertEntities(entStream, (IEntityIndexer<T>) indexer);
    }

    public <T> void upsertEntities(Stream<T> entStream, IEntityIndexer<T> entityIndexer) {
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

    private <T> void verifyPreRegistered(IEntityIndexer<T> entityIndexer) {
        var indexerIsRecognized = this.indexers.containsKey(entityIndexer.clazz());
        if (!indexerIsRecognized) {
            throw new IllegalStateException("Preregistration missing of " + entityIndexer);
        }
    }


    public List<SearchResult> search(String searchterms) {
        return search(searchterms, Collections.emptyList());
    }

    public List<SearchResult> search(String searchterms, List<Class<?>> classFilter) {
        var sz = 30;
        var out = new ArrayList<SearchResult>(sz);
        try {
            IndexReader ir = DirectoryReader.open(bbdir);
            IndexSearcher is = new IndexSearcher(ir);

            var currentIndexers = indexersForDomainClasses(classFilter);

            var bqb = new BooleanQuery.Builder();
            for (var indexer : currentIndexers) {
                var typeRestrictQry = new TermQuery(new Term(IEntityIndexer.FIELD_TYPE, indexer.typeStr()));
                var actualQry = indexer.toQuery(searchterms);
                var combinedQry = new BooleanQuery.Builder().add(new BooleanClause(actualQry, BooleanClause.Occur.MUST))
                        .add(new BooleanClause(new BoostQuery(typeRestrictQry, 0.0f), BooleanClause.Occur.MUST))
                        .build();
                bqb.add(new BooleanClause(combinedQry, BooleanClause.Occur.SHOULD));
            }
            var bq = bqb.build();


            var res = is.search(bq, sz);
            for (var d : res.scoreDocs) {
                SearchResult searchRes = getSearchResult(ir, d);
                out.add(searchRes);
            }

            return out;
        } catch (IOException | ParseException ignored) {
            return List.of();
        }
    }

    private Collection<IEntityIndexer<?>> indexersForDomainClasses(List<Class<?>> filter) {
        if (filter.isEmpty()) {
            return this.indexers.values();
        } else {
            return this.indexers.values().stream().filter(it -> filter.contains(it.clazz())).toList();
        }
    }

    private SearchResult getSearchResult(IndexReader ir, ScoreDoc d) throws IOException {
        var doc = ir.document(d.doc);
        var entType = doc.getField(IEntityIndexer.FIELD_TYPE).stringValue();
        var nameFromDoc = doc.get(IEntityIndexer.FIELD_NAME_EXACT);
        for (var indexer : indexers.values()) {
            if (indexer.typeStr().equals(entType)) {
                var id = doc.getField(indexer.idField()).stringValue();
                return new SearchResult(id, d.score, entType, nameFromDoc);
            }
        }
        throw new IllegalStateException("Encountered invalid type: " + entType);
    }
}
