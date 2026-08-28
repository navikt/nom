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
| `userId` | `String` | User id link. |

## Navigation fields

| Field | Java type | Target |
|---|---|---|
| `emailNav` | `List<PerEmail>` | `emailNav` |
| `emergencyContactNav` | `List<PerEmergencyContacts>` | `emergencyContactNav` |
| `employmentNav` | `List<EmpEmployment>` | `employmentNav` |
| `homeAddressNavDEFLT` | `List<PerAddressDEFLT>` | `homeAddressNavDEFLT` |
| `nationalIdNav` | `List<PerNationalId>` | `nationalIdNav` |
| `personEmpTerminationInfoNav` | `PersonEmpTerminationInfo` | `personEmpTerminationInfoNav` |
| `personTypeUsageNav` | `PersonTypeUsage` | `personTypeUsageNav` |
| `personalInfoNav` | `List<PerPersonal>` | `personalInfoNav` |
| `phoneNav` | `List<PerPhone>` | `phoneNav` |
| `secondaryAssignmentsNav` | `List<SecondaryAssignments>` | `secondaryAssignmentsNav` |
| `userAccountNav` | `UserAccount` | `userAccountNav` |

