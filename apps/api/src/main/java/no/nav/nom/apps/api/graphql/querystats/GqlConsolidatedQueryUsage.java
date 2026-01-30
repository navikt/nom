package no.nav.nom.apps.api.graphql.querystats;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

@Data
@Entity
@Builder
@Table(name = "T_GQL_CONSOLIDATED_QUERY_USAGE")
@AllArgsConstructor
@NoArgsConstructor
public class GqlConsolidatedQueryUsage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    private String hash;

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

