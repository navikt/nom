package no.nav.nom.libs.utils.common.unleash;

import io.getunleash.strategy.Strategy;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class ByUserIdStrategy implements Strategy {

    private final boolean securityEnabled;

    public ByUserIdStrategy(Environment environment) {
        securityEnabled = !Arrays.stream(environment.getActiveProfiles()).toList().contains("local");
    }

    @Override
    public String getName() {
        return "byUserId";
    }

    @Override
    public boolean isEnabled(Map<String, String> parameters) {
        return ToggleChecker.isToggleEnabled("user", parameters, this::isUser);
    }

    private boolean isUser(String userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return Objects.equals(userId, getUserNavident().orElse("NOBODY"));
    }

    private Optional<String> getUserNavident() {
        if (securityEnabled) {
            final var auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth instanceof JwtAuthenticationToken token) {
                var azp_name = token.getTokenAttributes().get("NAVident");
                return Optional.ofNullable(azp_name).map(Objects::toString);
            }
        }
        return Optional.empty();
    }

}