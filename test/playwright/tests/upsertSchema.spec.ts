import { test, expect } from '@fixtures/gql';
import { APIRequestContext, request } from '@playwright/test';
import { getSdk } from '@gql';
import { getSdkRequester } from 'playwright-graphql';

// Helper function to create a base schema with customizable parameters
const createSchema = ({
    schemaName = "test-schema-basic",
    schemaVersion = "1.0.0",
    title = "Test Schema Basic",
    properties = {
        id: { type: "string" },
        name: { type: "string" }
    },
    required = ["id", "name"]
} = {}) => {
    return {
        schemaName,
        schemaVersion,
        content: {
            title,
            schema: "http://json-schema.org/draft-07/schema#",
            id: "https://github.com/cdcent/data-exchange-messages/reports/test",
            type: "object",
            required,
            properties,
            defs: {}
        }
    };
};

test.describe("upsertSchema mutation", async () => { 
    test("should create a new schema", async ({ gql }) => {
        const schema = createSchema();
        
        const response = await gql.upsertSchema({
            schemaName: schema.schemaName,
            schemaVersion: schema.schemaVersion,
            content: schema.content
        })
        expect(response.upsertSchema.result).toBe("Success")
    })  

    test("should update a schema when content is updated", async ({ gql }) => {
        const initialSchema = createSchema({
            schemaName: "test-schema-basic-update",
            title: "Test Schema Basic Update"
        });

        const response = await gql.upsertSchema({
            schemaName: initialSchema.schemaName,
            schemaVersion: initialSchema.schemaVersion,
            content: initialSchema.content
        })
        expect(response.upsertSchema.result).toBe("Success")

        const updatedSchema = {
            schemaName: initialSchema.schemaName,
            schemaVersion: initialSchema.schemaVersion,
            content: {
                ...initialSchema.content,
                properties: {
                    ...initialSchema.content.properties,
                    description: {
                        type: "string"
                    }
                }
            }
        }

        const updateResponse = await gql.upsertSchema({
            schemaName: updatedSchema.schemaName,
            schemaVersion: updatedSchema.schemaVersion,
            content: updatedSchema.content
        })
        expect(updateResponse.upsertSchema.result).toBe("Success")

        const schemaContentResponse = await gql.schemaContent({
            schemaName: updatedSchema.schemaName,
            schemaVersion: updatedSchema.schemaVersion
        })    

        expect(schemaContentResponse.schemaContent.properties.description).toBeDefined()
        expect(schemaContentResponse.schemaContent.properties.description).toStrictEqual({type: "string"})
    })
    
    test("should not create a schema when a token is not provided", async ({ }) => {
        const getClient = (apiContext: APIRequestContext) => getSdk(getSdkRequester(apiContext, { gqlEndpoint: '/graphql' }));

        const noTokenGQL = getClient(await request.newContext())

        const schema = createSchema({
            schemaName: "test-schema-basic-no-token"
        });
        
        await expect(noTokenGQL.upsertSchema({
            schemaName: schema.schemaName,
            schemaVersion: schema.schemaVersion,
            content: schema.content
        })).rejects.toThrow(/Unauthorized: Missing or invalid bearer token/)
    })

    test("should not create a schema when a provided token is incorrect", async ({ }) => {
        const getClient = (apiContext: APIRequestContext) => getSdk(getSdkRequester(apiContext, { gqlEndpoint: '/graphql' }));

        const badOption = {
            extraHTTPHeaders: {
                'Authorization': `Bearer THIS_TOKEN_IS_INCORRECT`
            }
        }

        const badTokenGQL = getClient(await request.newContext(badOption))

        const schema = createSchema({
            schemaName: "test-schema-basic-no-token"
        });
        
        await expect(badTokenGQL.upsertSchema({
            schemaName: schema.schemaName,
            schemaVersion: schema.schemaVersion,
            content: schema.content
        })).rejects.toThrow(/Unauthorized: Missing or invalid bearer token/)
    })

    test.describe.skip("validation failures", async () => {

        const validationTests = [
            // Schema Name Tests
            {
                title: "schema name empty string",
                schemaName: "",
                schemaVersion: "1.0.0",
                expectedResult: "Failure"
            },
            {
                title: "schema name with dot",
                schemaName: "schema.2-with-dot", 
                schemaVersion: "1.0.0",
                expectedResult: "Failure"
            },
            {
                title: "schema name with spaces",
                schemaName: "test schema name with spaces",
                schemaVersion: "1.0.0",
                expectedResult: "Failure"
            },
            {
                title: "schema name with unicode characters",
                schemaName: "schema-émoji-测试-схема",
                schemaVersion: "1.0.0",
                expectedResult: "Success"
            },
            {
                title: "schema name with special characters",
                schemaName: "schema@test#special",
                schemaVersion: "1.0.0",
                expectedResult: "Success"
            },
            {
                title: "schema name with emoji characters",
                schemaName: "schema-😊-test",
                schemaVersion: "1.0.0",
                expectedResult: "Success"
            },
            {
                title: "schema name with control characters",
                schemaName: "schema\u0000test",
                schemaVersion: "1.0.0",
                expectedResult: "Failure"
            },
            {
                title: "schema name with forward slash",
                schemaName: "schema/test/name",
                schemaVersion: "1.0.0",
                expectedResult: "Failure"
            },
            {
                title: "schema name with backslash",
                schemaName: "schema\\test\\name",
                schemaVersion: "1.0.0",
                expectedResult: "Failure"
            },
            {
                title: "schema name with path traversal attempt",
                schemaName: "../schema",
                schemaVersion: "1.0.0",
                expectedResult: "Failure"
            },
            
            // Schema Version Tests
            {
                title: "schema version empty string", 
                schemaName: "schema-version-empty-string",
                schemaVersion: "",
                expectedResult: "Failure"
            },
            {
                title: "schema version multiple dots",
                schemaName: "test-schema-multi-version-dot",
                schemaVersion: "1.0.",
                expectedResult: "Failure"
            },
            {
                title: "schema version with spaces",
                schemaName: "test-schema-version-spaces",
                schemaVersion: "1 0 0",
                expectedResult: "Failure"
            },
            {
                title: "schema version with unicode characters",
                schemaName: "test-schema-unicode-version",
                schemaVersion: "1.0.测试",
                expectedResult: "Failure"
            },
            {
                title: "schema version with forward slash",
                schemaName: "test-schema-version-slash",
                schemaVersion: "1/0/0",
                expectedResult: "Failure"
            },
            {
                title: "schema version with backslash",
                schemaName: "test-schema-version-backslash",
                schemaVersion: "1\\0\\0",
                expectedResult: "Failure"
            },
        ];


        validationTests.forEach(({title, schemaName, schemaVersion, expectedResult}) => {
            test(`should return ${expectedResult} creating a schema - ${title}`, async ({ gql }) => {
                const schema = createSchema({
                    schemaName: schemaName,
                    schemaVersion: schemaVersion,
                    title: title
                }); 

                const response = await gql.upsertSchema({
                    schemaName: schema.schemaName,
                    schemaVersion: schema.schemaVersion,
                    content: schema.content
                    
                })
                expect(response.upsertSchema.result).toBe(expectedResult)

                const getSchemaResponse = await gql.schemaContent({
                    schemaName: schema.schemaName,
                    schemaVersion: schema.schemaVersion
                })

                expect(getSchemaResponse.schemaContent).toBeDefined()
            })
        })

    })
})
