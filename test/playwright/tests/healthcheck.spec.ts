import { test, expect } from '@fixtures/gql';

test('healtcheck test', async ({gql}) => {
    const res = await gql.getHealth({});
    console.log(res)
    expect(res.getHealth.status).toEqual('UP')
    expect(res.getHealth.dependencyHealthChecks).toEqual(
        expect.arrayContaining([
            expect.objectContaining({ 'healthIssues': null, 'service': 'Couchbase DB', 'status': 'UP' })
        ])
    )
})
