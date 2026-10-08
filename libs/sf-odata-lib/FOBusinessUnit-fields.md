# FOBusinessUnit entity fields

Source: `target/generated-sources/vdm/no/nav/nom/sf/odata/vdm/namespaces/successfactors/FOBusinessUnit.java`

This is a simple overview of the `FOBusinessUnit` entity fields.

## Key fields

- `externalCode` (`String`) - external/business business unit code.
- `startDate` (`LocalDateTime`) - start of validity period.

## Scalar fields

| Field | Java type | Notes |
|---|---|---|
| `createdBy` | `String` | User id that created the record. |
| `createdDateTime` | `ZonedDateTime` | Creation timestamp (with timezone). |
| `createdOn` | `LocalDateTime` | Creation datetime. |
| `cust_LegacyID` | `String` | Custom legacy identifier. |
| `cust_LegalEntity` | `String` | Custom legal entity (company) reference. |
| `endDate` | `LocalDateTime` | End of validity period. |
| `entityOID` | `String` | Internal object id. |
| `entityUUID` | `String` | Entity UUID. |
| `externalCode` | `String` | External/business business unit code (key). |
| `headOfUnit` | `String` | Head-of-unit user reference. |
| `lastModifiedBy` | `String` | Last modifying user id. |
| `lastModifiedDateTime` | `ZonedDateTime` | Last modification timestamp (with timezone). |
| `lastModifiedOn` | `LocalDateTime` | Last modification datetime. |
| `mdfSystemRecordId` | `String` | MDF system record id. |
| `name` | `String` | Business unit name. |
| `startDate` | `LocalDateTime` | Validity start (key). |
| `status` | `String` | Business unit status code.  A/I - Active/inactive |

## Navigation fields

| Field | Java type | Target |
|---|---|---|
| `toCreatedByNav` | `User` | `createdByNav` |
| `toCust_LegalEntityNav` | `FOCompany` | `cust_LegalEntityNav` |
| `toHeadOfUnitNav` | `User` | `headOfUnitNav` |
| `toLastModifiedByNav` | `User` | `lastModifiedByNav` |
| `toNameTranslationNav` | `FoTranslation` | `nameTranslationNav` |
| `toNameTranslationTextNav` | `List<MDFLocalizedValue>` | `nameTranslationTextNav` |
| `toStatusNav` | `MDFEnumValue` | `statusNav` |
