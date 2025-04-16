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

## Setup and Installation

### Installing  dependencies
   ```bash
   npm install
   ```
### Configuring the environment
Configure where to get the schema from
-  Edit the `package.json` `generate:schema` script to point to the environment where you want to get the schemas from

Configure where to point the tests
- Edit the `playwright.config.ts` file and update the `baseURL` in the project to define the URL where tests should be run against

## Usage

To use this project, follow these steps:

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

## Scripts

List key npm scripts and their descriptions:

| Script                        | Description                                                         |
|-------------------------------|---------------------------------------------------------------------|
| `npm run generate:schema`     | Fetch the GraphQL schema from the endpoint and save it locally.     |
| `npm run generate:operations` | Generate GraphQL queries and mutations from the schema.             |
| `npm run generate:types`      | Generate TypeScript types for the GraphQL schema.                   |
| `npm run codegen`             | Run all code generation scripts (schema, operations, and types).    |
| `npm run test`                | Run all Playwright tests.                                           |


