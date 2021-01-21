import React from "react";
import { useReducer, useState } from "react";
import { OrganisasjonsEnhet } from "../common/types";
import { lookups, LookupState } from "./utils_api_hook";
import {
  handleHttpResponse,
  performFetch as doWrappedFetch,
  validateGraphql,
  validateJson,
} from "./utils_fetch_api";
import fetchConfig from "./utils_fetch_configs";

function createQuery(id: string | null): string {
  const whereClause = id ? `where: {id: "${id}"}` : "where: null";
  return `query {
        organisasjonsenhet(${whereClause}) {
            id
            navn
            gyldigFom
            gyldigTom
            organiseringer {
              retning
              organisasjonsenhet{
                navn
                id
              }
            }
        }
    }`;
}

const graphqlApiUrl = (function () {
  const CREATE_REACT_APP_PORT = "3000";
  const FRACKEND_PORT = "8080";
  const CLOUD_PORT = "80";
  switch (window.location.port) {
    case CREATE_REACT_APP_PORT:
      return "http://localhost:8081/graphql";
    case FRACKEND_PORT:
    case CLOUD_PORT:
    default:
      return "/api/v1/nom-api/graphql";
  }
})();

async function doFetch(
  id: string | null,
  signal: AbortSignal,
  setOut: CallableFunction
): Promise<void> {
  try {
    const apiGetres = await doWrappedFetch(
      graphqlApiUrl,
      signal,
      fetchConfig.POST_GRAPHQL,
      createQuery(id)
    );
    const httpOkRes = await handleHttpResponse(apiGetres);
    const jsonOkRes = await validateJson(httpOkRes);
    const graphqlOkRes = await validateGraphql(jsonOkRes);
    setOut(lookups.createResolved(graphqlOkRes.data.organisasjonsenhet[0]));
  } catch (err) {
    if (err instanceof DOMException) {
      return; // abortController
    }
    console.error(err);
    setOut(lookups.createError(err.message));
  }
}

const pendingState = {
  lookupState: "PENDING",
} as LookupState<OrganisasjonsEnhet>;

export default function useGetOrganisasjonsEnhet(
  id: string | null
): [LookupState<OrganisasjonsEnhet>, () => void] {
  const [out, setOut] = useState<LookupState<OrganisasjonsEnhet>>(pendingState);
  const [toggle, flipToggle] = useReducer((toggle: boolean) => !toggle, false);

  React.useEffect(() => {
    if (id === null) {
      return;
    }
    setOut(pendingState);
    const abortController = new AbortController();
    const signal = abortController.signal;

    doFetch(id, signal, setOut);
    return function (): void {
      abortController.abort();
    };
  }, [id, toggle]);

  return [out, flipToggle];
}
