package no.nav.nom.apps.nom.enheter;

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.net.ssl.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

@EnableScheduling
@EnableJwtTokenValidation
@SpringBootApplication
public class NomEnheter {
    public static void main(String[] args) {
        SpringApplication.run(NomEnheter.class);
    }
}

