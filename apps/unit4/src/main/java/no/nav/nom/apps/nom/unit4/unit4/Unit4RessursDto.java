package no.nav.nom.apps.nom.unit4.unit4;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.val;
import no.nav.nom.apps.nom.unit4.input.NomRessursDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Value
@Builder
@RequiredArgsConstructor
public class Unit4RessursDto {

    @JsonIgnore
    private final static LocalDateTime AGRESSO_TIMESTAMP_VOIDVALUE = LocalDateTime.parse("2099-12-31T00:00:00.000");
    @JsonIgnore
    private final static LocalDateTime AGRESSO_STARTTIME = LocalDate.now().atStartOfDay();
    @JsonIgnore
    LocalDateTime createdTime = LocalDate.now().atStartOfDay();


    AdditionalInformation additionalInformation;
    LocalDateTime birthDate;
    String companyId = "HR";
    LocalDateTime dateFrom = AGRESSO_STARTTIME;
    LocalDateTime dateTo = AGRESSO_TIMESTAMP_VOIDVALUE;
    String languageCode = "NO";
    String name;
    NameInformation nameInformation;
    PaymentInformation paymentInformation;
    PersonalInformation personalInformation;
    String personId;
    String personnelType = "F";
    String status = "N";
    List<ContactPoint> contactPoints = getContactPointsDefault();
    List<RelatedValue> relatedValues = getRelatedValuesDefault();



    public Unit4RessursDto(NomRessursDTO nomRessursDTO) {
        String ressursNr = nomRessursDTO.getNavident().substring(1);
        String firstName = Utils.convertName(nomRessursDTO.getFornavn()) + (nomRessursDTO.getMellomnavn() != null ? " "+Utils.convertName(nomRessursDTO.getMellomnavn()) : "");
        String surName = Utils.convertName(nomRessursDTO.getEtternavn());

        additionalInformation = AdditionalInformation.builder()
                .relatedPersonId(ressursNr)
                .build();

        val parsedBirthDate = LocalDate.parse(nomRessursDTO.getPersonident().substring(0, 6), DateTimeFormatter.ofPattern("ddMMyy"));

        birthDate = (parsedBirthDate.isAfter(LocalDate.now()) ? parsedBirthDate.minusYears(100) : parsedBirthDate).atStartOfDay();
        name = firstName + " " + surName;
        nameInformation = NameInformation.builder()
                .aliasName(nomRessursDTO.getNavident())
                .firstName(firstName)
                .surname(surName)
                .build();
        paymentInformation = PaymentInformation.builder()
                .supplierId(ressursNr)
                .build();
        personalInformation = PersonalInformation.builder()
                .gender("F")
                .nationalIdentityNo(nomRessursDTO.getPersonident())
                .build();
        personId = ressursNr;
    }

    private List<ContactPoint> getContactPointsDefault(){
        val contactPontOne = ContactPoint.builder()
                .address(new ContactPoint.Address())
                .contactPointType("1")
                .build();
        return List.of(contactPontOne);
    }

    private List<RelatedValue> getRelatedValuesDefault(){
        return List.of(
                RelatedValue.builder()
                        .relationId("M0")
                        .relatedValue("5224300")
                        .dateFrom(AGRESSO_STARTTIME)
                        .dateTo(AGRESSO_TIMESTAMP_VOIDVALUE)
                        .build(),
                RelatedValue.builder()
                        .relationId("C1")
                        .relatedValue("522400")
                        .dateFrom(AGRESSO_STARTTIME)
                        .dateTo(AGRESSO_TIMESTAMP_VOIDVALUE)
                        .build(),
                RelatedValue.builder()
                        .relationId("20")
                        .relatedValue("000000")
                        .dateFrom(AGRESSO_STARTTIME)
                        .dateTo(AGRESSO_TIMESTAMP_VOIDVALUE)
                        .build(),
                RelatedValue.builder()
                        .relationId("C8")
                        .relatedValue("TLON")
                        .dateFrom(AGRESSO_STARTTIME)
                        .dateTo(AGRESSO_TIMESTAMP_VOIDVALUE)
                        .build(),
                RelatedValue.builder()
                        .relationId("AL")
                        .relatedValue("060501110000")
                        .dateFrom(AGRESSO_STARTTIME)
                        .dateTo(AGRESSO_TIMESTAMP_VOIDVALUE)
                        .build(),
                RelatedValue.builder()
                        .relationId("ZA")
                        .relatedValue("060501180000")
                        .dateFrom(AGRESSO_STARTTIME)
                        .dateTo(AGRESSO_TIMESTAMP_VOIDVALUE)
                        .build(),
                RelatedValue.builder()
                        .relationId("B1")
                        .relatedValue("000000")
                        .dateFrom(AGRESSO_STARTTIME)
                        .dateTo(AGRESSO_TIMESTAMP_VOIDVALUE)
                        .build(),
                RelatedValue.builder()
                        .relationId("MQ")
                        .relatedValue("BB")
                        .dateFrom(AGRESSO_STARTTIME)
                        .dateTo(AGRESSO_TIMESTAMP_VOIDVALUE)
                        .build(),
                RelatedValue.builder()
                        .relationId("Y7")
                        .relatedValue("SPK_A")
                        .dateFrom(AGRESSO_STARTTIME)
                        .dateTo(AGRESSO_TIMESTAMP_VOIDVALUE)
                        .build()
        );
    }
}
