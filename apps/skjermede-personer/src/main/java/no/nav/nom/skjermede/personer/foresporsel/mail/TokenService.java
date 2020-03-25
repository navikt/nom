package no.nav.nom.skjermede.personer.foresporsel.mail;

import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.aad.msal4j.IPublicClientApplication;
import com.microsoft.aad.msal4j.PublicClientApplication;
import com.microsoft.aad.msal4j.UserNamePasswordParameters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private  final AADProperties aadProperties;

    private String accessToken;
    private LocalDateTime expiresOn;

    public synchronized String getAccessToken() throws Exception {
        if (accessToken == null || expiresOn.isAfter(LocalDateTime.now().minusMinutes(5))) {
            refreshToken();
        }

        return accessToken;
    }

    private void refreshToken() throws Exception {
        final IPublicClientApplication pca = PublicClientApplication
                .builder(aadProperties.getClientId())
                .authority(aadProperties.getAuthority())
                .build();
        final UserNamePasswordParameters credentials = UserNamePasswordParameters.builder(
                Collections.singleton("User.Read"),
                aadProperties.getUsername(),
                aadProperties.getPassword().toCharArray()).build();
        final IAuthenticationResult authResult = pca.acquireToken(credentials).get();

        accessToken = authResult.accessToken();
        expiresOn = authResult.expiresOnDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private String get(final String path) throws Exception {
        try {
            return Files.readString(Paths.get(path));
        } catch (IOException e) {
            log.error("Uthenting av brukernavn/passord for aad servicebruker mislyktes");
            throw e;
        }
    }
}
