import type { KeyValueCache } from '@apollo/utils.keyvaluecache';
import { GraphQLError } from 'graphql';

import { CosmosDataSource } from 'apollo-datasource-cosmosdb';
import { Container } from '@azure/cosmos';

import { getVerifiedToken } from './jwt.js';

export interface ReportDoc {
    id: string;
}
  
export class ReportDataSource extends CosmosDataSource<ReportDoc> {
    private token: string;

    constructor(options: { token: string; cache: KeyValueCache; cosmosContainer: Container }) {
        // super(options); // this sends our server's `cache` through
        super(options.cosmosContainer)
        this.token = options.token.replace("Bearer ", "");
        const verifiedToken = getVerifiedToken(this.token);
        console.log(`verified token = ${JSON.stringify(verifiedToken)}`);
        if (verifiedToken == null) {
            throw new GraphQLError('You are not authorized to perform this action.', {
                extensions: {
                    code: 'FORBIDDEN',
                    http: {
                        status: 403,
                    }
                },
            });
        }
    }
}
 