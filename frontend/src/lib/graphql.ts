/**
 * Minimal GraphQL client for the Amrutam backend (spring-graphql, /graphql).
 *
 * Shares the auth layer with api.ts: the JWT is read from jwtStore and
 * attached as a Bearer header. Responses from spring-graphql are the raw
 * execution result ({ data, errors }) — NOT the REST envelope — so only
 * the `errors` array is unwrapped here.
 */

import { ApiError, buildUrl, jwtStore } from './api';

export interface GraphQLErrorBody {
  message: string;
  locations?: Array<{ line: number; column: number }>;
  path?: Array<string | number>;
}

export interface GraphQLResponse<T> {
  data: T | null;
  errors: GraphQLErrorBody[] | null;
}

let opId = 0;

/**
 * Runs a GraphQL query/mutation against /graphql. Throws ApiError when the
 * transport fails or the payload contains `errors`.
 */
export async function gql<T>(query: string, variables: Record<string, unknown> = {}): Promise<GraphQLResponse<T>> {
  const accessToken = jwtStore.accessToken;

  let res: Response;
  try {
    res = await fetch(buildUrl('/graphql'), {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(accessToken !== null ? { Authorization: `Bearer ${accessToken}` } : {})
      },
      body: JSON.stringify({ id: String(++opId), query, variables })
    });
  } catch {
    throw new ApiError(0, 'NETWORK_ERROR', 'Failed to reach the Amrutam GraphQL endpoint');
  }

  const body = (await res.json().catch(() => null)) as GraphQLResponse<T> | null;

  if (res.status === 401) {
    throw new ApiError(401, 'UNAUTHORIZED', 'GraphQL request rejected: authentication required');
  }
  if (body === null) {
    throw new ApiError(res.status, 'HTTP_' + res.status, res.statusText);
  }
  if (body.errors !== null && body.errors.length > 0) {
    throw new ApiError(res.status, 'GRAPHQL_ERROR', body.errors.map((e) => e.message).join('; '));
  }

  return body;
}
