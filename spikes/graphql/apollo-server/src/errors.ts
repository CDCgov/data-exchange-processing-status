import { GraphQLError } from 'graphql';

export function missingRequired(paramName: String) {
    throw new GraphQLError(`Missing required parameter, ${paramName}`, {
        extensions: {
            code: 'BAD_USER_INPUT',
            http: {
                status: 400,
            }
        },
    });
}

export function unauthorized() {
    throw new GraphQLError('You are not authorized to perform this action.', {
        extensions: {
            code: 'FORBIDDEN',
            http: {
                status: 403,
            }
        },
    });
}
