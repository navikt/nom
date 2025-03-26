package no.nav.nom.apps.admin.research;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = { Hello.class, ContextWrapper.class })
class MyDataTest {

    @Autowired Hello hello;

    @Test
    public void testHello() {
        MyData myData = MyData
            .builder()
            .id(1)
            .name("world")
            .build();

        assertEquals("Hello, world", myData.hello());

        assertEquals("Hello, Bob", myData.toBuilder().name("Bob").build().hello());

        MyData myData2 = new MyData(2, "Alice");
        myData2.name("Jane").id(3);

        assertEquals("Hello, Jane", myData2.hello());
    }
}