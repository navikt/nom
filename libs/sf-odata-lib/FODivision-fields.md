# FODivision entity fields

Source: `target/generated-sources/vdm/no/nav/nom/sf/odata/vdm/namespaces/successfactors/FODivision.java`

This is a simple overview of the `FODivision` entity fields.

## Key fields

- `externalCode` (`String`) - external/business division code.
- `startDate` (`LocalDateTime`) - start of validity period.

## Scalar fields

| Field | Java type | Notes |
|---|---|---|
| `createdBy` | `String` | User id that created the record. |
| `createdDateTime` | `ZonedDateTime` | Creation timestamp (with timezone). |
| `createdOn` | `LocalDateTime` | Creation datetime. |
| `cust_BusinessUnit` | `String` | Custom business unit reference. |
| `cust_LegacyID` | `String` | Custom legacy identifier. |
| `cust_Tilgangsenhet` | `String` | Custom access unit reference. |
| `cust_leder_navident` | `String` | Custom manager Nav identifier. |
| `cust_nom_nivaa` | `String` | Custom NOM level reference. |
| `cust_orgenhet_type` | `String` | Custom organizational unit type reference. |
| `description` | `String` | Division description. |
| `endDate` | `LocalDateTime` | End of validity period. |
| `entityOID` | `String` | Internal object id. |
| `entityUUID` | `String` | Entity UUID. |
| `externalCode` | `String` | External/business division code (key). |
| `headOfUnit` | `String` | Head-of-unit user reference. |
| `lastModifiedBy` | `String` | Last modifying user id. |
| `lastModifiedDateTime` | `ZonedDateTime` | Last modification timestamp (with timezone). |
| `lastModifiedOn` | `LocalDateTime` | Last modification datetime. |
| `mdfSystemRecordId` | `String` | MDF system record id. |
| `name` | `String` | Division name. |
| `parent` | `String` | Parent division reference. |
| `startDate` | `LocalDateTime` | Validity start (key). |
| `status` | `String` | Status. |

## Navigation fields

| Field | Java type | Target |
|---|---|---|
| `toCreatedByNav` | `User` | `createdByNav` |
| `toCust_BusinessUnitNav` | `FOBusinessUnit` | `cust_BusinessUnitNav` |
| `toCust_nom_nivaaNav` | `Cust_nom_nivaa` | `cust_nom_nivaaNav` |
| `toCust_orgenhet_typeNav` | `Cust_orgenhet` | `cust_orgenhet_typeNav` |
| `toDescriptionTranslationNav` | `FoTranslation` | `descriptionTranslationNav` |
| `toDescriptionTranslationTextNav` | `List<MDFLocalizedValue>` | `descriptionTranslationTextNav` |
| `toHeadOfUnitNav` | `User` | `headOfUnitNav` |
| `toLastModifiedByNav` | `User` | `lastModifiedByNav` |
| `toNameTranslationNav` | `FoTranslation` | `nameTranslationNav` |
| `toNameTranslationTextNav` | `List<MDFLocalizedValue>` | `nameTranslationTextNav` |
| `toParentNav` | `FODivision` | `parentNav` |
| `toStatusNav` | `MDFEnumValue` | `statusNav` |
