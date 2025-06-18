import { test, expect } from '@fixtures/gql';
const environmentName = process.env.ENV || 'local';

test.describe("schemaLoaderInfo query", async () => {
    test(`matches the expected snapshot for enviromnent: ${environmentName}`, async ({ gql }, testInfo) => {
      const response = await gql.schemaLoaderInfo()
      expect(JSON.stringify(response.schemaLoaderInfo)).toMatchSnapshot("schemaLoaderInfo.json");
    });
});
