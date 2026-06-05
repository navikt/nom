package no.nav.nom.apps.sfintegrasjon.sfclient.saml;

import no.nav.nom.apps.sfintegrasjon.sfclient.SfProperties;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * Generates signed SAML 2.0 assertions for the SAP SuccessFactors OAuth SAML Bearer Grant flow.
 *
 * <p>Flow overview:
 * <ol>
 *   <li>Build an XML document representing the SAML assertion</li>
 *   <li>Sign it using the RSA private key and Java's built-in XML Digital Signature API (javax.xml.crypto.dsig)</li>
 *   <li>Base64-encode the signed XML — ready to use as the {@code assertion} parameter in the OAuth token request</li>
 * </ol>
 *
 * <p>Reference: <a href="https://www.rfc-editor.org/rfc/rfc7522">RFC 7522 — SAML 2.0 Bearer Grant</a>
 */
@Service
public class SamlAssertionService {

    private static final String SAML2_NS = "urn:oasis:names:tc:SAML:2.0:assertion";

    /**
     * Formats Instant to the ISO 8601 UTC format that SAML requires, e.g. {@code 2026-06-03T14:00:00Z}.
     * The SAML spec mandates UTC ("Z") — never use local timezone offsets here.
     */
    private static final DateTimeFormatter SAML_INSTANT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

    private static final int VALIDITY_NOT_BEFORE_SECONDS = 60;
    private static final int VALIDITY_NOT_ON_OR_AFTER_SECONDS = 600;

    private final SfProperties sfProperties;

    public SamlAssertionService(SfProperties sfProperties) {
        this.sfProperties = sfProperties;
    }

    /**
     * Generates a signed SAML 2.0 assertion.
     *
     * <p>If a field in {@code params} is null, the corresponding value from {@link SfProperties} is used.
     * This lets debug endpoints override individual fields without re-supplying everything.
     *
     * @param params Override parameters (may be {@link SamlAssertionParams#defaults()} for no overrides)
     * @return Base64-encoded signed SAML assertion XML, ready for the OAuth token request
     */
    public String generateAssertion(SamlAssertionParams params) {
        try {
            String clientId = params.clientId() != null ? params.clientId() : sfProperties.clientId();
            String privateKeyPem = params.privateKey() != null ? params.privateKey() : sfProperties.privateKey();
            String apiUrl = params.apiUrl() != null ? params.apiUrl() : sfProperties.apiUrl();
            String userId = params.userId() != null ? params.userId() : sfProperties.userId();

            PrivateKey privateKey = loadPrivateKey(privateKeyPem);

            Instant now = Instant.now();
            // SAML IDs must be NCNames (XML name tokens), which cannot start with a digit.
            // Prefixing with underscore is the standard convention.
            String assertionId = "_" + UUID.randomUUID().toString().replace("-", "");

            Document document = buildAssertionDocument(assertionId, clientId, userId, apiUrl, now);
            signDocument(document, assertionId, privateKey);

            byte[] xmlBytes = serializeDocument(document);
            return Base64.getEncoder().encodeToString(xmlBytes);
        } catch (SamlAssertionException e) {
            throw e;
        } catch (Exception e) {
            throw new SamlAssertionException("Failed to generate SAML assertion", e);
        }
    }

    /**
     * Loads an RSA private key from a PKCS#8 PEM-encoded string (standard PEM format with header/footer). // gitleaks:allow
     */
    private PrivateKey loadPrivateKey(String encodedKey) throws Exception {
        // The env var is PEM-wrapped outer base64:
        //   <PEM-header> // gitleaks:allow
        //   <Base64( base64_key + optional_"###<tenant>" )>
        //   <PEM-footer>

        // Step 1: strip PEM headers and whitespace
        String b64Outer = encodedKey
                .replaceAll("-----[^\\n]*-----", "")
                .replaceAll("\\s+", "");

        // Step 2: outer decode → UTF-8 string: base64_key + optional_"###<tenant>"
        String inner = new String(Base64.getDecoder().decode(b64Outer), StandardCharsets.UTF_8);

        // Step 3: strip "###<tenant>" suffix if present
        String keyB64 = inner.split("###")[0];

        // Step 4: inner decode → DER bytes
        byte[] keyBytes = Base64.getDecoder().decode(keyB64.replaceAll("\\s+", ""));
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }

    /**
     * Builds the SAML 2.0 Assertion XML document.
     *
     * <p>Structure (SAML Bearer Grant per RFC 7522, adapted for SAP SuccessFactors):
     * <pre>{@code
     * <saml2:Assertion ID="_uuid" IssueInstant="now" Version="2.0">
     *   <saml2:Issuer>{clientId}</saml2:Issuer>
     *   <!-- Signature element will be inserted here by signDocument() -->
     *   <saml2:Subject>
     *     <saml2:NameID Format="...unspecified">{userId}</saml2:NameID>
     *     <saml2:SubjectConfirmation Method="...bearer">
     *       <saml2:SubjectConfirmationData NotOnOrAfter="now+10min" Recipient="{apiUrl}/oauth/token"/>
     *     </saml2:SubjectConfirmation>
     *   </saml2:Subject>
     *   <saml2:Conditions NotBefore="now-1min" NotOnOrAfter="now+10min">
     *     <saml2:AudienceRestriction>
     *       <saml2:Audience>www.successfactors.com</saml2:Audience>
     *     </saml2:AudienceRestriction>
     *   </saml2:Conditions>
     *   <saml2:AuthnStatement AuthnInstant="now">
     *     <saml2:AuthnContext>
     *       <saml2:AuthnContextClassRef>...PasswordProtectedTransport</saml2:AuthnContextClassRef>
     *     </saml2:AuthnContext>
     *   </saml2:AuthnStatement>
     * </saml2:Assertion>
     * }</pre>
     *
     * <p>Key field notes:
     * <ul>
     *   <li><b>Issuer</b>: the OAuth {@code client_id} / SF API Key — identifies the calling application</li>
     *   <li><b>NameID</b>: the SF user ID who will receive the token — must exist in SuccessFactors</li>
     *   <li><b>Audience</b>: SF expects {@code www.successfactors.com} regardless of which API host you call</li>
     *   <li><b>Recipient</b>: the full token endpoint URL — SF validates this matches the actual request URL</li>
     *   <li><b>NotOnOrAfter</b>: 10 minutes is a safe validity window; too long increases replay attack risk</li>
     * </ul>
     */
    private Document buildAssertionDocument(
            String assertionId,
            String clientId,
            String userId,
            String apiUrl,
            Instant now
    ) throws Exception {
        String notBefore = SAML_INSTANT.format(now.minusSeconds(VALIDITY_NOT_BEFORE_SECONDS));
        String notOnOrAfter = SAML_INSTANT.format(now.plusSeconds(VALIDITY_NOT_ON_OR_AFTER_SECONDS));
        String tokenEndpoint = apiUrl.replaceAll("/$", "") + "/oauth/token";

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        // We are creating XML, not parsing untrusted input, but XXE hardening is good practice
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.newDocument();

        // Root: saml2:Assertion
        Element assertion = doc.createElementNS(SAML2_NS, "saml2:Assertion");
        // Explicit namespace declaration ensures consistent signing and serialization
        assertion.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:saml2", SAML2_NS);
        assertion.setAttribute("ID", assertionId);
        // Required: declare "ID" as an xs:ID attribute so the XML Signature engine can resolve
        // the "#assertionId" reference URI via Document.getElementById(). Without this, signing fails
        // with "Cannot resolve element with ID ..." because the DOM doesn't know which attribute is the key.
        assertion.setIdAttribute("ID", true);
        assertion.setAttribute("IssueInstant", SAML_INSTANT.format(now));
        assertion.setAttribute("Version", "2.0");
        // Required by the xsi:type="xs:string" on AttributeValue
        assertion.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:xs", "http://www.w3.org/2001/XMLSchema");
        assertion.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        doc.appendChild(assertion);

        // saml2:Issuer — identifies the SF service (fixed per SAP spec)
        Element issuer = doc.createElementNS(SAML2_NS, "saml2:Issuer");
        issuer.setTextContent("www.successfactors.com");
        assertion.appendChild(issuer);

        // saml2:Subject — who is authenticating
        Element nameId = doc.createElementNS(SAML2_NS, "saml2:NameID");
        nameId.setAttribute("Format", "urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified");
        nameId.setTextContent(userId);

        Element confirmationData = doc.createElementNS(SAML2_NS, "saml2:SubjectConfirmationData");
        confirmationData.setAttribute("NotOnOrAfter", notOnOrAfter);
        confirmationData.setAttribute("Recipient", tokenEndpoint);

        Element subjectConfirmation = doc.createElementNS(SAML2_NS, "saml2:SubjectConfirmation");
        subjectConfirmation.setAttribute("Method", "urn:oasis:names:tc:SAML:2.0:cm:bearer");
        subjectConfirmation.appendChild(confirmationData);

        Element subject = doc.createElementNS(SAML2_NS, "saml2:Subject");
        subject.appendChild(nameId);
        subject.appendChild(subjectConfirmation);
        assertion.appendChild(subject);

        // saml2:Conditions — validity window and audience restriction
        Element audience = doc.createElementNS(SAML2_NS, "saml2:Audience");
        audience.setTextContent("www.successfactors.com");

        Element audienceRestriction = doc.createElementNS(SAML2_NS, "saml2:AudienceRestriction");
        audienceRestriction.appendChild(audience);

        Element conditions = doc.createElementNS(SAML2_NS, "saml2:Conditions");
        conditions.setAttribute("NotBefore", notBefore);
        conditions.setAttribute("NotOnOrAfter", notOnOrAfter);
        conditions.appendChild(audienceRestriction);
        assertion.appendChild(conditions);

        // saml2:AuthnStatement — declares how the subject was authenticated
        Element authnContextClassRef = doc.createElementNS(SAML2_NS, "saml2:AuthnContextClassRef");
        authnContextClassRef.setTextContent("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport");

        Element authnContext = doc.createElementNS(SAML2_NS, "saml2:AuthnContext");
        authnContext.appendChild(authnContextClassRef);

        Element authnStatement = doc.createElementNS(SAML2_NS, "saml2:AuthnStatement");
        authnStatement.setAttribute("AuthnInstant", SAML_INSTANT.format(now));
        authnStatement.setAttribute("SessionIndex", UUID.randomUUID().toString());
        authnStatement.appendChild(authnContext);
        assertion.appendChild(authnStatement);

        // saml2:AttributeStatement — carries the OAuth api_key (clientId) required by SF
        Element attributeValue = doc.createElementNS(SAML2_NS, "saml2:AttributeValue");
        attributeValue.setAttributeNS("http://www.w3.org/2001/XMLSchema-instance", "xsi:type", "xs:string");
        attributeValue.setTextContent(clientId);

        Element apiKeyAttribute = doc.createElementNS(SAML2_NS, "saml2:Attribute");
        apiKeyAttribute.setAttribute("Name", "api_key");
        apiKeyAttribute.appendChild(attributeValue);

        Element attributeStatement = doc.createElementNS(SAML2_NS, "saml2:AttributeStatement");
        attributeStatement.appendChild(apiKeyAttribute);
        assertion.appendChild(attributeStatement);

        return doc;
    }

    /**
     * Signs the SAML assertion using RSA-SHA256 with an XML Enveloped Signature.
     *
     * <p>Signature placement: The {@code <ds:Signature>} element is inserted after {@code <saml2:Issuer>}
     * and before {@code <saml2:Subject>}. This is required by the SAML 2.0 XML Schema — the Signature
     * must come after Issuer.
     *
     * <p>Algorithms used:
     * <ul>
     *   <li><b>Canonicalization</b>: Exclusive C14N ({@code http://www.w3.org/2001/10/xml-exc-c14n#}) —
     *       normalizes XML before digesting/signing, ensuring whitespace and namespace prefix differences
     *       don't break signature verification</li>
     *   <li><b>Signature</b>: RSA-SHA256 ({@code http://www.w3.org/2001/04/xmldsig-more#rsa-sha256})</li>
     *   <li><b>Digest</b>: SHA-256</li>
     *   <li><b>Transform</b>: Enveloped signature — removes the {@code <ds:Signature>} element itself
     *       from the data that gets digested, solving the circular dependency of signing a document
     *       that contains the signature</li>
     * </ul>
     *
     * <p>The Reference URI {@code "#assertionId"} means "sign the element with this ID attribute",
     * which is the root {@code <saml2:Assertion>} element. SF validates that this matches.
     */
    private void signDocument(Document document, String assertionId, PrivateKey privateKey) throws Exception {
        XMLSignatureFactory sigFactory = XMLSignatureFactory.getInstance("DOM");

        Reference reference = sigFactory.newReference(
                "#" + assertionId,
                sigFactory.newDigestMethod(DigestMethod.SHA256, null),
                List.of(
                        sigFactory.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null),
                        // Required by SAML 2.0 standard: canonicalize the signed content so the
                        // digest is reproducible regardless of whitespace or namespace prefix differences.
                        // OpenSAML (SAP reference implementation) always includes this transform.
                        sigFactory.newTransform(CanonicalizationMethod.EXCLUSIVE, (TransformParameterSpec) null)
                ),
                null,
                null
        );

        SignedInfo signedInfo = sigFactory.newSignedInfo(
                sigFactory.newCanonicalizationMethod(
                        CanonicalizationMethod.EXCLUSIVE,
                        (C14NMethodParameterSpec) null
                ),
                sigFactory.newSignatureMethod(
                        "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256",
                        null
                ),
                List.of(reference)
        );

        XMLSignature xmlSignature = sigFactory.newXMLSignature(signedInfo, null);

        // Insert signature after Issuer (SAML 2.0 schema order: Issuer, Signature, Subject, ...)
        Element assertionElement = document.getDocumentElement();
        Element issuerElement = (Element) assertionElement.getElementsByTagNameNS(SAML2_NS, "Issuer").item(0);
        DOMSignContext signContext = new DOMSignContext(
                privateKey,
                assertionElement,
                issuerElement.getNextSibling()
        );

        xmlSignature.sign(signContext);
    }

    private byte[] serializeDocument(Document document) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(document), new StreamResult(baos));
        return baos.toByteArray();
    }
}
