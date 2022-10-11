package no.nav.nom.apps.api.graphql;

import org.hamcrest.CustomTypeSafeMatcher;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.util.CollectionUtils;

public class GraphQLMatchers {

    public static Matcher<GraphQlTester.Response> hasNoErrors() {
        return new CustomTypeSafeMatcher<>("Errors should be empty") {
            final ThreadLocal<Boolean> match = ThreadLocal.withInitial(() -> true);

            @Override
            public boolean matchesSafely(GraphQlTester.Response response) {
                response.errors().satisfy(responseErrors -> {
                    describeTo(new StringDescription().appendValueList("[ ", ", ", " ]", responseErrors));
                    match.set(CollectionUtils.isEmpty(responseErrors));
                });
                return match.get();
            }
        };
    }

    public static Matcher<GraphQlTester.Response> hasErrors() {
        return new CustomTypeSafeMatcher<>("Errors should be present") {
            final ThreadLocal<Boolean> match = ThreadLocal.withInitial(() -> true);

            @Override
            public boolean matchesSafely(GraphQlTester.Response response) {
                response.errors().satisfy(responseErrors -> {
                    match.set(!CollectionUtils.isEmpty(responseErrors));
                });

                return match.get();
            }
        };
    }
}
