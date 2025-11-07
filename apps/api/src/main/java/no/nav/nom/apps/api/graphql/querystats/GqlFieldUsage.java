package no.nav.nom.apps.api.graphql.querystats;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@Table(name = "T_GQL_FIELD_USAGE")
@AllArgsConstructor
@NoArgsConstructor
public class GqlFieldUsage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    private String field;

    @NotNull
    private String path;

    @NotNull
    private String operationName;

    @NotNull
    private String consumer;

    @NotNull
    private LocalDateTime timestamp;
}
