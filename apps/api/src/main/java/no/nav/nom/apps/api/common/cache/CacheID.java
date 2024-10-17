package no.nav.nom.apps.api.common.cache;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

@NoArgsConstructor
@Getter
public class CacheID implements java.io.Serializable {
    private String cacheKey;
    private String cacheType;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CacheQueue that)) return false;

        return new EqualsBuilder()
            .append(cacheKey, that.getCacheKey())
            .append(cacheType, that.getCacheType())
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(cacheKey)
            .append(cacheType)
            .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
            .append("cacheKey", cacheKey)
            .append("cacheType", cacheType)
            .toString();
    }
}
