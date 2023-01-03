package no.nav.nom.libs.lucenewrapper;

import no.nav.nom.libs.lucenewrapper.search.GenericEntityIndexer;
import no.nav.nom.libs.lucenewrapper.search.QueryFilter;
import no.nav.nom.libs.lucenewrapper.search.WrappedLucene;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

public class MainTest {

    @Test
    void genericDataSearch(){
        var entIndexer = new GenericEntityIndexer<>(
                GenericData.class,
                entity -> entity.id,
                entity -> entity.name,
                "t:GenericData",
                GenericDataFilter.class
        );

        var repo = new WrappedLucene(List.of(entIndexer));

        var one = new GenericData("1", "one", 5);
        var two = new GenericData("2", "two", 67);
        var three = new GenericData("3", "actually three", 12);

        repo.upsertEntity(one);
        repo.upsertEntities(Stream.of(two,three),GenericData.class);

        var resList = repo.search("tw");
        Assertions.assertEquals(1, resList.size());
        Assertions.assertEquals(two.id, resList.get(0).id());

        var resList2 = repo.search("one");
        Assertions.assertEquals(1, resList2.size());
        Assertions.assertEquals(one.id, resList2.get(0).id());
    }

    record GenericData(String id, String name, int number){

    }

    record GenericDataFilter() implements QueryFilter<GenericData> {

        @Override
        public Class<GenericData> getEntityClass() {
            return GenericData.class;
        }

        @Override
        public Mode mode() {
            return Mode.INCLUDE;
        }
    }

}
