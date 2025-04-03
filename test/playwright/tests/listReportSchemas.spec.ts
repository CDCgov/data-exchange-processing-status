import { test, expect } from '@fixtures/gql';

test.describe("listReportSchemas query", async () => {
  const expectedSchemas = [
    { schemaName: 'base', version: '1.0.0' },
    { schemaName: 'base', version: '0.0.1' },
    { schemaName: 'metadata-verify', version: '1.0.0' },
    { schemaName: 'blob-file-copy', version: '1.0.0' },
    { schemaName: 'upload-started', version: '1.0.0' },
    { schemaName: 'upload-completed', version: '1.0.0' },
    { schemaName: 'metadata-transform', version: '1.0.0' },
    { schemaName: 'upload-status', version: '1.0.0' }
  ];
    
  expectedSchemas.forEach(expectedSchema => {
    test(`matches snapshot for schema ${expectedSchema.schemaName}/${expectedSchema.version} in ${process.env.ENV}`, async ({ gql }, testInfo) => {
      const response = await gql.listReportSchemas();
      const schema = response.listReportSchemas.find(
        schema => schema.schemaName === expectedSchema.schemaName && 
        schema.schemaVersion === expectedSchema.version
      );
      expect(JSON.stringify(schema)).toMatchSnapshot(`schema.json`);
    });
  });

  test(`validates the response structure for environment: ${process.env.ENV}`, async ({ gql }) => {
    const response = await gql.listReportSchemas();
    
    expect(Array.isArray(response.listReportSchemas)).toBe(true);
    
    if (response.listReportSchemas.length > 0) {
      const schema = response.listReportSchemas[0];
      
      expect(schema).toHaveProperty('description');
      expect(schema).toHaveProperty('filename');
      expect(schema).toHaveProperty('schemaName');
      expect(schema).toHaveProperty('schemaVersion');
      
      expect(typeof schema.description).toBe('string');
      expect(typeof schema.filename).toBe('string');
      expect(typeof schema.schemaName).toBe('string');
      expect(typeof schema.schemaVersion).toBe('string');
    }
  });
});
