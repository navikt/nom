package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.errorstate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@ToString
@NoArgsConstructor
@EqualsAndHashCode
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(name = "error_state")
public class ErrorState {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "error_id")
    private UUID errorId;

    @Column(name = "error_state_fid")
    private String errorStateFid;

    @Column(name = "error_state_type")
    private String errorStateType;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "navident")
    private String navident;
}
