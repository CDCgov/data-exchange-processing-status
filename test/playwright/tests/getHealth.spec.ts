import { test, expect } from '@fixtures/gql';

test.describe('GraphQL getHealth', () => {
    test('returns healtheck data', async ({gql}) => {
        const res = await gql.getHealth();
            
        expect(res.getHealth.status).toEqual('UP')
        expect(res.getHealth.totalChecksDuration).toBeDefined()
        expect(res.getHealth.dependencyHealthChecks).toEqual(
            expect.arrayContaining([
                expect.objectContaining({'service': 'Couchbase DB', 'status': 'UP', 'healthIssues': null })
            ])
        )
    })
} )
