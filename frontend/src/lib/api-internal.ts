/**
 * Barrel export so pages can import the REST + GraphQL helpers from one
 * place. Re-exports the public surface of api.ts and graphql.ts.
 */

export * from './api';
export { gql } from './graphql';
export type { GraphQLResponse, GraphQLErrorBody } from './graphql';
