package no.nav.nom.apps.sfintegrasjon.sfclient.saml;

import no.nav.nom.apps.sfintegrasjon.sfclient.SfProperties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for SamlAssertionService.
 *
 * Uses a freshly generated in-memory RSA key pair — no real credentials needed.
 *
 * Before modifying this service, ensure you understand:
 * - Why the Signature element must come after Issuer (SAML 2.0 schema ordering)
 * - Why Exclusive C14N is required (namespace normalization before signing)
 * - How the Enveloped transform prevents circular signing
 */
class SamlAssertionServiceTest {

    private static SamlAssertionService service;
    private static PrivateKey testPrivateKey;
    private static String testPrivateKeyPem;
    private static PublicKey testPublicKey;

    @BeforeAll
    static void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();

        testPublicKey = keyPair.getPublic();
        testPrivateKey = keyPair.getPrivate();
        testPrivateKeyPem = encodePrivateKey(testPrivateKey);

        SfProperties props = new SfProperties(
                "https://api55.sapsf.eu/",
                "test-client-id",
                testPrivateKeyPem,
                "test-tenant",
                "test-user-id",
                "dummy-cpi-url"
        );
        service = new SamlAssertionService(props);
    }

    @Test
    void generateAssertion_returnsBase64EncodedXml() {
        String assertion = service.generateAssertion(SamlAssertionParams.defaults());

        assertThat(assertion).isNotBlank();
        // Must be valid base64
        byte[] decoded = Base64.getDecoder().decode(assertion);
        assertThat(new String(decoded, StandardCharsets.UTF_8)).contains("<saml2:Assertion");
    }

    @Test
    void generateAssertion_xmlContainsRequiredSamlElements() throws Exception {
        String assertion = service.generateAssertion(SamlAssertionParams.defaults());
        Document doc = parseXml(Base64.getDecoder().decode(assertion));

        String saml2Ns = "urn:oasis:names:tc:SAML:2.0:assertion";

        assertThat(doc.getDocumentElement().getLocalName()).isEqualTo("Assertion");
        assertThat(elementText(doc, saml2Ns, "Issuer")).isEqualTo("www.successfactors.com");
        assertThat(elementText(doc, saml2Ns, "NameID")).isEqualTo("test-user-id");
        assertThat(elementText(doc, saml2Ns, "Audience")).isEqualTo("www.successfactors.com");
    }

    @Test
    void generateAssertion_recipientPointsToTokenEndpoint() throws Exception {
        String assertion = service.generateAssertion(SamlAssertionParams.defaults());
        Document doc = parseXml(Base64.getDecoder().decode(assertion));

        Element confirmationData = (Element) doc
                .getElementsByTagNameNS("urn:oasis:names:tc:SAML:2.0:assertion", "SubjectConfirmationData")
                .item(0);

        assertThat(confirmationData).isNotNull();
        assertThat(confirmationData.getAttribute("Recipient"))
                .isEqualTo("https://api55.sapsf.eu/oauth/token");
    }

    @Test
    void generateAssertion_containsXmlSignatureElement() throws Exception {
        String assertion = service.generateAssertion(SamlAssertionParams.defaults());
        Document doc = parseXml(Base64.getDecoder().decode(assertion));

        NodeList signatures = doc.getElementsByTagNameNS("http://www.w3.org/2000/09/xmldsig#", "Signature");
        assertThat(signatures.getLength()).isEqualTo(1);
    }

    @Test
    void generateAssertion_overrideParamsAreUsed() throws Exception {
        SamlAssertionParams overrides = new SamlAssertionParams(
                "override-client-id", null, null, "override-user-id", null
        );

        String assertion = service.generateAssertion(overrides);
        Document doc = parseXml(Base64.getDecoder().decode(assertion));
        String saml2Ns = "urn:oasis:names:tc:SAML:2.0:assertion";

        assertThat(elementText(doc, saml2Ns, "Issuer")).isEqualTo("www.successfactors.com");
        assertThat(elementText(doc, saml2Ns, "NameID")).isEqualTo("override-user-id");

        // clientId now maps to the api_key attribute, not the Issuer
        NodeList attributes = doc.getElementsByTagNameNS(saml2Ns, "Attribute");
        assertThat(attributes.getLength()).isGreaterThan(0);
        assertThat(((Element) attributes.item(0)).getAttribute("Name")).isEqualTo("api_key");
        assertThat(attributes.item(0).getTextContent().trim()).isEqualTo("override-client-id");
    }

    @Test
    void generateAssertion_containsApiKeyAttributeWithClientId() throws Exception {
        String assertion = service.generateAssertion(SamlAssertionParams.defaults());
        Document doc = parseXml(Base64.getDecoder().decode(assertion));
        String saml2Ns = "urn:oasis:names:tc:SAML:2.0:assertion";

        NodeList attributes = doc.getElementsByTagNameNS(saml2Ns, "Attribute");
        assertThat(attributes.getLength()).isGreaterThan(0);
        Element apiKeyAttr = (Element) attributes.item(0);
        assertThat(apiKeyAttr.getAttribute("Name")).isEqualTo("api_key");
        assertThat(apiKeyAttr.getTextContent().trim()).isEqualTo("test-client-id");
    }

    @Test
    void generateAssertion_containsSessionIndex() throws Exception {
        String assertion = service.generateAssertion(SamlAssertionParams.defaults());
        Document doc = parseXml(Base64.getDecoder().decode(assertion));

        Element authnStatement = (Element) doc
                .getElementsByTagNameNS("urn:oasis:names:tc:SAML:2.0:assertion", "AuthnStatement")
                .item(0);

        assertThat(authnStatement).isNotNull();
        assertThat(authnStatement.getAttribute("SessionIndex")).isNotBlank();
    }

    @Test
    void generateAssertion_stripsHashTenantSuffixFromPrivateKey() {
        // Format with tenant: PEM( Base64( Base64(DER) + "###NAVTest" ) )
        String innerWithTenant = Base64.getEncoder().encodeToString(testPrivateKey.getEncoded()) + "###NAVTest";
        String outerB64 = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(
                innerWithTenant.getBytes(StandardCharsets.UTF_8));
        String pem = "-----BEGIN ENCRYPTED PRIVATE KEY-----\n" + outerB64 + "\n-----END ENCRYPTED PRIVATE KEY-----"; // gitleaks:allow

        String assertion = service.generateAssertion(new SamlAssertionParams(null, pem, null, null, null));

        assertThat(assertion).isNotBlank();
    }

    @Test
    void generateAssertion_invalidPrivateKeyThrowsSamlAssertionException() {
        SamlAssertionParams params = new SamlAssertionParams(
                null, "not-a-valid-pem", null, null, null
        );

        assertThatThrownBy(() -> service.generateAssertion(params))
                .isInstanceOf(SamlAssertionException.class)
                .hasMessageContaining("Failed to generate SAML assertion");
    }

    private static String encodePrivateKey(PrivateKey key) {
        // Matches actual SAP SF secret format:
        // -----BEGIN ENCRYPTED PRIVATE KEY-----
        // <Base64( Base64(DER_bytes) )>
        // -----END ENCRYPTED PRIVATE KEY-----
        String innerB64 = Base64.getEncoder().encodeToString(key.getEncoded());
        String outerB64 = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(
                innerB64.getBytes(StandardCharsets.UTF_8));
        return "-----BEGIN ENCRYPTED PRIVATE KEY-----\n" + outerB64 + "\n-----END ENCRYPTED PRIVATE KEY-----";
    }

    private Document parseXml(byte[] xml) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        return dbf.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
    }

    private String elementText(Document doc, String ns, String localName) {
        NodeList nodes = doc.getElementsByTagNameNS(ns, localName);
        assertThat(nodes.getLength()).as("Expected element <%s>", localName).isGreaterThan(0);
        return nodes.item(0).getTextContent();
    }
}
