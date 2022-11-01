package no.nav.nom.apps.api.orgenhet.implementations.dvh.v2;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import no.nav.nom.apps.api.postgresql.GenericStorage;
import no.nav.nom.apps.api.postgresql.GenericStorageElement;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ValidationService {

    private final ObjectMapper objectMapper;

    private final ValidationRepository validationRepository;

    public void saveValidations(List<Validation> validations){
        validationRepository.saveAll(
                diffValidation(validations).stream()
                        .map(v -> GenericStorageElement.builder()
                                .id(UUID.randomUUID())
                                .document(objectMapper.valueToTree(v))
                                .storedDate(LocalDateTime.now())
                                .dataType(GenericStorageElement.DataType.VALIDATION_V1)
                                .build()
                        )
                        .toList()
        );
    }

    private List<Validation> diffValidation(List<Validation> validations){
        val validationsCopy = new LinkedList<>(validations);
        validationsCopy.removeAll(validationRepository.findAllInValidationCode(
                validations.stream().map(Validation::getValidationCode).distinct().toList()
        ).stream().map(g -> objectMapper.convertValue(g.getDocument(), Validation.class)).toList());
        return validationsCopy;
    }
}
