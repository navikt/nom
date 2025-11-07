package no.nav.nom.apps.api.graphql.querystats;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Builder
@Table(name = "T_GQL_QUERY_USAGE")
@AllArgsConstructor
@NoArgsConstructor
public class GqlQueryUsage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    private String hash;

    @NotNull
    private String consumer;

    @NotNull
    private LocalDateTime timestamp;
}
