# Processing Status - Playwright-Graphql Tests

## Overview

This project is part of the CDC's Public Health Data Operations (PHDO) Processing Status (PS) API initiative. It includes automated tests using Playwright to validate GraphQL endpoints. The tests ensure reliable data processing and integration for various data exchange workflows, focusing on both functional and integration-level validations. Key features include schema generation and code generation and customized playwright utilities for GQL query execution.

This project is designed to be run against the GraphQL endpoint of PS API and by default points to a locally running version based on the instruction on setting up PS API from its [README](../../README.md).

## Technologies

The primary technologies and tools used in this project are:

- [Playwright](https://playwright.dev/) - End-to-end testing framework.
- [playwright-graphql](https://www.npmjs.com/package/playwright-graphql) - Utility for GraphQL testing and schema/code generation that includes the following tools packaged together: 
  - [get-graphql-schema](https://www.npmjs.com/package/get-graphql-schema) - Fetches the full GraphQL schema via HTTP endpoint.
  - [gqlg](https://www.npmjs.com/package/gqlg) - Generates queries from fetched GraphQL schemas.
  - [graphql-codegen](https://www.npmjs.com/package/@graphql-codegen/cli) - Generates TypeScript interfaces for queries for use in Playwright.
- [TypeScript](https://www.typescriptlang.org/) - Typed superset of JavaScript for better maintainability.
- [MailHog](https://github.com/mailhog/MailHog) - Local smtp mock email server
- [webhook.site](https://github.com/webhooksite/webhook.site) - Webhook listener for testing

## Setup and Installation

### Installing  dependencies
   ```bash
   npm install
   ```
### Configuring the environment

A default `.env` file is provided in `/test/playwright` with the following configuration parameters for use with the default settings when running locally in a docker container (as detailed below)

| Parameter | Description | Default Value |
|-----------|-------------|---------------|
| `ENV` | Environment name for test execution | `local` |
| `BASEURL` | GraphQL endpoint URL | `http://127.0.0.1:8090/graphql` |
| `EMAILURL` | Email service URL for notifications | `http://127.0.0.1:8025` |
| `WEBHOOKURL` | Webhook service URL | `http://webhook:80` |
| `WEBHOOKAPI` | Webhook API endpoint | `http://localhost:8084` |
| `NODE_TLS_REJECT_UNAUTHORIZED` | SSL/TLS verification setting | `0` |
| `GRAPHQL_AUTH_TOKEN` | Authentication token for GraphQL requests | `LET_ME_IN_PLS_THANKS` |

To configure the test environment:

**Schema Source Configuration**:
   - Edit the BASEURL in the `.env` file to point to your test environment

### Setting up local docker configuration

1. Run the base Processing Status API (from project base directory)
```bash
docker compose up -d
```

2. Run the Processing Status Notitfications API (with mock services .env) (from project base directory)
```bash
docker compose -f docker-compose.notifications.yml --env-file mock-email.env up -d
```

3. Run the Mock Services for Tests (from project base directory)
```bash
docker compose -f docker-compose.test-mocks.yml up -d
```

## Usage

To setup and run the tests: 

1. **Run the code generation script**:
   This script will generate the required GraphQL schema, operations, and TypeScript types.
   ```bash
   npm run codegen
   ```

2. **Run the tests**:
   Once the code generation is complete, execute the Playwright tests.
   ```bash
   npm run test
   ```

## NPM Scripts

| Script                        | Description                                                         |
|-------------------------------|---------------------------------------------------------------------|
| `npm run codegen`             | Run all code generation scripts (schema, operations, and types).    |
| `npm run test`                | Run all Playwright tests.                                           |


