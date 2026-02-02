package no.nav.nom.apps.api.ressurs.api.v1.dvh;

import no.nav.nom.apps.api.TestTokensGenerator;
import no.nav.nom.apps.api.rest.RestTestBase;
import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import no.nav.nom.libs.state.ressurs.mastered.MasteredRessursStateRepository;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ActiveProfiles("DvhRessursControllerTest-FAKES")
public class DvhRessursControllerTestResponse extends RestTestBase {

    @Autowired
    OrgtilknytningStateRepository orgtilknytningStateRepository;

    @Autowired
    MasteredRessursStateRepository masteredRessursStateRepository;

    @Autowired
    TestTokensGenerator testToken;

    @Test
    @DisplayName("Test Rest endepunktet til DvhRessursController.nomMasteredRessurser()")
    public void test_001() {
        webTestClient.get()
            .uri("/api/v1/dvh/nom-mastered-ressurser")
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(testToken.queryPersonClient());
            })
            .exchange()
            .expectStatus().is2xxSuccessful()
            .expectBodyList(DvhRessursDTO.class);

        var expected ="[{\"navident\":\"A000001\",\"personident\":\"01019000001\",\"sektor\":\"NAV_STATLIG\",\"identType\":\"STANDARD\",\"fornavn\":\"Fornavn_1\",\"mellomnavn\":null,\"etternavn\":\"Etternavn_1\",\"startdato\":\"2007-12-01\",\"sluttdato\":\"2012-11-30\",\"orgtilknytninger\":[{\"orgenhetId\":\"ENHET_1\",\"startdato\":\"2007-12-01\",\"sluttdato\":null,\"sektor\":null,\"erDagligOppfolging\":true,\"orgtilknytningType\":null,\"orgtilknytningFid\":\"OT_FID_1\"}],\"tilknytningsType\":null,\"primaryFysiskBrukersted\":null,\"jobbtelefon\":null,\"nomRegistrertTelefonnummer\":null},{\"navident\":\"A000002\",\"personident\":\"01019000002\",\"sektor\":\"NAV_KOMMUNAL\",\"identType\":\"STANDARD\",\"fornavn\":\"Fornavn_2\",\"mellomnavn\":null,\"etternavn\":\"Etternavn_2\",\"startdato\":\"2008-12-01\",\"sluttdato\":null,\"orgtilknytninger\":[{\"orgenhetId\":\"ENHET_2\",\"startdato\":\"2012-12-01\",\"sluttdato\":null,\"sektor\":null,\"erDagligOppfolging\":true,\"orgtilknytningType\":null,\"orgtilknytningFid\":\"OT_FID_3\"},{\"orgenhetId\":\"ENHET_1\",\"startdato\":\"2008-12-01\",\"sluttdato\":\"2012-11-30\",\"sektor\":null,\"erDagligOppfolging\":true,\"orgtilknytningType\":null,\"orgtilknytningFid\":\"OT_FID_2\"}],\"tilknytningsType\":null,\"primaryFysiskBrukersted\":null,\"jobbtelefon\":null,\"nomRegistrertTelefonnummer\":null},{\"navident\":\"A000003\",\"personident\":\"01019000003\",\"sektor\":\"EKSTERN\",\"identType\":\"UTENLANDSK_KONSULENT\",\"fornavn\":\"Fornavn_3\",\"mellomnavn\":\"Mellomnavn_3\",\"etternavn\":\"Etternavn_3\",\"startdato\":\"2009-12-01\",\"sluttdato\":null,\"orgtilknytninger\":[{\"orgenhetId\":\"ENHET_2\",\"startdato\":\"2012-12-01\",\"sluttdato\":\"2018-12-31\",\"sektor\":null,\"erDagligOppfolging\":false,\"orgtilknytningType\":null,\"orgtilknytningFid\":\"OT_FID5\"},{\"orgenhetId\":\"ENHET_1\",\"startdato\":\"2009-12-01\",\"sluttdato\":null,\"sektor\":null,\"erDagligOppfolging\":true,\"orgtilknytningType\":null,\"orgtilknytningFid\":\"OT_FID_4\"}],\"tilknytningsType\":null,\"primaryFysiskBrukersted\":null,\"jobbtelefon\":null,\"nomRegistrertTelefonnummer\":null}]";

        var result = webTestClient.get()
            .uri("/api/v1/dvh/nom-mastered-ressurser")
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(testToken.queryPersonClient());
            })
            .exchange()
            .returnResult(String.class).getResponseBody().blockFirst();

        assertEquals(expected, result);
    }

    @BeforeEach
    public void setup() {
        orgtilknytningStateRepository.update(
            "OT_FID_1",
            OrgtilknytningState.builder()
                .fid("OT_FID_1")
                .navident("A000001")
                .orgEnhetId("ENHET_1")
                .gyldigFom(LocalDate.of(2007, 12, 1))
                .gyldigTom(null)
                .erDagligOppfolging(true)
                .build());

        orgtilknytningStateRepository.update(
            "OT_FID_2",
            OrgtilknytningState.builder()
                .fid("OT_FID_2")
                .navident("A000002")
                .orgEnhetId("ENHET_1")
                .gyldigFom(LocalDate.of(2008, 12, 1))
                .gyldigTom(LocalDate.of(2012, 11, 30))
                .erDagligOppfolging(true)
                .build());

        orgtilknytningStateRepository.update(
            "OT_FID_3",
            OrgtilknytningState.builder()
                .fid("OT_FID_3")
                .navident("A000002")
                .orgEnhetId("ENHET_2")
                .gyldigFom(LocalDate.of(2012, 12, 1))
                .gyldigTom(null)
                .erDagligOppfolging(true)
                .build());

        orgtilknytningStateRepository.update(
            "OT_FID_4",
            OrgtilknytningState.builder()
                .fid("OT_FID_4")
                .navident("A000003")
                .orgEnhetId("ENHET_1")
                .gyldigFom(LocalDate.of(2009, 12, 1))
                .gyldigTom(null)
                .erDagligOppfolging(true)
                .build());

        orgtilknytningStateRepository.update(
            "OT_FID5",
            OrgtilknytningState.builder()
                .fid("OT_FID5")
                .navident("A000003")
                .orgEnhetId("ENHET_2")
                .gyldigFom(LocalDate.of(2012, 12, 1))
                .gyldigTom(LocalDate.of(2018, 12, 31))
                .erDagligOppfolging(false)
                .build());

        orgtilknytningStateRepository.update(
            "OT_FID6",
            OrgtilknytningState.builder()
                .fid("OT_FID6")
                .navident("A000004")
                .orgEnhetId("ENHET_2")
                .gyldigFom(LocalDate.of(2010, 12, 1))
                .gyldigTom(null)
                .erDagligOppfolging(true)
                .build());

        masteredRessursStateRepository.update("A000001", LocalDateTime.now());
        masteredRessursStateRepository.update("A000002", LocalDateTime.now());
        masteredRessursStateRepository.update("A000003", LocalDateTime.now());
    }
}

@Profile("DvhRessursControllerTest-FAKES")
@Primary
@Service
class RessursStateRepositoryFake implements RessursStateRepository {
    private final List<RessursState> alleRessurser = List.of(
        RessursState.builder()
            .fid("FID_1")
            .personident("01019000001")
            .navident("A000001")
            .sektor(Sektor.NAV_STATLIG)
            .identType(IdentType.STANDARD)
            .visningsnavn(new Visningsnavn("Fornavn_1", Optional.empty(), "Etternavn_1"))
            .startdato(LocalDate.of(2007, 12, 1))
            .sluttdato(LocalDate.of(2012, 11, 30))
            .build(),
        RessursState.builder()
            .fid("FID_2")
            .personident("01019000002")
            .navident("A000002")
            .sektor(Sektor.NAV_KOMMUNAL)
            .identType(IdentType.STANDARD)
            .visningsnavn(new Visningsnavn("Fornavn_2", Optional.empty(), "Etternavn_2"))
            .startdato(LocalDate.of(2008, 12, 1))
            .sluttdato(null)
            .build(),
        RessursState.builder()
            .fid("FID_3")
            .personident("01019000003")
            .navident("A000003")
            .sektor(Sektor.EKSTERN)
            .identType(IdentType.UTENLANDSK_KONSULENT)
            .visningsnavn(new Visningsnavn("Fornavn_3", Optional.of("Mellomnavn_3"), "Etternavn_3"))
            .startdato(LocalDate.of(2009, 12, 1))
            .sluttdato(null)
            .build(),
        RessursState.builder()
            .fid("FID_4")
            .personident("01019000004")
            .navident("A000004")
            .sektor(Sektor.NAV_STATLIG)
            .identType(IdentType.STANDARD)
            .visningsnavn(new Visningsnavn("Fornavn_4", Optional.empty(), "Etternavn_4"))
            .startdato(LocalDate.of(2010, 12, 1))
            .sluttdato(null)
            .build()
    );

    private final Map<String, RessursState> ressursByFid = alleRessurser.stream().collect(Collectors.toMap(RessursState::fid, Function.identity()));
    private final Map<String, RessursState> ressursByPersonident = alleRessurser.stream().collect(Collectors.toMap(RessursState::personident, Function.identity()));
    private final Map<String, RessursState> ressursByNavident = alleRessurser.stream().collect(Collectors.toMap(RessursState::navident, Function.identity()));
    private final Map<String, RessursState> ressursByNavidentBareEksterne = alleRessurser.stream().filter(ressursState -> ressursState.sektor().equals(Sektor.EKSTERN)).collect(Collectors.toMap(RessursState::navident, Function.identity()));

    @Override
    public Optional<RessursState> findByFid(String fid) {
        return Optional.ofNullable(ressursByFid.get(fid));
    }

    @Override
    public Optional<RessursState> findByPersonident(String personident) {
        return Optional.ofNullable(ressursByPersonident.get(personident));
    }

    @Override
    public Optional<RessursState> findByNavident(String navident) {
        return Optional.ofNullable(ressursByNavident.get(navident));
    }

    @Override
    public <T> Optional<T> findByNavident(String navIdent, Function<RessursState, T> withMapper) {
        return findByNavident(navIdent)
            .map(withMapper);
    }

    @Override
    public Collection<RessursState> getAll() {
        return ressursByNavident.values();
    }

    @Override
    public Collection<RessursState> getAllBareEksterne() {
        return ressursByNavidentBareEksterne.values();
    }

    @Override
    public Collection<RessursState> getAllByFid() {
        return ressursByFid.values();
    }

    @Override
    public long count() {
        return ressursByFid.size();
    }

    @Override
    public long countEksterne() {
        return 0;
    }

    @Override
    public long countKommunale() {
        return 0;
    }

    @Override
    public long countStatlige() {
        return 0;
    }

    @Override
    public long countIkkeAvsluttede() {
        return 0;
    }

    @Override
    public long countIkkeAvsluttedeEksterne() {
        return 0;
    }

    @Override
    public long countIkkeAvsluttedeKommunale() {
        return 0;
    }

    @Override
    public long countIkkeAvsluttedeStatlige() {
        return 0;
    }

    @Override
    public long countFremtidige() {
        return 0;
    }

    @Override
    public long countFremtidigeEksterne() {
        return 0;
    }

    @Override
    public long countFremtidigeKommunale() {
        return 0;
    }

    @Override
    public long countFremtidigeStatlige() {
        return 0;
    }

    @Override
    public Map<String, RessursState> findByPersonidentBulk(List<String> personIdenter) {
        return ressursByPersonident.entrySet().stream()
            .filter(entry -> personIdenter.contains(entry.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public Map<String, RessursState> findByNavidentBulk(List<String> navIdenter) {
        return ressursByNavident.entrySet().stream()
            .filter(entry -> navIdenter.contains(entry.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public Map<String, RessursState> findAllWithSluttdatoBeforeToday() {
        return ressursByNavident.values().stream()
            .filter(ressurs -> ressurs.sluttdato() != null)
            .filter(ressurs -> ressurs.sluttdato().isBefore(LocalDate.now()))
            .collect(Collectors.toMap(RessursState::navident, ressurs -> ressurs));
    }

    @Override
    public Map<String, RessursState> findAllActiveRessurser() {
        return ressursByNavident.values().stream()
            .filter(ressurs -> isNull(ressurs.sluttdato()) || !ressurs.sluttdato().isBefore(LocalDate.now()))
            .collect(Collectors.toMap(RessursState::navident, ressurs -> ressurs));
    }


    @Override
    public void update(RessursState ressursEventData) {throw new NotImplementedException();   }

    @Override
    public void clear() {
        // Don't clear in Fake
    }

    @Override
    public void remove(String fid) { throw new NotImplementedException();   }
}
