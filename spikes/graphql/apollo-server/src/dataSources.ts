import type { KeyValueCache } from '@apollo/utils.keyvaluecache';
import { GraphQLError } from 'graphql';

import { CosmosDataSource } from 'apollo-datasource-cosmosdb';
import { Container } from '@azure/cosmos';

import jsonwebtoken from 'jsonwebtoken';

import dotenv from 'dotenv';

dotenv.config();

const JWT_SECRET = process.env.ACCESS_TOKEN_SECRET!;

export interface ReportDoc {
    id: string;
}
  
const getUser = (token: string) => {
    try {
        if (token) {
          console.log("incoming token = " + token);
          console.log("secret = " + JWT_SECRET);
          return jsonwebtoken.verify(token, JWT_SECRET)
        }
        return null
    } catch (error) {
        return null
    }
}
  
export class ReportDataSource extends CosmosDataSource<ReportDoc> {
    private token: string;

    constructor(options: { token: string; cache: KeyValueCache; cosmosContainer: Container }) {
        // super(options); // this sends our server's `cache` through
        super(options.cosmosContainer)
        this.token = options.token.replace("Bearer ", "");
        const verifiedToken = getUser(this.token);
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
 