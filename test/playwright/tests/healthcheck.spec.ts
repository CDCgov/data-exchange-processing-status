import { test, expect, request } from "@playwright/test";
import { Environments } from "fixtures/environment";

let testEnvironments = new Environments(process.env.ENV?.toString());
test.describe("Healthcheck", () => {
  testEnvironments.envs.forEach(({ environment, name, url }) => {
    test(`[${environment}] ${name}`, async ({ request }) => {
      const healthRequest = await request.get(`${url}/health`);
      expect(await healthRequest.status()).toBe(200);
      const healthResponse = await healthRequest.json();
      expect(
        JSON.stringify(healthResponse.dependencyHealthChecks)
      ).toMatchSnapshot("healthcheck.json");
    });
  });
});

test.describe("Version", () => {
  testEnvironments.envs.forEach(({ environment, name, url }) => {
    test(`[${environment}] ${name}`, async ({ request }) => {
      const version = await request.get(`${url}/version`);
      expect(await version.status()).toBe(200);
      const versionResponse = await version.json();
      expect(versionResponse.version).toBeDefined();
      expect(versionResponse.branch).toBeDefined();
      expect(versionResponse.commit).toBeDefined();
      expect(versionResponse.commitId).toBeDefined();
      expect(versionResponse.commitTime).toBeDefined();
    });
  });
});
