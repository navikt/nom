package no.nav.nom.libs.lucenewrapper.search;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.ByteBuffersDirectory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class SearchRepo {

    ByteBuffersDirectory bbdir = new ByteBuffersDirectory();
    private final List<IEntityIndexer<?>> indexerList = new ArrayList<>();

    public SearchRepo(List<IEntityIndexer<?>> indexerList) {
        this.indexerList.addAll(indexerList);
    }

    public <T> void upsertEntity(T ent, IEntityIndexer<T> indexEntity) {
        this.upsertEntities(Stream.of(ent), indexEntity);
    }

    public <T> void upsertEntity(T ent) {
        for(var indexer : this.indexerList){
            if(indexer.clazz().equals(ent.getClass())){
                this.upsertEntity(ent, (IEntityIndexer<T>) indexer);
                return;
            }
        }
        throw new IllegalStateException("No indexer registered for type " + ent.getClass().getName());
    }

    public <T> void upsertEntities(Stream<T> entStream, Class<T> clazz){
        for(var indexer : this.indexerList){
            if(indexer.clazz().equals(clazz)){
                this.upsertEntities(entStream, (IEntityIndexer<T>) indexer);
                return;
            }
        }
        throw new IllegalStateException("No indexer registered for type " + clazz.getName());
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
        var indexerIsRecognized = this.indexerList.contains(entityIndexer);
        if (!indexerIsRecognized) {
            throw new IllegalStateException("Preregistration missing of " + entityIndexer);
        }
    }

    public List<SearchResult> search(String searchterms) {
        var sz = 30;
        var out = new ArrayList<SearchResult>(sz);
        try {
            IndexReader ir = DirectoryReader.open(bbdir);
            IndexSearcher is = new IndexSearcher(ir);

            var bqb = new BooleanQuery.Builder();
            for (var indexer : this.indexerList) {
                bqb.add(new BooleanClause(indexer.toQuery(searchterms), BooleanClause.Occur.SHOULD));
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

    private SearchResult getSearchResult(IndexReader ir, ScoreDoc d) throws IOException {
        var doc = ir.document(d.doc);
        var entType = doc.getField(IEntityIndexer.FIELD_TYPE).stringValue();
        var nameFromDoc = doc.get(IEntityIndexer.FIELD_NAME_EXACT);
        for (var indexer : indexerList) {
            if (indexer.typeStr().equals(entType)) {
                var id = doc.getField(indexer.idField()).stringValue();
                return new SearchResult(id, d.score, entType, nameFromDoc);
            }
        }
        throw new IllegalStateException("Encountered invalid type: " + entType);
    }

}
