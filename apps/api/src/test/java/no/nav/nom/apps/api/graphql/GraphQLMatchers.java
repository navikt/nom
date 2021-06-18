package no.nav.nom.apps.api.graphql;

import com.fasterxml.jackson.databind.JsonNode;
import com.graphql.spring.boot.test.GraphQLResponse;
import lombok.val;
import org.hamcrest.CustomMatcher;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;
import org.springframework.util.CollectionUtils;

import java.util.List;

public class GraphQLMatchers {

    public static Matcher<GraphQLResponse> hasNoErrors() {
        return new CustomMatcher<>("Errors should be empty") {
            @Override
            public boolean matches(Object actual) {
                val response = ((GraphQLResponse) actual);
                if (response.getRawResponse().getBody().contains("errors")) {
                    List<JsonNode> list = response.getList("$.errors", JsonNode.class);
                    describeTo(new StringDescription().appendValueList("[ ", ", ", " ]", list));
                    return CollectionUtils.isEmpty(list);
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
                return !CollectionUtils.isEmpty(response.getList("$.errors", JsonNode.class));
            }
        };
    }
}
