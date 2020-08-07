package no.nav.nom.skjermede.personer.common;

import com.nimbusds.jose.util.IOUtils;
import com.nimbusds.jose.util.Resource;
import lombok.val;
import no.nav.security.token.support.core.configuration.ProxyAwareResourceRetriever;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;

public class CustomFileResourceRetriever extends ProxyAwareResourceRetriever {

    private static final String JWSK_FILE = "/jwkset.json";

    public Resource retrieveResource(final URL url) {
        val content = this.getContentFromFile(url);
        return content != null ? new Resource(content, "application/json") : null;
    }

    private String getContentFromFile(final URL url) {
        try {
            if (url.toString().contains("metadata")) {
                val filename = String.format("/%s.json", url.toString().substring(url.toString().indexOf("metadata")));
                return IOUtils.readInputStreamToString(this.getInputStream(filename), Charset.forName("UTF-8"));
            }
            return url.toString().contains("jwks") ? IOUtils.readInputStreamToString(this.getInputStream(JWSK_FILE), Charset.forName("UTF-8")) : null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private InputStream getInputStream(final String file) {
        return CustomFileResourceRetriever.class.getResourceAsStream(file);
    }
}
