import { defineConfig } from '@playwright/test';

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
  reporter: [['list', { printSteps: true }],['html']],

  use: {
    // Collect trace when retrying the failed test.
    trace: 'on-first-retry',
    ignoreHTTPSErrors: true,
  },
  // Configure projects for major browsers.
  projects: [
    {
      name: 'GQL-Local',
          use: { 
          baseURL: 'http://127.0.0.1:8090/graphql'
      },
    },
    {
      name: 'GQL-Dev',
      use: {
            baseURL: 'https://pstatusgraphql.ocio-eks-dev-ede.cdc.gov/graphql'
          }
    },
    {
      name: 'GQL-Test',
      use: {
            baseURL: 'https://pstatusgraphql.phdo-eks-test.cdc.gov/graphql'
          }
    },
    {
      name: 'GQL-Stage',
      use: {
            baseURL: 'https://pstatusgraphql.phdo-eks-test.cdc.gov/graphql'
          }
    }
  ],

});
