package no.nav.nom.apps.api.dvh.dvhdiff;

import lombok.val;
import no.nav.nom.apps.api.dvh.DvhTestBase;
import no.nav.nom.apps.api.ressurs.contract.IRessurs;
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
        stubRessurser("responses/dvhdiff/dvhAnsatt1_Pre.json");
        loadFromDvh();

        val alleRessurserAntall = ressursService.getAll().size();
        assertThat(alleRessurserAntall).isEqualTo(4);
    }

    @Test
    @DisplayName("1 ny ressurs og 1 endring av personnummer gir riktig antall på hendelser og ressurser")
    public void test2() {
        stubRessurser("responses/dvhdiff/dvhAnsatt1_Pre.json");
        loadFromDvh();

        stubRessurser("responses/dvhdiff/dvhAnsatt1_After1.json");
        loadFromDvh();

        val alleRessurser = ressursService.getAll();
        assertThat(alleRessurser.size()).isEqualTo(5);
    }

    @Test
    @DisplayName("endring av personnummer i dvh snapshot blir oppdaget")
    public void test3() {
        stubRessurser("responses/dvhdiff/dvhAnsatt1_Pre.json");
        loadFromDvh();
        val fnrPre = ressursService.findByNavIdent("O123459").get().getPersonIdent();

        stubRessurser("responses/dvhdiff/dvhAnsatt1_After2.json");
        loadFromDvh();
        val fnrAfter = ressursService.findByNavIdent("O123459").get().getPersonIdent();

        assertThat(fnrPre).isNotEqualTo(fnrAfter);
        assertThat(fnrAfter).isEqualTo("92345678904");
    }

    @Test
    @DisplayName("ny navident på eksisterende personnummer har ingen effekt")
    public void test4() {
        stubRessurser("responses/dvhdiff/dvhAnsatt2_SinglePre.json");
        loadFromDvh();
        val hashedRessursDataPre = hashedRessurser(ressursService.getAll());

        stubRessurser("responses/dvhdiff/dvhAnsatt2_SingleEndretNavIdent.json");
        loadFromDvh();
        val hashedRessursDataPost = hashedRessurser(ressursService.getAll());

        val antallRessurserIRepo = ressursService.getAll().size();

        assertThat(antallRessurserIRepo).isEqualTo(1);
        assertThat(hashedRessursDataPre).hasSameElementsAs(hashedRessursDataPost);
    }

    @Test
    @DisplayName("navident/rad fjernet fra dvh har ingen effekt")
    public void test5() {
        stubRessurser("responses/dvhdiff/dvhAnsatt1_Pre.json");
        loadFromDvh();
        val hashedRessursDataPre = hashedRessurser(ressursService.getAll());

        stubRessurser("responses/dvhdiff/dvhAnsatt1_After3.json");
        loadFromDvh();
        val hashedRessursDataPost = hashedRessurser(ressursService.getAll());

        val antallRessurserIRepo = ressursService.getAll().size();;

        assertThat(antallRessurserIRepo).isEqualTo(4);
        assertThat(hashedRessursDataPre).hasSameElementsAs(hashedRessursDataPost);
    }

    @Test
    @DisplayName("Duplikat rader i responsen blir behandlet")
    public void test6(){
        stubRessurser("responses/dvh/dvhAnsatt3_duplikaterader.json");
        loadFromDvh();

        assertThat(ressursService.findByNavIdent("O123456").isPresent()).isTrue();
        assertThat(ressursService.getAll().size()).isEqualTo(1);
    }

    @Test
    @DisplayName("Navidenter mappet til ulike personidenter blir filtrert bort")
    public void test7(){
        stubRessurser("responses/dvh/dvhAnsatt3_sammeNavidentForskjelligPnr.json");
        loadFromDvh();

        assertThat(ressursService.findByNavIdent("O123456").isEmpty()).isTrue();
        assertThat(ressursService.getAll().size()).isEqualTo(0);

    }

    @Test
    @DisplayName("Personidenter mappet til ulike navIdenter blir filtrert bort")
    public void test8(){
        stubRessurser("responses/dvh/dvhAnsatt3_sammePnrForskjelligNavident.json");
        loadFromDvh();

        assertThat(ressursService.findByPersonIdent("12345678901").isEmpty()).isTrue();
        assertThat(ressursService.findByPersonIdent("12345678933").isEmpty()).isTrue();
    }

    @Test
    @DisplayName("Personidenter med flere navidenter, så velges den som er aktiv i remedy")
    public void test9(){
        stubRessurser("responses/dvh/dvhAnsatt3_sammePnrForskjelligNavident.json");
        loadFromDvh();

        assertThat(ressursService.findByNavIdent("B123456").isPresent()).isTrue();
    }

    // test helpers

    private void stubRessurser(String fileName) {
        stubFor(get("/proxy/dvh/json/Hr/NOM2/NOM_ANSATTE_BUCKETS_v1").willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json;charset=UTF-8")
                .withBodyFile(fileName)
        ));
    }

    private int hashRessurs(IRessurs r) {
        return r.getId().hashCode() ^ r.getNavIdent().hashCode() ^ r.getPersonIdent().hashCode();
    }

    private Collection<Integer> hashedRessurser(Collection<IRessurs> ressurser) {
        return ressurser
                .stream().map(this::hashRessurs).collect(Collectors.toCollection(ArrayList::new));
    }


}
