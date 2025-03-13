import { test, expect, request} from '@playwright/test';

type Environment = {
    environment: string;
    name: string;
    url: string;
}

const environments: [Environment] = JSON.parse(JSON.stringify(require('fixtures/environments.json')))
const environment = environments.filter((env: any) => {return env.environment === process.env.ENV?.toString()})

test.describe('Healthcheck', () => {
    environment.forEach(({ environment, name, url }) => {
        test(`[${environment}] ${name}@${url}`, async ({ request }) => {
            const healthRequest = await request.get(`${url}/health`)
            expect(await healthRequest.ok()).toBeTruthy()
            const healthResponse = await healthRequest.json()
            expect(JSON.stringify(healthResponse.dependencyHealthChecks)).toMatchSnapshot("healthcheck.json")
        })
    })
})

test.describe('Version', () => {
    environment.forEach(({ environment, name, url }) => {
        test(`[${environment}] ${name}@${url}`, async ({ request }) => {
            const version = await request.get(`${url}/version`)
            expect(await version.ok()).toBeTruthy()
            const versionResponse = await version.json()
            expect(versionResponse.version).toBeDefined()
            expect(versionResponse.branch).toBeDefined()
            expect(versionResponse.commit).toBeDefined()
            expect(versionResponse.commitId).toBeDefined()
            expect(versionResponse.commitTime).toBeDefined()
        })
    })
})
