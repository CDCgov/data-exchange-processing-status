# Playwright Test Development TODO

## Test Coverage Improvements

### Upload Tests
- [ ] **Enhance getUploads test coverage with comprehensive data validation**
  - [ ] Create utility function using reportHelper library to inject test data into the system
  - [ ] Add assertions to validate total number of reports present in the system matches expected counts
  - [ ] Verify that API response counts accurately reflect the injected test data
  - [ ] Implement checks to ensure uploaded file data is correctly displayed in the uploads list response

### Report Query Tests
- [ ] **Implement comprehensive test suite for report query endpoints**
  - [ ] Test `reportCountWithParams` - validate filtering and counting reports with various parameter combinations
  - [ ] Test `reportCountsWithUploadId` - verify report counting functionality when filtered by specific upload IDs
  - [ ] Test `rollupCountsByStage` - ensure aggregation of reports by processing stage works correctly
  - [ ] Test `processingCounts` - validate real-time processing statistics and counts

### Deadletter Query Tests
- [ ] **Build test coverage for deadletter queue monitoring and reporting**
  - [ ] Create test data injection mechanism to simulate deadletter scenarios and generate realistic statistics
  - [ ] Test `getDeadLetterReportsByDataStream` - verify filtering deadletter reports by specific data streams
  - [ ] Test `getDeadLetterReportsByUploadId` - validate retrieval of deadletter reports for specific uploads
  - [ ] Test `getDeadLetterReportsCountByDataStream` - ensure accurate counting of deadletter reports per data stream

## CI/CD and Reporting

### GitHub Pages Integration
- [ ] **Establish automated test reporting pipeline with GitHub Pages**
  - [ ] Develop GitHub Action workflow to automatically publish test results during CI pipeline execution
  - [ ] Configure automated publishing of detailed test run results including pass/fail status and execution times
  - [ ] Set up coverage report publishing to track test coverage trends over time
  - [ ] Implement report aggregation system to maintain historical test data (retain last 5-10 reports for trend analysis)

## Code Refactoring

### Library Improvements
- [ ] **Refactor dataGenerator library for better maintainability and separation of concerns**
  - [ ] Extract report creation logic into dedicated report creation library to improve modularity
  - [ ] Implement clear separation between data generation utilities and report-specific functionality
  - [ ] Enhance code reusability by creating well-defined interfaces and reducing coupling between components

---

## Notes for Future Developers

This TODO list represents the roadmap for improving Playwright test coverage and infrastructure for the data-exchange-processing-status project. When working on these items:

1. **Test Coverage**: Focus on comprehensive validation of data flows and edge cases
2. **CI/CD**: Ensure automated reporting provides clear visibility into test health
3. **Code Quality**: Maintain clean separation between test utilities and business logic

Consider updating this list as new requirements emerge or existing items are completed.
