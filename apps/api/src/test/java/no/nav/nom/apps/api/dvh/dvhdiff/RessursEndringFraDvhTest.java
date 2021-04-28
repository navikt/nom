package no.nav.nom.apps.api.dvh.dvhdiff;

import lombok.val;
import no.nav.nom.apps.api.domain.Ressurs;
import no.nav.nom.apps.api.dvh.DvhTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("dvh-diffing")
public class RessursEndringFraDvhTest extends DvhTestBase {

    @Test
    @DisplayName("Ressurser fra datavarehus blir tilgjengelige i ressurs repo")
    public void test1() {
        simulateSyncedWithKafka();
        stubRessurser("responses/dvhdiff/dvhAnsatt1_Pre.json");
        loadFromDvhAndUpdateRepo();

        val alleRessurserAntall = this.ressursRepository.getAlleRessurser().size();
        assertThat(alleRessurserAntall).isEqualTo(4);
    }

    @Test
    @DisplayName("1 ny ressurs og 1 endring av personnummer gir riktig antall på hendelser og ressurser")
    public void test2() {
        simulateSyncedWithKafka();

        stubRessurser("responses/dvhdiff/dvhAnsatt1_Pre.json");
        loadFromDvhAndUpdateRepo();

        stubRessurser("responses/dvhdiff/dvhAnsatt1_After1.json");
        loadFromDvhAndUpdateRepo();

        val alleRessurser = this.ressursRepository.getAlleRessurser();
        val alleHendelser = getRecordsFromKafka();
        assertThat(alleRessurser.size()).isEqualTo(5);
        assertThat(alleHendelser.size()).isEqualTo(6);
    }

    @Test
    @DisplayName("endring av personnummer i dvh snapshot blir oppdaget")
    public void test3() {
        simulateSyncedWithKafka();

        stubRessurser("responses/dvhdiff/dvhAnsatt1_Pre.json");
        loadFromDvhAndUpdateRepo();
        val fnrPre = ressursRepository.findByNavIdent("O123459").get().getPersonIdent();

        stubRessurser("responses/dvhdiff/dvhAnsatt1_After2.json");
        loadFromDvhAndUpdateRepo();
        val fnrAfter = ressursRepository.findByNavIdent("O123459").get().getPersonIdent();

        assertThat(fnrPre).isNotEqualTo(fnrAfter);
        assertThat(fnrAfter).isEqualTo("92345678904");
    }

    @Test
    @DisplayName("ny navident på eksisterende personnummer har ingen effekt")
    public void test4() {
        simulateSyncedWithKafka();

        stubRessurser("responses/dvhdiff/dvhAnsatt2_SinglePre.json");
        loadFromDvhAndUpdateRepo();
        val hashedRessursDataPre = hashedRessurser(ressursRepository.getAlleRessurser());

        stubRessurser("responses/dvhdiff/dvhAnsatt2_SingleEndretNavIdent.json");
        loadFromDvhAndUpdateRepo();
        val hashedRessursDataPost = hashedRessurser(ressursRepository.getAlleRessurser());

        val antallRessurserIRepo = ressursRepository.getAlleRessurser().size();

        assertThat(antallRessurserIRepo).isEqualTo(1);
        assertThat(hashedRessursDataPre).hasSameElementsAs(hashedRessursDataPost);
    }

    @Test
    @DisplayName("navident/rad fjernet fra dvh har ingen effekt")
    public void test5() {
        simulateSyncedWithKafka();

        stubRessurser("responses/dvhdiff/dvhAnsatt1_Pre.json");
        loadFromDvhAndUpdateRepo();
        val hashedRessursDataPre = hashedRessurser(ressursRepository.getAlleRessurser());

        stubRessurser("responses/dvhdiff/dvhAnsatt1_After3.json");
        loadFromDvhAndUpdateRepo();
        val hashedRessursDataPost = hashedRessurser(ressursRepository.getAlleRessurser());

        val antallRessurserIRepo = ressursRepository.getAlleRessurser().size();

        assertThat(antallRessurserIRepo).isEqualTo(4);
        assertThat(hashedRessursDataPre).hasSameElementsAs(hashedRessursDataPost);
    }

    // test helpers

    private void stubRessurser(String fileName) {
        stubFor(get("/proxy/ressurser").willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json;charset=UTF-8")
                .withBodyFile(fileName)
        ));
    }

    private int hashRessurs(Ressurs r) {
        return r.getId().hashCode() ^ r.getNavIdent().hashCode() ^ r.getPersonIdent().hashCode();
    }

    private Collection<Integer> hashedRessurser(Collection<Ressurs> ressurser) {
        return ressurser
                .stream().map(this::hashRessurs).collect(Collectors.toCollection(ArrayList::new));
    }


}
