package no.nav.nom.apps.ui.me;

import com.microsoft.aad.msal4j.IConfidentialClientApplication;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MeController {
    private final IConfidentialClientApplication msalClient;

    @GetMapping("/api/v1/me")
    public Object me() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
