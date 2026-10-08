# Cust_ARBOMR entity fields

Source: `target/generated-sources/vdm/no/nav/nom/sf/odata/vdm/namespaces/successfactors/Cust_ARBOMR.java`

This is a simple overview of the `Cust_ARBOMR` entity fields.

## Key fields

- `effectiveStartDate` (`LocalDateTime`) - start of validity period.
- `externalCode` (`String`) - external/business work area code.

## Scalar fields

| Field | Java type | Notes |
|---|---|---|
| `createdBy` | `String` | User id that created the record. |
| `createdDateTime` | `ZonedDateTime` | Creation timestamp (with timezone). |
| `cust_Description` | `String` | Custom description of the work area. |
| `cust_Division` | `String` | Custom division reference. |
| `cust_HeadofWorkArea` | `String` | Head-of-work-area user reference. |
| `cust_LegacyID` | `String` | Custom legacy identifier. |
| `cust_ParentWorkArea` | `String` | Parent work area reference. |
| `effectiveStartDate` | `LocalDateTime` | Validity start (key). |
| `externalCode` | `String` | External/business work area code (key). |
| `externalName` | `String` | Work area external name. |
| `lastModifiedBy` | `String` | Last modifying user id. |
| `lastModifiedDateTime` | `ZonedDateTime` | Last modification timestamp (with timezone). |
| `mdfSystemEffectiveEndDate` | `LocalDateTime` | MDF system effective end date. |
| `mdfSystemRecordStatus` | `String` | MDF system record status code. |
| `mdfSystemStatus` | `String` | MDF system status code. |

## Navigation fields

| Field | Java type | Target |
|---|---|---|
| `toCreatedByNav` | `User` | `createdByNav` |
| `toCust_DivisionNav` | `FODivision` | `cust_DivisionNav` |
| `toCust_HeadofWorkAreaNav` | `User` | `cust_HeadofWorkAreaNav` |
| `toCust_ParentWorkAreaNav` | `Cust_ARBOMR` | `cust_ParentWorkAreaNav` |
| `toLastModifiedByNav` | `User` | `lastModifiedByNav` |
| `toMdfSystemRecordStatusNav` | `MDFEnumValue` | `mdfSystemRecordStatusNav` |
| `toMdfSystemStatusNav` | `MDFEnumValue` | `mdfSystemStatusNav` |
| `toWfRequestNav` | `List<WfRequest>` | `wfRequestNav` |
