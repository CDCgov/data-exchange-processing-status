import { test, expect } from '@fixtures/gql';
import { APIRequestContext, request } from '@playwright/test';
import { getSdk } from '@gql';
import { getSdkRequester } from 'playwright-graphql';
import removalSchema from '../fixtures/removal-schema.json';
import { GraphQLError } from 'graphql';
type GraphQLErrorResponse = { errors:GraphQLError[] };

test.describe("removeSchema mutation", async () => {
    const testSchemas = [
        { schemaName: 'removal-test', version: '1.0.0', content: removalSchema },
        { schemaName: 'removal-test', version: '2.0.0', content: removalSchema }
    ];

    test.beforeAll(async ({ gql }) => {
        // Create the test schemas
        for (const schema of testSchemas) {
            await gql.upsertSchema({
                schemaName: schema.schemaName,
                schemaVersion: schema.version,
                content: schema.content
            });

            const verifyResponse = await gql.schemaContent({
                schemaName: schema.schemaName,
                schemaVersion: schema.version
            });
            expect(verifyResponse.schemaContent).toBeDefined();
        }
    });

    test.afterAll(async ({ gql }) => {
        for (const schema of testSchemas) {
            await gql.removeSchema({
                schemaName: schema.schemaName,
                schemaVersion: schema.version
            }, { failOnEmptyData: false });
        }
    });
    
    test("should successfully remove an existing schema", async ({ gql }) => {
        const schema = testSchemas[0];
        
        // Remove the schema
        const removeResponse = await gql.removeSchema({
            schemaName: schema.schemaName,
            schemaVersion: schema.version
        });
        expect(removeResponse.removeSchema.result).toBe("Success");

        // Verify the schema is gone
        const checkResponse = await gql.schemaContent({
            schemaName: schema.schemaName,
            schemaVersion: schema.version
        });
        expect(checkResponse.schemaContent).toEqual({ "failure": "fromJson(...) must not be null" });
    });

    test("should handle removal of non-existent schema gracefully", async ({ gql }) => {
        const response = await gql.removeSchema({
            schemaName: "non-existent-schema",
            schemaVersion: "1.0.0"
        }, { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;
        
        expect(JSON.stringify(response.errors)).toMatchSnapshot("remove-schema-gracefully");
    });

    test("should not remove a schema when a token is not provided", async ({ }) => {
        const getClient = (apiContext: APIRequestContext) => getSdk(getSdkRequester(apiContext, { gqlEndpoint: '/graphql' }));
        const noTokenGQL = getClient(await request.newContext());
        
        const errorResponse = await noTokenGQL.removeSchema({
            schemaName: "test-schema",
            schemaVersion: "1.0.0"
        }, { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;

        expect(JSON.stringify(errorResponse.errors)).toMatchSnapshot("remove-schema-no-token");
    });

    test("should not remove a schema when a provided token is incorrect", async ({ }) => {
        const getClient = (apiContext: APIRequestContext) => getSdk(getSdkRequester(apiContext, { gqlEndpoint: '/graphql' }));
        const badOption = {
            extraHTTPHeaders: {
                'Authorization': `Bearer THIS_TOKEN_IS_INCORRECT`
            }
        };
        const badTokenGQL = getClient(await request.newContext(badOption));
        
        const errorResponse = await badTokenGQL.removeSchema({
            schemaName: "test-schema",
            schemaVersion: "1.0.0"
        }, { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;

        expect(JSON.stringify(errorResponse.errors)).toMatchSnapshot("remove-schema-incorrect-token");
    });

    test.describe("validation failures", async () => {
        const validationTests = [
            {
                title: "empty schema name",
                schemaName: "",
                schemaVersion: "1.0.0",
                expectedResult: "Failure"
            },
            {
                title: "empty schema version",
                schemaName: "test-schema",
                schemaVersion: "",
                expectedResult: "Failure"
            },
            {
                title: "invalid schema name with spaces",
                schemaName: "test schema name",
                schemaVersion: "1.0.0",
                expectedResult: "Failure"
            },
            {
                title: "invalid schema version format",
                schemaName: "test-schema",
                schemaVersion: "1.0",
                expectedResult: "Failure"
            }
        ];

        validationTests.forEach(({title, schemaName, schemaVersion, expectedResult}) => {
            test(`should return ${expectedResult} when removing a schema - ${title}`, async ({ gql }) => {
                const response = await gql.removeSchema({
                    schemaName,
                    schemaVersion
                }, { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;
                expect(JSON.stringify(response.errors)).toMatchSnapshot("remove-schema-validation-failure");
            });
        });
    });
});
