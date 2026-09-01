package no.nav.nom.apps.sfintegrasjon.sfclient;

import com.sap.cloud.sdk.cloudplatform.connectivity.DefaultHttpDestination;
import com.sap.cloud.sdk.datamodel.odata.client.exception.ODataException;
import no.nav.nom.apps.sfintegrasjon.sfclient.oauth.SfAccessToken;
import no.nav.nom.apps.sfintegrasjon.sfclient.oauth.SfTokenCache;
import no.nav.nom.sf.odata.vdm.namespaces.successfactors.EmpEmployment;
import no.nav.nom.sf.odata.vdm.namespaces.successfactors.FOBusinessUnit;
import no.nav.nom.sf.odata.vdm.namespaces.successfactors.FODepartment;
import no.nav.nom.sf.odata.vdm.namespaces.successfactors.FODivision;
import no.nav.nom.sf.odata.vdm.namespaces.successfactors.PerPerson;
import no.nav.nom.sf.odata.vdm.namespaces.successfactors.User;
import no.nav.nom.sf.odata.vdm.services.DefaultSuccessfactorsService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Client for SAP SuccessFactors OData v2 API.
 * Reuses a cached OAuth token; the cache proactively refreshes when fewer than 10 minutes remain before expiry.
 *
 * <p>TODO: Add OData query methods as integration needs grow.
 */
@Component
public class SfODataClient {

    private static final String ODATA2_SERVICE_PATH = "odata/v2";

    private final SfProperties sfProperties;
    private final SfTokenCache sfTokenCache;
    private final RestClient.Builder restClientBuilder;

    public SfODataClient(
        SfProperties sfProperties,
        SfTokenCache sfTokenCache,
        RestClient.Builder restClientBuilder
    ) {
        this.sfProperties = sfProperties;
        this.sfTokenCache = sfTokenCache;
        this.restClientBuilder = restClientBuilder;
    }

    /**
     * Fetches the complete OData $metadata document for all SF entities.
     *
     * @return Raw XML metadata response from SAP SuccessFactors
     */
    public String getAllMetadata() {
        return authenticatedClient()
            .get()
            .uri("$metadata")
            .retrieve()
            .body(String.class);
    }

    /**
     * Fetches the OData $metadata document for the given SF entity.
     *
     * @param entity Entity name (letters only, validated by caller)
     * @return Raw XML metadata response from SAP SuccessFactors
     */
    public String getEntityMetadata(String entity) {
        return authenticatedClient()
            .get()
            .uri(entity + "/$metadata")
            .retrieve()
            .body(String.class);
    }

    public List<User> getFirst10Users(String accessToken) throws ODataException {
        return new DefaultSuccessfactorsService()
            .withServicePath(ODATA2_SERVICE_PATH)
            .getAllUser()
            .top(10)
            .executeRequest(DefaultHttpDestination.builder(sfProperties.apiUrl().replaceAll("/$", ""))
                .header("Authorization", "Bearer " + resolveAccessToken(accessToken))
                .build());
    }

    public List<User> getUsersBySsn(String ssn) throws ODataException {
        return getUsersBySsn(ssn, null);
    }

    public List<User> getUsersBySsn(String ssn, String accessToken) throws ODataException {
        return new DefaultSuccessfactorsService()
            .withServicePath(ODATA2_SERVICE_PATH)
            .getAllUser()
            .filter(User.SSN.eq(ssn))
            .executeRequest(DefaultHttpDestination.builder(sfProperties.apiUrl().replaceAll("/$", ""))
                .header("Authorization", "Bearer " + resolveAccessToken(accessToken))
                .build());
    }

    public List<User> getUsersByEmpId(String empId) throws ODataException {
        return getUsersByEmpId(empId, null);
    }

    public List<User> getUsersByEmpId(String empId, String accessToken) throws ODataException {
        return new DefaultSuccessfactorsService()
            .withServicePath(ODATA2_SERVICE_PATH)
            .getAllUser()
            .filter(User.EMP_ID.eq(empId))
            .executeRequest(DefaultHttpDestination.builder(sfProperties.apiUrl().replaceAll("/$", ""))
                .header("Authorization", "Bearer " + resolveAccessToken(accessToken))
                .build());
    }

    public List<User> getUsersByUserId(String userId) throws ODataException {
        return getUsersByUserId(userId, null);
    }

    public List<User> getUsersByUserId(String userId, String accessToken) throws ODataException {
        return new DefaultSuccessfactorsService()
            .withServicePath(ODATA2_SERVICE_PATH)
            .getAllUser()
            .filter(User.USER_ID.eq(userId))
            .executeRequest(DefaultHttpDestination.builder(sfProperties.apiUrl().replaceAll("/$", ""))
                .header("Authorization", "Bearer " + resolveAccessToken(accessToken))
                .build());
    }

    public List<User> getUsersByUsername(String username) throws ODataException {
        return getUsersByUsername(username, null);
    }

    public List<User> getUsersByUsername(String username, String accessToken) throws ODataException {
        return new DefaultSuccessfactorsService()
            .withServicePath(ODATA2_SERVICE_PATH)
            .getAllUser()
            .filter(User.USERNAME.eq(username))
            .executeRequest(DefaultHttpDestination.builder(sfProperties.apiUrl().replaceAll("/$", ""))
                .header("Authorization", "Bearer " + resolveAccessToken(accessToken))
                .build());
    }

    public List<User> getUsersByEmail(String email) throws ODataException {
        return getUsersByEmail(email, null);
    }

    public List<User> getUsersByEmail(String email, String accessToken) throws ODataException {
        return new DefaultSuccessfactorsService()
            .withServicePath(ODATA2_SERVICE_PATH)
            .getAllUser()
            .filter(User.EMAIL.eq(email))
            .executeRequest(DefaultHttpDestination.builder(sfProperties.apiUrl().replaceAll("/$", ""))
                .header("Authorization", "Bearer " + resolveAccessToken(accessToken))
                .build());
    }

    public List<User> getUsersByDisplayName(String displayName) throws ODataException {
        return getUsersByDisplayName(displayName, null);
    }

    public List<User> getUsersByDisplayName(String displayName, String accessToken) throws ODataException {
        return new DefaultSuccessfactorsService()
            .withServicePath(ODATA2_SERVICE_PATH)
            .getAllUser()
            .filter(User.DISPLAY_NAME.eq(displayName))
            .executeRequest(DefaultHttpDestination.builder(sfProperties.apiUrl().replaceAll("/$", ""))
                .header("Authorization", "Bearer " + resolveAccessToken(accessToken))
                .build());
    }

    public List<EmpEmployment> getFirst10EmpEmployments(String accessToken) throws ODataException {
        return new DefaultSuccessfactorsService()
            .withServicePath(ODATA2_SERVICE_PATH)
            .getAllEmpEmployment()
            .top(10)
            .executeRequest(DefaultHttpDestination.builder(sfProperties.apiUrl().replaceAll("/$", ""))
                .header("Authorization", "Bearer " + resolveAccessToken(accessToken))
                .build());
    }


    public List<FODepartment> getFODepartmentsByNomOrgEnhetId(String nomOrgEnhetId) throws ODataException {
        return getFODepartmentsByNomOrgEnhetId(nomOrgEnhetId, null);
    }

    public List<FODepartment> getFODepartmentsByNomOrgEnhetId(String nomOrgEnhetId, String accessToken) throws ODataException {
        return new DefaultSuccessfactorsService()
            .withServicePath(ODATA2_SERVICE_PATH)
            .getAllFODepartment()
            .filter(FODepartment.EXTERNAL_CODE.eq(nomOrgEnhetId))
            .executeRequest(DefaultHttpDestination.builder(sfProperties.apiUrl().replaceAll("/$", ""))
                .header("Authorization", "Bearer " + resolveAccessToken(accessToken))
                .build());
    }

    public List<FODivision> getFODivisionsByNomOrgEnhetId(String nomOrgEnhetId) throws ODataException {
        return getFODivisionsByNomOrgEnhetId(nomOrgEnhetId, null);
    }

    public List<FODivision> getFODivisionsByNomOrgEnhetId(String nomOrgEnhetId, String accessToken) throws ODataException {
        return new DefaultSuccessfactorsService()
            .withServicePath(ODATA2_SERVICE_PATH)
            .getAllFODivision()
            .filter(FODivision.EXTERNAL_CODE.eq(nomOrgEnhetId))
            .executeRequest(DefaultHttpDestination.builder(sfProperties.apiUrl().replaceAll("/$", ""))
                .header("Authorization", "Bearer " + resolveAccessToken(accessToken))
                .build());
    }

    public List<FOBusinessUnit> getFOBusinessUnitsByNomOrgEnhetId(String nomOrgEnhetId) throws ODataException {
        return getFOBusinessUnitsByNomOrgEnhetId(nomOrgEnhetId, null);
    }

    public List<FOBusinessUnit> getFOBusinessUnitsByNomOrgEnhetId(String nomOrgEnhetId, String accessToken) throws ODataException {
        return new DefaultSuccessfactorsService()
            .withServicePath(ODATA2_SERVICE_PATH)
            .getAllFOBusinessUnit()
            .filter(FOBusinessUnit.EXTERNAL_CODE.eq(nomOrgEnhetId))
            .executeRequest(DefaultHttpDestination.builder(sfProperties.apiUrl().replaceAll("/$", ""))
                .header("Authorization", "Bearer " + resolveAccessToken(accessToken))
                .build());
    }

    public List<PerPerson> getFirst10PerPersons(String accessToken) throws ODataException {
        return new DefaultSuccessfactorsService()
            .withServicePath(ODATA2_SERVICE_PATH)
            .getAllPerPerson()
            .top(10)
            .executeRequest(DefaultHttpDestination.builder(sfProperties.apiUrl().replaceAll("/$", ""))
                .header("Authorization", "Bearer " + resolveAccessToken(accessToken))
                .build());
    }

    private RestClient authenticatedClient() {
        SfAccessToken token = sfTokenCache.getToken();
        return restClientBuilder
            .baseUrl(sfProperties.apiUrl().replaceAll("/$", "") + "/odata/v2/")
            .defaultHeader("Authorization", "Bearer " + token.accessToken())
            .build();
    }

    private String resolveAccessToken(String accessToken) {
        if (StringUtils.hasText(accessToken)) {
            return accessToken.startsWith("Bearer ") ? accessToken.substring("Bearer ".length()) : accessToken;
        }
        return sfTokenCache.getToken().accessToken();
    }
}
