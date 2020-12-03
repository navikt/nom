package no.nav.nom.apps.ui.common;

import lombok.val;
import org.hamcrest.CustomMatcher;
import org.hamcrest.Matcher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesRegex;
import static org.hamcrest.Matchers.nullValue;

public class Matchers {

    public static Matcher<MultiValueMap<String, String>> hasKeyWithSingleValueThat(String key, Matcher<String> matcher) {
        return new CustomMatcher<>("") {
            @Override
            public boolean matches(Object o) {
                val obj = (MultiValueMap<String, String>) o;
                assertThat(obj.get(key), hasSize(1));
                assertThat(obj.get(key).get(0), matcher);
                return true;
            }
        };
    }

    public static Matcher<ResponseEntity<?>> isOnlyRedirectTo(String uri) {
        return new CustomMatcher<>("") {
            @Override
            public boolean matches(Object o) {
                val obj = (ResponseEntity<?>) o;
                assertThat(obj.getStatusCode(), is(HttpStatus.FOUND));
                assertThat(obj.getBody(), is(nullValue()));
                assertThat(obj.getHeaders(), hasKeyWithSingleValueThat("Location", matchesRegex(uri)));
                return true;
            }
        };
    }
}
