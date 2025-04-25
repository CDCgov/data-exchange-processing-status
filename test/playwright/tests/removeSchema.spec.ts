import { test, expect } from '@fixtures/gql';
import { request } from '@playwright/test';
import { getClient } from '@gql';
import removalSchema from '../fixtures/removal-schema.json';
import { GraphQLError } from 'graphql';

type GraphQLErrorResponse = { errors:GraphQLError[] };

test.describe("removeSchema mutation", async () => {
    const testSchemas = [
        { schemaName: 'removal-test', version: '1.0.0', content: removalSchema },
        { schemaName: 'removal-test', version: '2.0.0', content: removalSchema }
    ];

    test.beforeAll(async ({ gql }) => {
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
    
        const removeResponse = await gql.removeSchema({
            schemaName: schema.schemaName,
            schemaVersion: schema.version
        });
        expect(removeResponse.removeSchema.result).toBe("Success");

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

    test("should throw an error when a token is not provided", async ({  }) => {
        const apiContext = await request.newContext({
            baseURL: process.env.BASEURL,
        })
        const noTokenGQL = getClient(apiContext, { gqlEndpoint: '/graphql' });

        const errorResponse = await noTokenGQL.removeSchema({
            schemaName: "test-schema",
            schemaVersion: "1.0.0"
        }, { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;

        expect(JSON.stringify(errorResponse.errors)).toMatchSnapshot("remove-schema-no-token");
    });

    test("should throw an error when a provided token is incorrect", async ({ }) => {
        const apiContext = await request.newContext({
            baseURL: process.env.BASEURL,
            extraHTTPHeaders: {
                'Authorization': `Bearer THIS_TOKEN_IS_INCORRECT`
            }
        })
        const badTokenGQL = getClient(apiContext, { gqlEndpoint: '/graphql' });

        const errorResponse = await badTokenGQL.removeSchema({
            schemaName: "test-schema",
            schemaVersion: "1.0.0"
        }, { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;

        expect(JSON.stringify(errorResponse.errors)).toMatchSnapshot("remove-schema-bad-token");
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
