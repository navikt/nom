package no.nav.nom.apps.api.postgresql;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

@Data
@Entity
@Builder
@Table(name = "T_GENERIC_STORAGE")
@AllArgsConstructor
@NoArgsConstructor
public class GenericStorageElement {

    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "ID", nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @Convert(converter = DataTypeConverter.class)
    @Column(name = "DATA_TYPE", nullable = false, updatable = false)
    private DataType dataType;

    @Type(JsonBinaryType.class)
    @Column(name = "DOCUMENT", nullable = false, updatable = false)
    private JsonNode document;

    @NotNull
    @Column(name = "STORED_DATE", nullable = false, updatable = false)
    private LocalDateTime storedDate;

    public enum DataType {
        HENDELSE_V1("no.nav.nom.generic.storage:Hendelse:v1"),
        VALIDATION_V1("no.nav.nom.generic.storage:Validation:v1");

        String dataType;

        DataType(String dataType) {
            this.dataType = dataType;
        }

        @JsonValue
        public String getDataType() {
            return dataType;
        }

        public static DataType fromString(String value) {
            return Arrays.stream(DataType.values())
                    .filter(v -> v.getDataType().equals(value))
                    .findAny()
                    .orElseThrow(() -> new IllegalArgumentException("No enum with value " + value + " found"));
        }
    }

    @Converter
    static class DataTypeConverter implements AttributeConverter<DataType, String> {

        @Override
        public String convertToDatabaseColumn(DataType dataType) {
            return dataType.getDataType();
        }

        @Override
        public DataType convertToEntityAttribute(String dataTypeValue) {
            return DataType.fromString(dataTypeValue);
        }
    }
}
