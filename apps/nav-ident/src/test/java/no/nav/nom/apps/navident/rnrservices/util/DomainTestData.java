package no.nav.nom.apps.navident.rnrservices.util;

import no.nav.nom.apps.navident.domain.*;
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

        navidenter.put(KNYTNING_FINNES_1_NavId, knytning_finnes_1());
        navidenter.put(KNYTNING_FINNES_2_NavId, knytning_finnes_2());

        navidenter.put(KNYTNING_FINNES_FLERE_ID_3_NavId, knytning_finnes_flere_id_3());

        navidenter.put(KNYTNING_FINNES_DUPLIKAT_NavId_4_NavId_1, knytning_finnes_duplikat_navId_1());
        navidenter.put(KNYTNING_FINNES_DUPLIKAT_NavId_4_NavId_2, knytning_finnes_duplikat_navId_2());
    }

    public static String ROBOT_1_PersonId = "X1019900001";
    public static String ROBOT_1_NavId = "X000001";

    public static String ROBOT_2_PersonId = "X1019900002";
    public static String ROBOT_2_NavId = "X000002";

    public static String KNYTNING_FINNES_IKKE_1_PersonId = "01019900000";

    public static String KNYTNING_FINNES_1_PersonId = "01019900001";
    public static String KNYTNING_FINNES_1_NavId = "A000001";

    public static String KNYTNING_FINNES_2_PersonId = "01019900002";
    public static String KNYTNING_FINNES_2_NavId = "A000002";

    public static String KNYTNING_FINNES_FLERE_ID_3_PersonId_1= "01019900003";
    public static String KNYTNING_FINNES_FLERE_ID_3_PersonId_2= "02019900003";
    public static String KNYTNING_FINNES_FLERE_ID_3_NavId = "A000003";

    public static String KNYTNING_FINNES_DUPLIKAT_NavId_4_PersonId = "01019900004";
    public static String KNYTNING_FINNES_DUPLIKAT_NavId_4_NavId_1 = "A000004";
    public static String KNYTNING_FINNES_DUPLIKAT_NavId_4_NavId_2 = "B000004";



    private Navident robot_1() {
        return Navident.builder()
                .navident(ROBOT_1_NavId)
                .identType(IdentType.ROBOT)
                .gjeldendePersonid(ROBOT_1_PersonId)
                .allePersonider(Arrays.asList(
                        Personid.builder().id(ROBOT_1_PersonId).type(PersonidType.MANUELL).build()
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
                .build();
    }

    private Navident knytning_finnes_1() {
        return Navident.builder()
                .navident(KNYTNING_FINNES_1_NavId)
                .identType(IdentType.STANDARD)
                .gjeldendePersonid(KNYTNING_FINNES_1_PersonId)
                .allePersonider(Arrays.asList(
                        Personid.builder().id(KNYTNING_FINNES_1_PersonId).type(PersonidType.PDL).build()
                ))
                .build();
    }

    private Navident knytning_finnes_2() {
        return Navident.builder()
                .navident(KNYTNING_FINNES_2_NavId)
                .identType(IdentType.STANDARD)
                .gjeldendePersonid(KNYTNING_FINNES_2_PersonId)
                .allePersonider(Arrays.asList(
                        Personid.builder().id(KNYTNING_FINNES_2_PersonId).type(PersonidType.PDL).build()
                ))
                .build();
    }

    private Navident knytning_finnes_flere_id_3() {
        return Navident.builder()
                .navident(KNYTNING_FINNES_FLERE_ID_3_NavId)
                .identType(IdentType.STANDARD)
                .gjeldendePersonid(KNYTNING_FINNES_FLERE_ID_3_PersonId_1)
                .allePersonider(Arrays.asList(
                        Personid.builder().id(KNYTNING_FINNES_FLERE_ID_3_PersonId_1).type(PersonidType.PDL).build(),
                        Personid.builder().id(KNYTNING_FINNES_FLERE_ID_3_PersonId_2).type(PersonidType.PDL).build()
                ))
                .build();
    }

    private Navident knytning_finnes_duplikat_navId_1() {
        return Navident.builder()
                .navident(KNYTNING_FINNES_DUPLIKAT_NavId_4_NavId_1)
                .identType(IdentType.STANDARD)
                .gjeldendePersonid(KNYTNING_FINNES_DUPLIKAT_NavId_4_PersonId)
                .allePersonider(Arrays.asList(
                        Personid.builder().id(KNYTNING_FINNES_DUPLIKAT_NavId_4_PersonId).type(PersonidType.PDL).build()
                ))
                .build();
    }

    private Navident knytning_finnes_duplikat_navId_2() {
        return Navident.builder()
                .navident(KNYTNING_FINNES_DUPLIKAT_NavId_4_NavId_2)
                .identType(IdentType.STANDARD)
                .gjeldendePersonid(KNYTNING_FINNES_DUPLIKAT_NavId_4_PersonId)
                .allePersonider(Arrays.asList(
                        Personid.builder().id(KNYTNING_FINNES_DUPLIKAT_NavId_4_PersonId).type(PersonidType.PDL).build()
                ))
                .build();
    }
}
