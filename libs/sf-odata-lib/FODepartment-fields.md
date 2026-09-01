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
| `cust_BusinessUnit` | `String` | Custom Business Unit. |
| `cust_Division` | `String` | Custom division reference. |
| `cust_LederNavIdent` | `String` | Custom Leder Nav Ident. |
| `cust_LegacyID` | `String` | Custom legacy identifier. |
| `cust_Location` | `String` | Custom location reference. |
| `cust_Tilgangsenhet` | `String` | Custom access unit reference. |
| `cust_Virksomhet` | `String` | Custom virksomhet reference. |
| `cust_nom_nivaa` | `String` | Custom NOM level reference. |
| `cust_orgenhet_type` | `String` | Custom organizational unit type reference. |
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
| `startDate` | `LocalDateTime` | Validity start (key). |
| `status` | `String` | Department status code. A/I - Active/inactive |

## Navigation fields

| Field | Java type | Target |
|---|---|---|
| `toCostCenterNav` | `FOCostCenter` | `costCenterNav` |
| `toCreatedByNav` | `User` | `createdByNav` |
| `toCust_BusinessUnitNav` | `FOBusinessUnit` | `cust_BusinessUnitNav` |
| `toCust_DivisionNav` | `FODivision` | `cust_DivisionNav` |
| `toCust_LocationNav` | `FOLocation` | `cust_LocationNav` |
| `toCust_VirksomhetNav` | `Cust_NAVVirksomhet` | `cust_VirksomhetNav` |
| `toCust_nom_nivaaNav` | `Cust_nom_nivaa` | `cust_nom_nivaaNav` |
| `toCust_orgenhet_typeNav` | `Cust_orgenhet` | `cust_orgenhet_typeNav` |
| `toHeadOfUnitNav` | `User` | `headOfUnitNav` |
| `toLastModifiedByNav` | `User` | `lastModifiedByNav` |
| `toNameTranslationNav` | `FoTranslation` | `nameTranslationNav` |
| `toNameTranslationTextNav` | `List<MDFLocalizedValue>` | `nameTranslationTextNav` |
| `toStatusNav` | `MDFEnumValue` | `statusNav` |
