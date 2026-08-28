# EmpEmployment entity fields

Source: `target/generated-sources/vdm/no/nav/nom/sf/odata/vdm/namespaces/successfactors/EmpEmployment.java`

This is a simple overview of the `EmpEmployment` entity fields.

## Key fields

- `personIdExternal` (`String`) - external person id.
- `userId` (`String`) - user id.

## Scalar fields

| Field | Java type | Notes |
|---|---|---|
| `StockEndDate` | `LocalDateTime` | Stock plan end date. |
| `assignmentClass` | `String` | Assignment class/code. |
| `assignmentIdExternal` | `String` | External assignment id. |
| `benefitsEndDate` | `LocalDateTime` | Benefits end date. |
| `createdBy` | `String` | User id that created the record. |
| `createdDateTime` | `ZonedDateTime` | Creation timestamp (with timezone). |
| `createdOn` | `LocalDateTime` | Creation datetime. |
| `customString16` | `String` | Custom string field 16. |
| `eligibleForSalContinuation` | `Boolean` | Salary continuation eligibility flag. |
| `employmentId` | `Long` | Internal employment id. |
| `endDate` | `LocalDateTime` | Employment end date. |
| `hiringNotCompleted` | `Boolean` | Hiring completion flag. |
| `includeAllRecords` | `Boolean` | Include all records flag. |
| `isContingentWorker` | `Boolean` | Contingent worker flag. |
| `isECRecord` | `Boolean` | Employee Central record flag. |
| `isPrimary` | `Boolean` | Primary employment flag. |
| `isSecondaryAllSfProcesses` | `Boolean` | Secondary assignment processing flag. |
| `jobNumber` | `Long` | Job number/id. |
| `lastDateWorked` | `LocalDateTime` | Last worked date. |
| `lastModifiedBy` | `String` | Last modifying user id. |
| `lastModifiedDateTime` | `ZonedDateTime` | Last modification timestamp (with timezone). |
| `lastModifiedOn` | `LocalDateTime` | Last modification datetime. |
| `okToRehire` | `Boolean` | Rehire eligibility flag. |
| `originalStartDate` | `LocalDateTime` | Original start date. |
| `payrollEndDate` | `LocalDateTime` | Payroll end date. |
| `salaryEndDate` | `LocalDateTime` | Salary end date. |
| `seniorityDate` | `LocalDateTime` | Seniority date. |
| `sourceOfRecord` | `Long` | Source-of-record identifier. |
| `startDate` | `LocalDateTime` | Employment start date. |

## Navigation fields

| Field | Java type | Target |
|---|---|---|
| `compInfoNav` | `List<EmpCompensation>` | `compInfoNav` |
| `costDistributionNav` | `List<EmpCostDistribution>` | `costDistributionNav` |
| `customString16Nav` | `PicklistOption` | `customString16Nav` |
| `empJobRelationshipNav` | `List<EmpJobRelationships>` | `empJobRelationshipNav` |
| `empPayCompNonRecurringNav` | `List<EmpPayCompNonRecurring>` | `empPayCompNonRecurringNav` |
| `jobInfoNav` | `List<EmpJob>` | `jobInfoNav` |
| `onboardingInfoNav` | `List<OnboardingInfo>` | `onboardingInfoNav` |
| `paymentInformationNav` | `List<PaymentInformationV3>` | `paymentInformationNav` |
| `personNav` | `PerPerson` | `personNav` |
| `photoNav` | `List<Photo>` | `photoNav` |
| `sourceOfRecordNav` | `PicklistOption` | `sourceOfRecordNav` |
| `userNav` | `User` | `userNav` |
| `wfRequestNav` | `WfRequest` | `wfRequestNav` |
| `workOrderNav` | `List<WorkOrder>` | `workOrderNav` |

