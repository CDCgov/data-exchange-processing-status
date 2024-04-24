import type { KeyValueCache } from '@apollo/utils.keyvaluecache';
import { GraphQLError } from 'graphql';

import { CosmosDataSource } from 'apollo-datasource-cosmosdb';
import { Container } from '@azure/cosmos';

import { getVerifiedToken } from './jwt.js';
import { User } from './users.js';

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
        const tokenJson = JSON.stringify(verifiedToken);
        console.log(`verified token = ${tokenJson}`);
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
        const userPermissions = JSON.parse(tokenJson) as User;
        console.log(`userId = ${userPermissions.userId}`);
    }
}
 