package no.nav.nom.apps.navident.rnrservices.util;

import no.nav.nom.apps.navident.domain.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;

@Component
public class DomainTestData {


    public void prepNavidenter(Map<String, Navident> navidenter) {
        navidenter.clear();
        navidenter.put(ROBOT_1_NavId, robot_1());
        navidenter.put(ROBOT_2_NavId, robot_2());
        navidenter.put(TIDLIGERE_ANSATT_1_NavId, tidligere_ansatt_1());
        navidenter.put(NÅVÆRENDE_ANSATT_2_NavId, nåværende_ansatt_2());
        navidenter.put(TIDLIGERE_ANSATT_FLERE_ID_3_NavId, tidligere_ansatt_flere_id_3());

        navidenter.put(TIDLIGERE_ANSATT_DUPLIKAT_NavId_4_NavId_1, tidligere_ansatt_duplikat_navId_1());
        navidenter.put(TIDLIGERE_ANSATT_DUPLIKAT_NavId_4_NavId_2, tidligere_ansatt_duplikat_navId_2());
    }

    public static String ROBOT_1_PersonId = "X1019900001";
    public static String ROBOT_1_NavId = "X000001";

    public static String ROBOT_2_PersonId = "X1019900002";
    public static String ROBOT_2_NavId = "X000002";

    public static String IKKE_TIDLIGERE_ANSATT_1_PersonId = "01019900000";

    public static String TIDLIGERE_ANSATT_1_PersonId = "01019900001";
    public static String TIDLIGERE_ANSATT_1_NavId = "A000001";

    public static String NÅVÆRENDE_ANSATT_2_PersonId = "01019900002";
    public static String NÅVÆRENDE_ANSATT_2_NavId = "A000002";

    public static String TIDLIGERE_ANSATT_FLERE_ID_3_PersonId_1= "01019900003";
    public static String TIDLIGERE_ANSATT_FLERE_ID_3_PersonId_2= "02019900003";
    public static String TIDLIGERE_ANSATT_FLERE_ID_3_NavId = "A000003";

    public static String TIDLIGERE_ANSATT_DUPLIKAT_NavId_4_PersonId = "01019900004";
    public static String TIDLIGERE_ANSATT_DUPLIKAT_NavId_4_NavId_1 = "A000004";
    public static String TIDLIGERE_ANSATT_DUPLIKAT_NavId_4_NavId_2 = "B000004";



    private Navident robot_1() {
        return Navident.builder()
                .navident(ROBOT_1_NavId)
                .identType(IdentType.ROBOT)
                .gjeldendePersonid(ROBOT_1_PersonId)
                .allePersonider(Arrays.asList(
                        Personid.builder().id(ROBOT_1_PersonId).type(PersonidType.MANUELL).build()
                ))
                .aktivePerioder(Arrays.asList(
                        Periode.builder().fraOgMed(LocalDate.now().minusYears(2)).build()
                ))
                .build();
    }


    private Navident robot_2() {
        return Navident.builder()
                .navident(ROBOT_2_NavId)
                .identType(IdentType.ROBOT)
                .gjeldendePersonid(ROBOT_2_PersonId)
                .allePersonider(Arrays.asList(
                        Personid.builder().id(ROBOT_2_PersonId).type(PersonidType.MANUELL).build()
                ))
                .aktivePerioder(Arrays.asList(
                        Periode.builder().fraOgMed(LocalDate.now().minusYears(2)).tilOgMed(LocalDate.now().minusMonths(6)).build()
                ))
                .build();
    }

    private Navident tidligere_ansatt_1() {
        return Navident.builder()
                .navident(TIDLIGERE_ANSATT_1_NavId)
                .identType(IdentType.STANDARD)
                .gjeldendePersonid(TIDLIGERE_ANSATT_1_PersonId)
                .allePersonider(Arrays.asList(
                        Personid.builder().id(TIDLIGERE_ANSATT_1_PersonId).type(PersonidType.PDL).build()
                ))
                .aktivePerioder(Arrays.asList(
                        Periode.builder().fraOgMed(LocalDate.now().minusYears(2)).tilOgMed(LocalDate.now().minusYears(1)).build()
                ))
                .build();
    }

    private Navident nåværende_ansatt_2() {
        return Navident.builder()
                .navident(NÅVÆRENDE_ANSATT_2_NavId)
                .identType(IdentType.STANDARD)
                .gjeldendePersonid(NÅVÆRENDE_ANSATT_2_PersonId)
                .allePersonider(Arrays.asList(
                        Personid.builder().id(NÅVÆRENDE_ANSATT_2_PersonId).type(PersonidType.PDL).build()
                ))
                .aktivePerioder(Arrays.asList(
                        Periode.builder().fraOgMed(LocalDate.now().minusYears(2)).build()
                ))
                .build();
    }

    private Navident tidligere_ansatt_flere_id_3() {
        return Navident.builder()
                .navident(TIDLIGERE_ANSATT_FLERE_ID_3_NavId)
                .identType(IdentType.STANDARD)
                .gjeldendePersonid(TIDLIGERE_ANSATT_FLERE_ID_3_PersonId_1)
                .allePersonider(Arrays.asList(
                        Personid.builder().id(TIDLIGERE_ANSATT_FLERE_ID_3_PersonId_1).type(PersonidType.PDL).build(),
                        Personid.builder().id(TIDLIGERE_ANSATT_FLERE_ID_3_PersonId_2).type(PersonidType.PDL).build()
                ))
                .aktivePerioder(Arrays.asList(
                        Periode.builder().fraOgMed(LocalDate.now().minusYears(2)).tilOgMed(LocalDate.now().minusYears(1)).build()
                ))
                .build();
    }

    private Navident tidligere_ansatt_duplikat_navId_1() {
        return Navident.builder()
                .navident(TIDLIGERE_ANSATT_DUPLIKAT_NavId_4_NavId_1)
                .identType(IdentType.STANDARD)
                .gjeldendePersonid(TIDLIGERE_ANSATT_DUPLIKAT_NavId_4_PersonId)
                .allePersonider(Arrays.asList(
                        Personid.builder().id(TIDLIGERE_ANSATT_DUPLIKAT_NavId_4_PersonId).type(PersonidType.PDL).build()
                ))
                .aktivePerioder(Arrays.asList(
                        Periode.builder().fraOgMed(LocalDate.now().minusYears(2)).tilOgMed(LocalDate.now().minusYears(1)).build()
                ))
                .build();
    }

    private Navident tidligere_ansatt_duplikat_navId_2() {
        return Navident.builder()
                .navident(TIDLIGERE_ANSATT_DUPLIKAT_NavId_4_NavId_2)
                .identType(IdentType.STANDARD)
                .gjeldendePersonid(TIDLIGERE_ANSATT_DUPLIKAT_NavId_4_PersonId)
                .allePersonider(Arrays.asList(
                        Personid.builder().id(TIDLIGERE_ANSATT_DUPLIKAT_NavId_4_PersonId).type(PersonidType.PDL).build()
                ))
                .aktivePerioder(Arrays.asList(
                        Periode.builder().fraOgMed(LocalDate.now().minusMonths(8)).tilOgMed(LocalDate.now().minusMonths(2)).build()
                ))
                .build();
    }
}
