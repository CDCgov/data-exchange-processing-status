import { test, expect } from '@fixtures/gql';

test.describe('GraphQL getHealth', () => {
    test('returns healtheck data', async ({ gql }) => {
        const expectedDepenedencyHealthChecks = [
            {
                "healthIssues": null,
                "service": "Couchbase DB",
                "status": "UP",
                "system": "Database"
            },
            {
                "healthIssues": null,
                "service": "file_system",
                "status": "UP",
                "system": "Schema Loader"
            }
        ]
        const res = await gql.getHealth();
            
        expect(res.getHealth.status).toEqual('UP')
        expect(res.getHealth.totalChecksDuration).toBeDefined()
        expect(res.getHealth.dependencyHealthChecks).toStrictEqual(expectedDepenedencyHealthChecks)
    })

    test('returns healtheck data snapshot check', async ({ gql }) => {
        
        const res = await gql.getHealth();
            
        expect(res.getHealth.status).toEqual('UP')
        expect(res.getHealth.totalChecksDuration).toBeDefined()
        expect(JSON.stringify(res.getHealth.dependencyHealthChecks)).toMatchSnapshot("healthcheck.json")
    })
} )
