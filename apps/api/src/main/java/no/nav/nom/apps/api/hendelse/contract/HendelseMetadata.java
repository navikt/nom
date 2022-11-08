package no.nav.nom.apps.api.hendelse.contract;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.LocalDateTime;
import java.util.Objects;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class HendelseMetadata {
    OpprettetAv opprettetAv;
    LocalDateTime opprettetTidspunkt;

    @Value
    @RequiredArgsConstructor
    public static class OpprettetAv {
        String initiatorId;
        String originId;

        public OpprettetAv(JwtAuthenticationToken jwtAuthenticationToken){
            Objects.requireNonNull(jwtAuthenticationToken);
            originId = jwtAuthenticationToken.getTokenAttributes().get("azp_name").toString();
            initiatorId = jwtAuthenticationToken.getToken().getClaims().get("NAVident") != null ?
                    jwtAuthenticationToken.getToken().getClaims().get("NAVident").toString() : originId;
        }
    }
}
