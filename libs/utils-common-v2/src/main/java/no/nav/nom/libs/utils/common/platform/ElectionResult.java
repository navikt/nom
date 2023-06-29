package no.nav.nom.libs.utils.common.platform;

import java.time.OffsetDateTime;

public record ElectionResult(
    String name,
    OffsetDateTime last_update
) {
    public boolean isHostLeader(String hostname) {
        return name.equals(hostname);
    }
}
