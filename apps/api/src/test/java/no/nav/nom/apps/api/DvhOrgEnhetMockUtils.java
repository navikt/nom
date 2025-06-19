package no.nav.nom.apps.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.contract.nom.OrgEnhetsType;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.apache.commons.lang3.StringUtils.isEmpty;

@Service
@RequiredArgsConstructor
public class DvhOrgEnhetMockUtils {

    private final List<DvhOrgEnhetMock> response = new LinkedList<>();
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    private void mockDvhOrgEnhetResponse(String response) {
        stubFor(get("/dvh_nom/nom_ords_hrorg_orgenhet.json").willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(response)));
    }

    @SneakyThrows
    public void addToResponse(DvhOrgEnhetMock... enheter) {
        response.addAll(List.of(enheter));
        createResponse(response);
    }

    public void createEmptyResponse() {
        response.clear();
        createResponse(response);
    }

    public void recreateResponse() {
        createResponse(response);
    }

    @SneakyThrows
    private void createResponse(List<DvhOrgEnhetMock> orgenhetMocks) {
        val response = objectMapper.createObjectNode();
        response.set("items", objectMapper.convertValue(orgenhetMocks, JsonNode.class));
        mockDvhOrgEnhetResponse(objectMapper.writeValueAsString(response));
    }

    public OrgEnhetState dvhMockToState(DvhOrgEnhetMock dvhMock, int nr, String from, String to) {
        LocalDate fromDate = LocalDate.of(Integer.parseInt(from.substring(0, 4)), Integer.parseInt(from.substring(4, 6)), 1);
        Optional<LocalDate> toDate = Optional.empty();
        if (!isEmpty(to)) {
            toDate = Optional.of(LocalDate.of(Integer.parseInt(to.substring(0, 4)), Integer.parseInt(to.substring(4, 6)), 1)
                .plusMonths(1)
                .minusDays(1));
        }
        String agressoId = dvhMock.getORGENHET_KODE();
        String agressoNavn = dvhMock.getORGENHET_NAVN();
        String agressoNiv = dvhMock.getORGNODE_NIV();
        String agressoType = dvhMock.getENHETTYPE_NAVN();
        String uniqueAgressoId = agressoId + "-" + agressoNavn + "-" + agressoNiv;
        OrgEnhetsType orgenhetstype = dvhMock.getORGENHET_TYPE();
        return new OrgEnhetState(
            "FID_" + nr,
            "ID_" + nr,
            agressoNavn,
            fromDate,
            toDate,
            Optional.empty(),
            Optional.empty(),
            Optional.ofNullable(orgenhetstype),
            Optional.empty(),
            Optional.of(uniqueAgressoId),
            Optional.of(agressoId),
            Optional.of(agressoNiv),
            Optional.of(agressoType)
        );
    }
}
