package no.nav.nom.apps.ui.common.security;

import lombok.val;

import javax.servlet.http.Cookie;
import java.util.Optional;

public class CookieUtils {
    public static final String ID_TOKEN_COOKIE_KEY = "nom-id-token";
    public static final String ACCESS_TOKEN_COOKIE_KEY = "nom-access-token";
    public static final String REFRESH_TOKEN_COOKIE_KEY = "nom-refresh-token";
    public static final int SEVEN_DAYS = 7 * 24 * 60 * 60;

    public static String extractCookieValue(String name, Cookie... cookies) {
        val cookie = findCookie(name, cookies);
        return cookie.map(Cookie::getValue).orElse(null);
    }

    public static Optional<Cookie> findCookie(String name, Cookie... cookies) {
        if (cookies != null) {
            for (val cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    return Optional.of(cookie);
                }
            }
        }
        return Optional.empty();
    }

    public static Cookie createCookie(String name, String value, boolean secure, int age) {
        val cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(age);
        cookie.setSecure(secure);
        return cookie;
    }

    public static Cookie createClearingCookie(String name, boolean secure) {
        return createCookie(name, "", secure, 0);
    }

}
