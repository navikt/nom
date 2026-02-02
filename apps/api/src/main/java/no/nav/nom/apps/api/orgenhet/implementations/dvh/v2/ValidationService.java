package no.nav.nom.apps.api.orgenhet.implementations.dvh.v2;


import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ValidationService {

    private final JsonMapper jsonMapper;

    private final ValidationRepository validationRepository;

    public void saveValidations(List<Validation> validations) {
        /*
        validationRepository.saveAll(
                diffValidation(validations).stream()
                        .map(v -> GenericStorageElement.builder()
                                .id(UUID.randomUUID())
                                .document(jsonMapper.valueToTree(v))
                                .storedDate(LocalDateTime.now())
                                .dataType(GenericStorageElement.DataType.VALIDATION_V1)
                                .build())
                        .toList());

         */
    }

    private List<Validation> diffValidation(List<Validation> validations) {
        val validationsCopy = new LinkedList<>(validations);
        validationsCopy.removeAll(
                validationRepository.findAllInValidationCode(validations.stream().map(Validation::getValidationCode).distinct().toList())
                        .stream()
                        .map(g -> jsonMapper.convertValue(g.getDocument(), Validation.class))
                        .toList());
        return validationsCopy;
    }
}
