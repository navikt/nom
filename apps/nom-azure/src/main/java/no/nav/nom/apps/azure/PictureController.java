package no.nav.nom.apps.azure;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.core.codec.ByteArrayEncoder;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Optional;

@Controller
public class PictureController {


    private final Counter countPictureRequested;

    private final CachePictures cachePictures;


    public PictureController(MeterRegistry meterRegistry, CachePictures cachePictures) {
        this.countPictureRequested = meterRegistry.counter("requests.picture_requested.count");
        this.cachePictures = cachePictures;
    }

    @GetMapping("/hello")
    public ResponseEntity<String> hello() {
        return ResponseEntity.ok("Hello World");
    }

    @GetMapping(value="/picture/{navident}", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> lookupProfilePictureByNavIdent(@PathVariable String navident) {
        countPictureRequested.increment();
        Optional<byte[]> out = this.cachePictures.get(navident);
        return out.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/picture/{navident}/refresh")
    public ResponseEntity<Void> refreshProfilePictureCache(@PathVariable String navident) {
        this.cachePictures.refetch(navident);
        return ResponseEntity.ok().build();
    }
}
