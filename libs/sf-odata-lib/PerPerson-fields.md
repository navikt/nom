# PerPerson entity fields

Source: `target/generated-sources/vdm/no/nav/nom/sf/odata/vdm/namespaces/successfactors/PerPerson.java`

This is a simple overview of the `PerPerson` entity fields.

## Key fields

- `personIdExternal` (`String`) - external person id.

## Scalar fields

| Field | Java type | Notes |
|---|---|---|
| `createdBy` | `String` | User id that created the record. |
| `createdDateTime` | `ZonedDateTime` | Creation timestamp (with timezone). |
| `createdOn` | `LocalDateTime` | Creation datetime. |
| `customDouble1` | `Double` | Custom numeric field 1. |
| `customString1` | `String` | Custom string field 1. |
| `dateOfBirth` | `LocalDateTime` | Date of birth. |
| `includeAllRecords` | `Boolean` | Include all records flag. |
| `lastModifiedBy` | `String` | Last modifying user id. |
| `lastModifiedDateTime` | `ZonedDateTime` | Last modification timestamp (with timezone). |
| `lastModifiedOn` | `LocalDateTime` | Last modification datetime. |
| `perPersonUuid` | `String` | Per-person UUID. |
| `personId` | `Long` | Internal person id. |
| `personIdExternal` | `String` | External person id (key). |
| `userId` | `String` | User id link. |

## Navigation fields

| Field | Java type | Target |
|---|---|---|
| `toEmailNav` | `List<PerEmail>` | `emailNav` |
| `toEmergencyContactNav` | `List<PerEmergencyContacts>` | `emergencyContactNav` |
| `toEmploymentNav` | `List<EmpEmployment>` | `employmentNav` |
| `toHomeAddressNavDEFLT` | `List<PerAddressDEFLT>` | `homeAddressNavDEFLT` |
| `toNationalIdNav` | `List<PerNationalIdWithValidityPeriod>` | `nationalIdNav` |
| `toPersonEmpTerminationInfoNav` | `PersonEmpTerminationInfo` | `personEmpTerminationInfoNav` |
| `toPersonRerlationshipNav` | `List<PerPersonRelationship>` | `personRerlationshipNav` |
| `toPersonTypeUsageNav` | `PersonTypeUsage` | `personTypeUsageNav` |
| `toPersonalInfoNav` | `List<PerPersonal>` | `personalInfoNav` |
| `toPhoneNav` | `List<PerPhone>` | `phoneNav` |
| `toSecondaryAssignmentsNav` | `List<SecondaryAssignments>` | `secondaryAssignmentsNav` |
| `toUserAccountNav` | `UserAccount` | `userAccountNav` |
