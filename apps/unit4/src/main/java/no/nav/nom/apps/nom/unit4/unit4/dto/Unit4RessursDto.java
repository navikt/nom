package no.nav.nom.apps.nom.unit4.unit4.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.val;
import no.nav.nom.apps.nom.unit4.input.NomRessursDTO;
import no.nav.nom.apps.nom.unit4.unit4.Utils;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1.RessursOpprettetAdditionalData;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1.RessursOpprettetAdditionalData.FolkeregistrertNavn;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1.RessursOpprettetEventData;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Value
@Builder
@RequiredArgsConstructor
public class Unit4RessursDto {

    @JsonIgnore
    private final static LocalDateTime AGRESSO_TIMESTAMP_NULL_VALUE = LocalDateTime.parse("2099-12-31T00:00:00.000");
    @JsonIgnore
    private final static LocalDateTime AGRESSO_STARTTIME = LocalDate.now().atStartOfDay();
    @JsonIgnore
    LocalDateTime createdTime = LocalDate.now().atStartOfDay();


    AdditionalInformation additionalInformation;
    LocalDateTime birthDate;
    String companyId = "HR";
    LocalDateTime dateFrom;
    LocalDateTime dateTo = AGRESSO_TIMESTAMP_NULL_VALUE;
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


        LocalDateTime birthDateTemp;
        try {
            val parsedBirthDate = LocalDate.parse(nomRessursDTO.getPersonident().substring(0, 6), DateTimeFormatter.ofPattern("ddMMyy"));
            birthDateTemp = (parsedBirthDate.isAfter(LocalDate.now()) ? parsedBirthDate.minusYears(100) : parsedBirthDate).atStartOfDay();
        } catch (Exception ex){
            birthDateTemp = LocalDate.parse("1995-04-12").atStartOfDay();
        }
        birthDate = birthDateTemp;
        dateFrom = AGRESSO_STARTTIME;
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
        relatedValues.add(
                RelatedValue.builder()
                .relationId("M0")
                .relatedValue("5224300")
                .dateFrom(AGRESSO_STARTTIME)
                .dateTo(AGRESSO_TIMESTAMP_NULL_VALUE)
                .build()
        );
    }

    public Unit4RessursDto(RessursOpprettetEventV1 record) throws IllegalArgumentException {
        RessursOpprettetEventData eventData = (RessursOpprettetEventData) record.getData();
        RessursOpprettetAdditionalData additionalData = (RessursOpprettetAdditionalData) record.getAdditionalData();

        String navident = additionalData.navident().orElseThrow(() -> new NoSuchElementException("Navident is missing from event"));
        String ressursNr = navident.substring(1);

        String firstName = additionalData.folkeregistrertNavn().map(navn -> navn.fornavn() + Optional.ofNullable(navn.mellomnavn()).orElse(""))
                .orElseThrow(() -> new NoSuchElementException("FolkeregistrertNavn is missing from event"));

        String surName = additionalData.folkeregistrertNavn().map(FolkeregistrertNavn::etternavn)
                .orElseThrow(() -> new NoSuchElementException("FolkeregistrertNavn is missing from event"));

        String kjoenn = additionalData.kjoenn().map(k ->
                switch (k){
                    case "MANN" -> "M";
                    case "KVINNE" -> "F";
                    default -> null;
                }
        ).orElseThrow(() -> new NoSuchElementException("Folkeregistrert kjoenn is missing from event"));
        dateFrom =  additionalData.startdato().map(LocalDate::atStartOfDay).orElse(AGRESSO_STARTTIME);
        birthDate = ((RessursOpprettetAdditionalData) record.getAdditionalData()).foedselsdato().map(LocalDate::atStartOfDay)
                .orElseThrow(() -> new NoSuchElementException("Foedseldato is missing from event"));

        additionalInformation = AdditionalInformation.builder()
                .relatedPersonId(ressursNr)
                .build();
        
        name = firstName + " " + surName;
        nameInformation = NameInformation.builder()
                .aliasName(navident)
                .firstName(firstName)
                .surname(surName)
                .build();
        paymentInformation = PaymentInformation.builder()
                .supplierId(ressursNr)
                .build();
        personalInformation = PersonalInformation.builder()
                .gender(kjoenn)
                .nationalIdentityNo(eventData.personident())
                .build();
        personId = ressursNr;
        relatedValues.add(
                additionalData.orgEnhet()
                        .map((orgenhet) -> RelatedValue.builder()
                                .relationId("M0")
                                .relatedValue(additionalData.orgEnhet().get())
                                .dateFrom(AGRESSO_STARTTIME)
                                .dateTo(AGRESSO_TIMESTAMP_NULL_VALUE)
                                .build()
                        ).orElse(RelatedValue.builder()
                                .relationId("M0")
                                .relatedValue("5224300")
                                .dateFrom(AGRESSO_STARTTIME)
                                .dateTo(AGRESSO_TIMESTAMP_NULL_VALUE)
                                .build()));
    }

    private List<ContactPoint> getContactPointsDefault(){
        val contactPontOne = ContactPoint.builder()
                .contactPointType("1")
                .build();
        return List.of(contactPontOne);
    }

    private List<RelatedValue> getRelatedValuesDefault(){
        return Stream.of(
                // Default verdi. Endres i unit4 etter opprettelse ved behov.
                RelatedValue.builder()
                        .relationId("23")   // 23: Begrep=FELLES, Beskrivelse=Felles
                        .relatedValue("L03034") // Skal være 000000 USPESIFISERT, men denne verdien finnes ikke enda.L03034 står for "Oslo, Fyrstikkalléen 1"
                        .dateFrom(AGRESSO_STARTTIME)
                        .dateTo(AGRESSO_TIMESTAMP_NULL_VALUE)
                        .build(),

                // Uspesifisert verdi som gjelder det fleste. Verdien Skal settes til dette for default, dermed endres det etter behov.
                // Eksterne skal muligens ha annen default verdi når det blir aktuelt.
                RelatedValue.builder()
                        .relationId("20")   // 20: Begrep=PRODUKT, Beskrivelse=Produkt
                        .relatedValue("000000") // 000000: Uspesifisert
                        .dateFrom(AGRESSO_STARTTIME)
                        .dateTo(AGRESSO_TIMESTAMP_NULL_VALUE)
                        .build(),

                // Varierer basert på INTERN/EKSTERN/MIDLERTIDIG
                RelatedValue.builder()
                        .relationId("C8")   // C8: Begrep=GRUPPE, Beskrivelse=Gruppe
                        .relatedValue("FLON") // FLON: 5000 - Oppg. Faste stillinger, ordinær
                        .dateFrom(AGRESSO_STARTTIME)
                        .dateTo(AGRESSO_TIMESTAMP_NULL_VALUE)
                        .build(),

                // Default verdi, 2 typer eksterne som kanskje skal ha annen default verdi.
                RelatedValue.builder()
                        .relationId("B1")   // B1: Begrep=OPPGAVE, Beskrivelse=Oppgave
                        .relatedValue("000000") // 000000: Uspesifisert
                        .dateFrom(AGRESSO_STARTTIME)
                        .dateTo(AGRESSO_TIMESTAMP_NULL_VALUE)
                        .build(),

                // Default verdi for alle
                RelatedValue.builder()
                        .relationId("MQ")   // MQ: Begrep=AGASTAT, Beskrivelse=Arbeidsgiveravgift
                        .relatedValue("BB") // BB: Statsforvaltning og helseforetak
                        .dateFrom(AGRESSO_STARTTIME)
                        .dateTo(AGRESSO_TIMESTAMP_NULL_VALUE)
                        .build(),

                // Default verdi for alle faste og midlertidige og timelønnet.
                // Kommunale Eksterne står det Nei på.
                RelatedValue.builder()
                        .relationId("Y7") // Y7: Begrep=PENSJON, Beskrivelse=Pensjonsplan - nivå
                        .relatedValue("SPK") // SPK: Pensjonstrekk SPK 2%
                        .dateFrom(AGRESSO_STARTTIME)
                        .dateTo(AGRESSO_TIMESTAMP_NULL_VALUE)
                        .build(),

                // Flagg for å indikere at ressursen er opprettet av NOM
                RelatedValue.builder()
                        .relationId("NOM")  // NOM: Begrep=AUTO_GEN_NOM, Beskrivelse=Generert av NOM
                        .relatedValue("1")  // 1: Generert av NOM
                        .dateFrom(AGRESSO_STARTTIME)
                        .dateTo(AGRESSO_TIMESTAMP_NULL_VALUE)
                        .build()

        ).collect(Collectors.toList());
    }
}
