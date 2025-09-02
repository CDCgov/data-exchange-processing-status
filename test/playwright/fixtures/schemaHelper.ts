import { test as base } from '@playwright/test';
import { expect } from '@playwright/test';
import { GraphQLErrorResponse } from '@fixtures/gql';

export class SchemaHelper {
    constructor(private readonly gql: any) {}
    
    createSchema = ({
        schemaName = "test-schema-basic",
        schemaVersion = "1.0.0", 
        title = "Test Schema Basic",
        properties = {
            id: { type: "string" },
            name: { type: "string" }
        },
        required = ["id", "name"]
    } = {}) => {
        const schemaContent = {
            title,
            schema: "http://json-schema.org/draft-07/schema#",
            id: "https://github.com/cdcent/data-exchange-messages/reports/test",
            type: "object",
            required,
            properties,
            defs: {}
        };

        return {
            schemaName,
            schemaVersion,
            content: schemaContent
        };
    };

    async upsertAndValidate(schema: { schemaName: string; schemaVersion: string; content: any }) {
        const response = await this.gql.upsertSchema({
            schemaName: schema.schemaName,
            schemaVersion: schema.schemaVersion,
            content: schema.content
        });
        
        expect(response.upsertSchema.result).toBe("Success");
        return response;
    }

    async validateSchemaContentResponse(schema: { schemaName: string; schemaVersion: string; content: any }) {
        let lastResponse;
        await expect.poll(
            async () => {
                lastResponse = await this.gql.schemaContent({
                    schemaName: schema.schemaName,
                    schemaVersion: schema.schemaVersion
                });
                return lastResponse.schemaContent;
            },
            {
                timeout: 10_000,
                intervals: [500]
            }
        ).toBeDefined();
        return lastResponse;
    }

    async removeSchemaAndMatchSnapshot(schema: { schemaName: string; schemaVersion: string }, snapshotName: string) {
        const checkResponse = await this.gql.removeSchema({
            schemaName: schema.schemaName,
            schemaVersion: schema.schemaVersion
        }, { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;
        
        expect(JSON.stringify(checkResponse.errors)).toMatchSnapshot(snapshotName);
        return checkResponse;
    }
}


