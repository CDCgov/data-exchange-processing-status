import { test, expect } from '@fixtures/gql';

test.describe("upsertSchema mutation", async () => { 
    test("should create a new schema", async ({ gql }) => {
        const schema = {
            schemaName: "test-schema-basic",
            schemaVersion: "1.0.0",
            content: {
                title: "Test Schema Basic",
                schema: "http://json-schema.org/draft-07/schema#",
                id: "https://github.com/cdcent/data-exchange-messages/reports/test",
                type: "object", 
                required: ["id", "name"],
                properties: {
                  id: {
                    type: "string"
                  },
                  name: {
                    type: "string"
                  }
                },
                defs: {}
            },
        }
        const response = await gql.upsertSchema({
            schemaName: schema.schemaName,
            schemaVersion: schema.schemaVersion,
            content: schema.content
        })
        expect(response.upsertSchema.result).toBe("Success")
    })  

    test("should update a schema when content is updated", async ({ gql }) => {
        const initialSchema = {
            schemaName: "test-schema-basic-update",
            schemaVersion: "1.0.0",
            content: {
                title: "Test Schema Basic Update",
                schema: "http://json-schema.org/draft-07/schema#",
                id: "https://github.com/cdcent/data-exchange-messages/reports/test",
                type: "object",
                required: ["id", "name"],
                properties: {
                    id: {
                        type: "string"
                    },
                    name: {
                        type: "string"
                    }
                },
                defs: {}
            }
        }

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
    
})
