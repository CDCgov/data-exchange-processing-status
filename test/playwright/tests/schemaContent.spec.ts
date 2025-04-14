import { test, expect } from '@fixtures/gql';
import complexSchema from '../fixtures/complex-schema.json';

test.describe("schemaContent query", async () => {
    test.beforeAll(async ({ gql }) => {
        [
            { schemaName: 'complex', version: '1.0.0', content: complexSchema },
            { schemaName: 'complex', version: '2.0.0', content: complexSchema }
        ].forEach(async (schema) => {
            await gql.upsertSchema({
                schemaName: schema.schemaName,
                schemaVersion: schema.version,
                content: schema.content
            });
        });
    });
    
    const expectedSchemas = [
        { schemaName: 'base', version: '1.0.0' },
        { schemaName: 'base', version: '0.0.1' },
        { schemaName: 'metadata-verify', version: '1.0.0' },
        { schemaName: 'blob-file-copy', version: '1.0.0' },
        { schemaName: 'upload-started', version: '1.0.0' },
        { schemaName: 'upload-completed', version: '1.0.0' },
        { schemaName: 'metadata-transform', version: '1.0.0' },
        { schemaName: 'upload-status', version: '1.0.0' },
        { schemaName: 'complex', version: '1.0.0' },
        { schemaName: 'complex', version: '2.0.0' }
    ];

    expectedSchemas.forEach(expectedSchema => {
        test(`matches snapshot for schema content ${expectedSchema.schemaName}/${expectedSchema.version} in ${process.env.ENV}`, async ({ gql }, testInfo) => {
            const response = await gql.schemaContent({
                schemaName: expectedSchema.schemaName,
                schemaVersion: expectedSchema.version
            });
            
            expect(response.schemaContent).toBeDefined();
            expect(JSON.stringify(response.schemaContent)).toMatchSnapshot(`schema-content.json`);
        });
    });

    test(`validates the response structure for environment: ${process.env.ENV}`, async ({ gql }) => {
        const response = await gql.schemaContent({
            schemaName: 'base',
            schemaVersion: '1.0.0'
        });
        
        expect(response).toHaveProperty('schemaContent');
        expect(typeof response.schemaContent).toBe('object');
    });

    test('handles invalid schema name gracefully', async ({ gql }) => {
        const response = await gql.schemaContent({
            schemaName: 'non-existent-schema',
            schemaVersion: '1.0.0'
        });

        expect(response.schemaContent).toBeNull();
    });

    test('handles invalid schema version gracefully', async ({ gql }) => {
        const response = await gql.schemaContent({
            schemaName: 'base',
            schemaVersion: '999.999.999'
        });

        expect(response.schemaContent).toBeNull();
    });
});
