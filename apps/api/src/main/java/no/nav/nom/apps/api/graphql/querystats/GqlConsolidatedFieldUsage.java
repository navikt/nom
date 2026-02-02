package no.nav.nom.apps.api.graphql.querystats;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import tools.jackson.databind.JsonNode;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

@Data
@Entity
@Builder
@Table(name = "T_GQL_CONSOLIDATED_FIELD_USAGE")
@AllArgsConstructor
@NoArgsConstructor
public class GqlConsolidatedFieldUsage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Type(JsonBinaryType.class)
    @NotNull
    private JsonNode fieldPathArray;

    @NotNull
    private String consumer;

    @NotNull
    private Integer year;

    @NotNull
    private Integer month;

    @NotNull
    private Integer day;

    @NotNull
    private Integer count;
}

