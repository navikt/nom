package no.nav.nom.apps.api.graphql;

import com.graphql.spring.boot.test.GraphQLResponse;
import lombok.val;
import org.hamcrest.CustomMatcher;
import org.hamcrest.Matcher;
import org.springframework.util.CollectionUtils;

public class GraphQLMatchers {
    public static Matcher<GraphQLResponse> hasNoErrors() {
        return new CustomMatcher<>("Errors should be empty") {
            @Override
            public boolean matches(Object actual) {
                val response = ((GraphQLResponse) actual);
                if(response.getRawResponse().getBody().contains("errors")) {
                    return CollectionUtils.isEmpty(response.getList("$.errors", String.class));
                }
                return true;
            }
        };
    }

    public static Matcher<GraphQLResponse> hasErrors() {
        return new CustomMatcher<>("Errors should be present") {
            @Override
            public boolean matches(Object actual) {
                val response = ((GraphQLResponse) actual);
                return !CollectionUtils.isEmpty(response.getList("$.errors", String.class));
            }
        };
    }
}
