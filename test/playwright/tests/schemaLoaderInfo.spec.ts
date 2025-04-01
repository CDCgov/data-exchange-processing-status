import { test, expect } from '@fixtures/gql';

test.describe("schemaLoaderInfo query", async () => {
    test(`matches the expected snapshot for enviromnent: ${process.env.ENV}`, async ({ gql }, testInfo) => {
      const response = await gql.schemaLoaderInfo()
      expect(JSON.stringify(response.schemaLoaderInfo)).toMatchSnapshot("schemaLoaderInfo.json");
    });
});
