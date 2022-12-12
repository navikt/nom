package no.nav.nom.apps.nom.unit4;

import com.nimbusds.jose.jwk.JWK;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.text.ParseException;

@Component
@ConfigurationPropertiesBinding
public class JWKConverter implements Converter<String, JWK> {
    @Override
    public JWK convert(String source) {
        try {
            return JWK.parse(source);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid JWK supplied", e);
        }
    }
}
