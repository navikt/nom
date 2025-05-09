package no.nav.nom.apps.azure;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class DemoController {

    private final AzureAdService azureAdService;

    public DemoController(AzureAdService azureAdService) {
        this.azureAdService = azureAdService;
    }

    @GetMapping("/hello")
    public ResponseEntity<String> hello() {
        return ResponseEntity.ok("Hello World");
    }

    @GetMapping("/picture/{navident}")
    public ResponseEntity<byte[]> lookupProfilePictureByNavIdent(@PathVariable String navident) {
        var out = this.azureAdService.lookupProfilePictureByNavIdent(navident);
        return out.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

}
