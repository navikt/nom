# FODepartment entity fields

Source: `target/generated-sources/vdm/no/nav/nom/sf/odata/vdm/namespaces/successfactors/FODepartment.java`

This is a simple overview of the `FODepartment` entity fields.

## Key fields

- `externalCode` (`String`) - external/business department code.
- `startDate` (`LocalDateTime`) - start of validity period.

## Scalar fields

| Field | Java type | Notes |
|---|---|---|
| `costCenter` | `String` | Cost center reference. |
| `createdBy` | `String` | User id that created the record. |
| `createdDateTime` | `ZonedDateTime` | Creation timestamp (with timezone). |
| `createdOn` | `LocalDateTime` | Creation datetime. |
| `cust_Division` | `String` | Custom division reference. |
| `cust_Location` | `String` | Custom location reference. |
| `cust_Virksomhet` | `String` | Custom virksomhet reference. |
| `description` | `String` | Department description. |
| `description_defaultValue` | `String` | Default description translation value. |
| `description_en_DEBUG` | `String` | Description (`en_DEBUG`). |
| `description_en_US` | `String` | Description (`en_US`). |
| `description_localized` | `String` | Localized description value. |
| `description_nb_NO` | `String` | Description (`nb_NO`). |
| `endDate` | `LocalDateTime` | End of validity period. |
| `entityOID` | `String` | Internal object id. |
| `entityUUID` | `String` | Entity UUID. |
| `externalCode` | `String` | External/business department code (key). |
| `headOfUnit` | `String` | Head-of-unit user reference. |
| `lastModifiedBy` | `String` | Last modifying user id. |
| `lastModifiedDateTime` | `ZonedDateTime` | Last modification timestamp (with timezone). |
| `lastModifiedOn` | `LocalDateTime` | Last modification datetime. |
| `mdfSystemRecordId` | `String` | MDF system record id. |
| `name` | `String` | Department name. |
| `name_defaultValue` | `String` | Default name translation value. |
| `name_en_DEBUG` | `String` | Name (`en_DEBUG`). |
| `name_en_US` | `String` | Name (`en_US`). |
| `name_localized` | `String` | Localized name value. |
| `name_nb_NO` | `String` | Name (`nb_NO`). |
| `parent` | `String` | Parent department reference. |
| `startDate` | `LocalDateTime` | Validity start (key). |
| `status` | `String` | Department status code. |

## Navigation fields

| Field | Java type | Target |
|---|---|---|
| `toCostCenterNav` | `FOCostCenter` | `costCenterNav` |
| `toCreatedByNav` | `User` | `createdByNav` |
| `toCust_DivisionNav` | `FODivision` | `cust_DivisionNav` |
| `toCust_LocationNav` | `FOLocation` | `cust_LocationNav` |
| `toCust_VirksomhetNav` | `Cust_NAVVirksomhet` | `cust_VirksomhetNav` |
| `toDescriptionTranslationNav` | `FoTranslation` | `descriptionTranslationNav` |
| `toDescriptionTranslationTextNav` | `List<MDFLocalizedValue>` | `descriptionTranslationTextNav` |
| `toHeadOfUnitNav` | `User` | `headOfUnitNav` |
| `toLastModifiedByNav` | `User` | `lastModifiedByNav` |
| `toNameTranslationNav` | `FoTranslation` | `nameTranslationNav` |
| `toNameTranslationTextNav` | `List<MDFLocalizedValue>` | `nameTranslationTextNav` |
| `toParentNav` | `FODepartment` | `parentNav` |
| `toStatusNav` | `MDFEnumValue` | `statusNav` |

