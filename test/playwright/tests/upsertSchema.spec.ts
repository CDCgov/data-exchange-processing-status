import { test, expect, GraphQLErrorResponse} from '@fixtures/gql';
import { APIRequestContext, request } from '@playwright/test';
import { getSdk } from '@gql';
import { getSdkRequester } from 'playwright-graphql';

test.describe("upsertSchema mutation", async () => { 
    test("should create a new schema", async ({ gql, schemaHelper }) => {
        const schema = schemaHelper.createSchema();
        
        const response = await gql.upsertSchema({
            schemaName: schema.schemaName,
            schemaVersion: schema.schemaVersion,
            content: schema.content
        })
        expect(response.upsertSchema.result).toBe("Success")
    })  

    test("should update a schema when content is updated", async ({ gql, schemaHelper }) => {
        const initialSchema = schemaHelper.createSchema({
            schemaName: "test-schema-basic-update",
            title: "Test Schema Basic Update"
        });

        await schemaHelper.upsertAndValidate(initialSchema);

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

        await schemaHelper.upsertAndValidate(updatedSchema);

        const schemaContentResponse = await schemaHelper.validateSchemaContentResponse(updatedSchema);
        expect(schemaContentResponse.schemaContent.properties.description).toStrictEqual({type: "string"})
    })
    
    test("should not create a schema when a token is not provided", async ({ schemaHelper }) => {
        const getClient = (apiContext: APIRequestContext) => getSdk(getSdkRequester(apiContext, { gqlEndpoint: '/graphql' }));

        const noTokenGQL = getClient(await request.newContext())

        const schema = schemaHelper.createSchema({
            schemaName: "test-schema-basic-no-token"
        });
        
        await expect(noTokenGQL.upsertSchema({
            schemaName: schema.schemaName,
            schemaVersion: schema.schemaVersion,
            content: schema.content
        })).rejects.toThrow(/Unauthorized: Missing or invalid bearer token/)
    })

    test("should not create a schema when a provided token is incorrect", async ({ schemaHelper }) => {
        const getClient = (apiContext: APIRequestContext) => getSdk(getSdkRequester(apiContext, { gqlEndpoint: '/graphql' }));

        const badOption = {
            extraHTTPHeaders: {
                'Authorization': `Bearer THIS_TOKEN_IS_INCORRECT`
            }
        }

        const badTokenGQL = getClient(await request.newContext(badOption))

        const schema = schemaHelper.createSchema({
            schemaName: "test-schema-basic-no-token"
        });
        
        await expect(badTokenGQL.upsertSchema({
            schemaName: schema.schemaName,
            schemaVersion: schema.schemaVersion,
            content: schema.content
        })).rejects.toThrow(/Unauthorized: Missing or invalid bearer token/)
    })

    test.describe("validation failures", async () => {

        const successValidationTests = [
            // Schema Name Tests
            {
                title: "schema name with unicode characters",
                schemaName: "schema-émoji-测试-схема",
                schemaVersion: "1.0.0"
            },
            {
                title: "schema name with special characters",
                schemaName: "schema@test#special",
                schemaVersion: "1.0.0"
            },
            {
                title: "schema name with emoji characters",
                schemaName: "schema-😊-test",
                schemaVersion: "1.0.0"
            }
        ];

        const failureValidationTests = [
            // Schema Name Tests
            {
                title: "schema name empty string",
                schemaName: "",
                schemaVersion: "1.0.0"
            },
            {
                title: "schema name with dot",
                schemaName: "schema.2-with-dot",
                schemaVersion: "1.0.0"
            },
            {
                title: "schema name with spaces",
                schemaName: "test schema name with spaces",
                schemaVersion: "1.0.0"
            },
            {
                title: "schema name with control characters",
                schemaName: "schema\u0000test",
                schemaVersion: "1.0.0"
            },
            {
                title: "schema name with forward slash",
                schemaName: "schema/test/name",
                schemaVersion: "1.0.0"
            },
            {
                title: "schema name with backslash",
                schemaName: "schema\\test\\name",
                schemaVersion: "1.0.0"
            },
            {
                title: "schema name with path traversal attempt",
                schemaName: "../schema",
                schemaVersion: "1.0.0"
            },

            // Schema Version Tests
            {
                title: "schema version empty string",
                schemaName: "schema-version-empty-string",
                schemaVersion: ""
            },
            {
                title: "schema version ends with dot",
                schemaName: "test-schema-multi-version-dot",
                schemaVersion: "1.0."
            },
            {
                title: "schema version with spaces",
                schemaName: "test-schema-version-spaces",
                schemaVersion: "1 0 0"
            },
            {
                title: "schema version with forward slash",
                schemaName: "test-schema-version-slash",
                schemaVersion: "1/0/0"
            },
            {
                title: "schema version with backslash",
                schemaName: "test-schema-version-backslash",
                schemaVersion: "1\\0\\0"
            },
            {
                title: "schema version with invalid punctuation",
                schemaName: "test-schema-invalid-punctuation",
                schemaVersion: "1.0.\u200B"
            },
            {
                title: "schema version with emoji characters",
                schemaName: "test-schema-emoji-version",
                schemaVersion: "1.0.⛔"
            },
            {
                title: "schema version with text",
                schemaName: "test-schema-emoji-version",
                schemaVersion: "a.b.c"
            },
            {
                title: "schema version with too many dots",
                schemaName: "test-schema-too-many-dots-version",
                schemaVersion: "1.2.3.4"
            },
            {
                title: "schema version with too few dots",
                schemaName: "test-schema-too-few-dots-version",
                schemaVersion: "1.2"
            }
        ];

        successValidationTests.forEach(({title, schemaName, schemaVersion}) => {
            test(`should return Success when creating a schema - ${title}`, async ({ gql, schemaHelper }) => {
                const schema = schemaHelper.createSchema({
                    schemaName: schemaName,
                    schemaVersion: schemaVersion,
                    title: title
                });
                await schemaHelper.upsertAndValidate(schema);
                await schemaHelper.validateSchemaContentResponse(schema);
            })
        })

        failureValidationTests.forEach(({title, schemaName, schemaVersion}) => {
            test(`should return error when creating a schema - ${title}`, async ({ gql, schemaHelper }) => {
                const schema = schemaHelper.createSchema({
                    schemaName: schemaName,
                    schemaVersion: schemaVersion,
                    title: title
                });

                const response = await gql.upsertSchema({
                    schemaName: schema.schemaName,
                    schemaVersion: schema.schemaVersion,
                    content: schema.content
                }, { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;

                expect(JSON.stringify(response.errors)).toMatchSnapshot("schema-upsert-failed");

                const getSchemaResponse = await gql.schemaContent({
                    schemaName: schema.schemaName,
                    schemaVersion: schema.schemaVersion
                }, { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;

                expect(JSON.stringify(getSchemaResponse.errors)).toMatchSnapshot("schema-get-content-failed");
            })
        })
    })
})
