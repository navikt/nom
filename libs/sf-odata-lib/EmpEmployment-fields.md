# EmpEmployment entity fields

Source: `target/generated-sources/vdm/no/nav/nom/sf/odata/vdm/namespaces/successfactors/EmpEmployment.java`

This is a simple overview of the `EmpEmployment` entity fields.

## Key fields

- `personIdExternal` (`String`) - external person id.
- `userId` (`String`) - user id.

## Scalar fields

| Field | Java type | Notes |
|---|---|---|
| `stockEndDate` | `LocalDateTime` | Stock plan end date. |
| `assignmentClass` | `String` | Assignment class/code. |
| `assignmentIdExternal` | `String` | External assignment id. |
| `benefitsEndDate` | `LocalDateTime` | Benefits end date. |
| `createdBy` | `String` | User id that created the record. |
| `createdDateTime` | `ZonedDateTime` | Creation timestamp (with timezone). |
| `createdOn` | `LocalDateTime` | Creation datetime. |
| `customDate1` | `LocalDateTime` | Custom date field 1. |
| `customString1` | `String` | Custom string field 1. |
| `customString16` | `String` | Custom string field 16. |
| `eligibleForSalContinuation` | `Boolean` | Salary continuation eligibility flag. |
| `employmentId` | `Long` | Internal employment id. |
| `endDate` | `LocalDateTime` | Employment end date. |
| `hiringNotCompleted` | `Boolean` | Hiring completion flag. |
| `includeAllRecords` | `Boolean` | Include all records flag. |
| `isContingentWorker` | `Boolean` | Contingent worker flag. |
| `isECRecord` | `Boolean` | Employee Central record flag. |
| `isSecondaryAllSfProcesses` | `Boolean` | Secondary assignment processing flag. |
| `jobNumber` | `Long` | Job number/id. |
| `lastDateWorked` | `LocalDateTime` | Last worked date. |
| `lastModifiedBy` | `String` | Last modifying user id. |
| `lastModifiedDateTime` | `ZonedDateTime` | Last modification timestamp (with timezone). |
| `lastModifiedOn` | `LocalDateTime` | Last modification datetime. |
| `okToRehire` | `Boolean` | Rehire eligibility flag. |
| `originalStartDate` | `LocalDateTime` | Original start date. |
| `payrollEndDate` | `LocalDateTime` | Payroll end date. |
| `personIdExternal` | `String` | External person id (key). |
| `salaryEndDate` | `LocalDateTime` | Salary end date. |
| `seniorityDate` | `LocalDateTime` | Seniority date. |
| `sourceOfRecord` | `Long` | Source-of-record identifier. |
| `startDate` | `LocalDateTime` | Employment start date. |
| `userId` | `String` | User id (key). |

## Navigation fields

| Field | Java type | Target |
|---|---|---|
| `toCompInfoNav` | `List<EmpCompensation>` | `compInfoNav` |
| `toCostDistributionNav` | `List<EmpCostDistribution>` | `costDistributionNav` |
| `toCustomString16Nav` | `PicklistOption` | `customString16Nav` |
| `toCustomString1Nav` | `PicklistOption` | `customString1Nav` |
| `toEmpJobRelationshipNav` | `List<EmpJobRelationships>` | `empJobRelationshipNav` |
| `toEmpPayCompNonRecurringNav` | `List<EmpPayCompNonRecurring>` | `empPayCompNonRecurringNav` |
| `toJobInfoNav` | `List<EmpJob>` | `jobInfoNav` |
| `toOnboardingInfoNav` | `List<OnboardingInfo>` | `onboardingInfoNav` |
| `toPaymentInformationNav` | `List<PaymentInformationV3>` | `paymentInformationNav` |
| `toPersonNav` | `PerPerson` | `personNav` |
| `toPhotoNav` | `List<Photo>` | `photoNav` |
| `toSourceOfRecordNav` | `PicklistOption` | `sourceOfRecordNav` |
| `toUserNav` | `User` | `userNav` |
| `toWfRequestNav` | `WfRequest` | `wfRequestNav` |
| `toWorkOrderNav` | `List<WorkOrder>` | `workOrderNav` |
