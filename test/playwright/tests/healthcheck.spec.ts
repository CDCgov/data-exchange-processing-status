import { test, expect } from '@fixtures/gql';

test('healtcheck test', async ({gql}) => {
    const res = await gql.getHealth({});
    console.log(res.data.getHealth.dependencyHealthChecks)
    expect(res.data?.getHealth.status).toEqual('UP')
    expect(res.data?.getHealth.dependencyHealthChecks).toEqual(
        expect.arrayContaining([
            expect.objectContaining({ 'healthIssues': null, 'service': 'Couchbase DB', 'status': 'UP' })
        ])
    )
})
