import { defineConfig } from '@playwright/test';
import dotenv from 'dotenv';
import path from 'path';
dotenv.config({ path: path.resolve(__dirname, '.env') });

export default defineConfig({
  // Look for test files in the "tests" directory, relative to this configuration file.
  testDir: 'tests',

  // Run all tests in parallel.
  fullyParallel: true,

  // Fail the build on CI if you accidentally left test.only in the source code.
  forbidOnly: !!process.env.CI,

  // Retry on CI only.
  retries: process.env.CI ? 2 : 0,

  // Opt out of parallel tests on CI.
  workers: process.env.CI ? 1 : undefined,

  // Reporter to use
  reporter: [
    ['list', { printSteps: true }],
    ['html', { open: 'never' }],
    ['playwright-graphql/coverage-reporter', {
      graphqlFilePath: path.resolve(__dirname, 'gql/graphql.ts'), 
      coverageFilePath: path.resolve(__dirname, 'coverage/gql-coverage.log'),
      htmlFilePath: path.resolve(__dirname, 'coverage/gql-coverage.html'),
      logUncoveredOperations: false,
      saveGqlCoverageLog: true,
      saveHtmlSummary: true
  }]
  ],

  snapshotPathTemplate: `{testDir}/__snapshot__/{testFileName}/{testName}-{arg}{ext}`,
  
  use: {
    // Collect trace when retrying the failed test.
    trace: 'on-first-retry',
    ignoreHTTPSErrors: true,
  },
  // Configure projects for major browsers.
  projects: [
    {
      name: 'GQL',
          use: { 
            baseURL: process.env.BASEURL
      },
    },
  ],

});
