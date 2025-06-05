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
        { schemaName: 'base', version: '1.0.0', schemaFilename: 'base.1.0.0.schema.json' },
        { schemaName: 'base', version: '0.0.1', schemaFilename: 'base.0.0.1.schema.json' },
        { schemaName: 'metadata-verify', version: '1.0.0', schemaFilename: 'metadata-verify.1.0.0.schema.json' },
        { schemaName: 'blob-file-copy', version: '1.0.0', schemaFilename: 'blob-file-copy.1.0.0.schema.json' },
        { schemaName: 'upload-started', version: '1.0.0', schemaFilename: 'upload-started.1.0.0.schema.json' },
        { schemaName: 'upload-completed', version: '1.0.0', schemaFilename: 'upload-completed.1.0.0.schema.json' },
        { schemaName: 'metadata-transform', version: '1.0.0', schemaFilename: 'metadata-transform.1.0.0.schema.json' },
        { schemaName: 'upload-status', version: '1.0.0', schemaFilename: 'upload-status.1.0.0.schema.json' },
        { schemaName: 'complex', version: '1.0.0', schemaFilename: 'complex.1.0.0.schema.json' },
        { schemaName: 'complex', version: '2.0.0', schemaFilename: 'complex.2.0.0.schema.json' }
    ];

    expectedSchemas.forEach(expectedSchema => {
        test(`matches snapshot for schema content ${expectedSchema.schemaName}/${expectedSchema.version} in ${process.env.ENV}`, async ({ gql }, testInfo) => {
            const response = await gql.schemaContent({
                schemaName: expectedSchema.schemaName,
                schemaVersion: expectedSchema.version
            });

            const schemaContentFilenameResponse = await gql.schemaContentFromFilename({
                schemaFilename: expectedSchema.schemaFilename
            });

            expect(response.schemaContent).toBeDefined();
            expect(schemaContentFilenameResponse.schemaContentFromFilename).toBeDefined();

            expect(JSON.stringify(response.schemaContent)).toMatchSnapshot(`schema-content.json`);
            expect(JSON.stringify(schemaContentFilenameResponse.schemaContentFromFilename)).toMatchSnapshot(`schema-content.json`);
        });
    });

    test(`by schema name validates the response structure for environment: ${process.env.ENV}`, async ({ gql }) => {
        const response = await gql.schemaContent({
            schemaName: 'base',
            schemaVersion: '1.0.0'
        });
        
        expect(response).toHaveProperty('schemaContent');
        expect(typeof response.schemaContent).toBe('object');
    });

    test(`by schema file name validates the response structure for environment: ${process.env.ENV}`, async ({ gql }) => {
        const response = await gql.schemaContentFromFilename({
            schemaFilename: 'base.1.0.0.schema.json'
        });
        
        expect(response).toHaveProperty('schemaContentFromFilename');
        expect(typeof response.schemaContentFromFilename).toBe('object');
    });

    test('handles invalid schema name gracefully', async ({ gql }) => {
        const response = await gql.schemaContent({
            schemaName: 'non-existent-schema',
            schemaVersion: '1.0.0'
        }, { failOnEmptyData: false });

        expect(JSON.stringify(response.errors)).toMatchSnapshot("schema-not-found-invalid-schema-name");
    });

    test('handles invalid schema version gracefully', async ({ gql }) => {
        const response = await gql.schemaContent({
            schemaName: 'base',
            schemaVersion: '999.999.999'
        }, { failOnEmptyData: false });

        expect(JSON.stringify(response.errors)).toMatchSnapshot("schema-not-found-invalid-schema-version");
    });

    test('handles invalid schema file name gracefully', async ({ gql }) => {
        const response = await gql.schemaContentFromFilename({
            schemaFilename: 'non-existent-schema.1.0.0.schema.json'
        }, { failOnEmptyData: false });

        expect(JSON.stringify(response.errors)).toMatchSnapshot("schema-not-found-invalid-schema-name-filename");
    });

    test('handles invalid schema file name version gracefully', async ({ gql }) => {
        const response = await gql.schemaContentFromFilename({
            schemaFilename: 'base.999.schema.json'
        }, { failOnEmptyData: false });

        expect(JSON.stringify(response.errors)).toMatchSnapshot("schema-not-found-invalid-schema-version-filename");
    });
});
