package no.nav.nom.apps.api.common.cache;

import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.annotations.Type;

@Data
@Entity
@Builder
@Table(name = "T_CACHE_QUEUE")
@AllArgsConstructor
@NoArgsConstructor
@IdClass(CacheID.class)
public class CacheQueue {
    @Id
    @NotNull
    @Column(name = "CACHE_KEY", nullable = false, updatable = false)
    private String cacheKey;

    @Id
    @NotNull
    @Column(name = "CACHE_TYPE", nullable = false, updatable = false)
    private String cacheType;

    @NotNull
    @Type(JsonBinaryType.class)
    @Column(name = "PAYLOAD", nullable = false, updatable = false)
    private JsonNode payload;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CacheQueue that)) return false;

        return new EqualsBuilder().append(cacheKey, that.cacheKey)
            .append(cacheType, that.cacheType)
            .append(payload, that.payload)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(cacheKey).append(cacheType).append(payload).toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
            .append("cacheKey", cacheKey)
            .append("cacheType", cacheType)
            .append("payload", payload)
            .toString();
    }
}
