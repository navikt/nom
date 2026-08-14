package no.nav.nom.apps.api.graphql.ressurs;

import no.nav.nom.apps.api.common.RessursStateBuilder;
import no.nav.nom.apps.api.graphql.GraphQlTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles({"GraphQLTest", "localgql"})
@GraphQlTest
public class RessurserPaginertTest extends GraphQlTestBase {

    @BeforeEach
    protected void setup() {
        ressursStateRepository.clear();
        orgEnhetStateRepository.clear();
        organiseringStateRepository.clear();
        orgtilknytningStateRepository.clear();
    }

    private void seedRessurser(int antall, int startSuffix, String fysiskBrukersted) {
        for (int i = 0; i < antall; i++) {
            var state = new RessursStateBuilder(startSuffix + i)
                .primaryFysiskBrukersted(fysiskBrukersted)
                .build();
            ressursStateRepository.update(state);
        }
    }

    @Test
    @DisplayName("Første side filtrert på fysiskBrukersted gir korrekt metadata og totalCount")
    void foerste_side_gir_korrekt_metadata_og_filtrert_total() {
        seedRessurser(250, 1, "2700");
        seedRessurser(50, 1000, "9999");

        var respons = gqlUtil.postForResource(
            "ressurs/ressurserPaginert.graphql",
            Map.of(
                "filter", Map.of("fysiskBrukersted", "2700"),
                "paginering", Map.of("pageNumber", 0, "pageSize", 100)
            )
        );

        respons.path("ressurserPaginert.totalCount").entity(Integer.class).isEqualTo(250);
        respons.path("ressurserPaginert.totalPages").entity(Integer.class).isEqualTo(3);
        respons.path("ressurserPaginert.numberOfElements").entity(Integer.class).isEqualTo(100);
        respons.path("ressurserPaginert.first").entity(Boolean.class).isEqualTo(true);
        respons.path("ressurserPaginert.last").entity(Boolean.class).isEqualTo(false);
        respons.path("ressurserPaginert.hasNextPage").entity(Boolean.class).isEqualTo(true);
        respons.path("ressurserPaginert.hasPreviousPage").entity(Boolean.class).isEqualTo(false);
        respons.path("ressurserPaginert.content").entityList(Map.class).hasSize(100);
        respons.path("ressurserPaginert.content[0].navident").entity(String.class).isEqualTo("A000001");
    }

    @Test
    @DisplayName("Siste side er delvis og har ikke neste side")
    void siste_side_er_delvis_og_uten_neste() {
        seedRessurser(250, 1, "2700");

        var respons = gqlUtil.postForResource(
            "ressurs/ressurserPaginert.graphql",
            Map.of(
                "filter", Map.of("fysiskBrukersted", "2700"),
                "paginering", Map.of("pageNumber", 2, "pageSize", 100)
            )
        );

        respons.path("ressurserPaginert.numberOfElements").entity(Integer.class).isEqualTo(50);
        respons.path("ressurserPaginert.last").entity(Boolean.class).isEqualTo(true);
        respons.path("ressurserPaginert.hasNextPage").entity(Boolean.class).isEqualTo(false);
        respons.path("ressurserPaginert.hasPreviousPage").entity(Boolean.class).isEqualTo(true);
    }

    @Test
    @DisplayName("Ugyldig sidestørrelse gir feil")
    void ugyldig_sidestoerrelse_gir_feil() {
        seedRessurser(10, 1, "2700");

        gqlUtil.postForResource(
            "ressurs/ressurserPaginert.graphql",
            Map.of(
                "filter", Map.of("fysiskBrukersted", "2700"),
                "paginering", Map.of("pageNumber", 0, "pageSize", 0)
            )
        ).errors().satisfy(errors -> assertThat(errors).isNotEmpty());
    }
}
