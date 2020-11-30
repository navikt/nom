import { useEffect, useReducer, useState } from "react";
import {
  handleHttpResponse,
  startApiPost,
  validateJson,
} from "./utils_fetch_api";
import {
  lookups,
  LookupState,
} from "./utils_api_hook";

type CreatedId = string;

interface ApiPostBody {
  parentId: string;
  enhetNavn: string;
  enhetType: string;
}

async function doFetch(
  payload: ApiPostBody,
  signal: AbortSignal,
  setOut: CallableFunction
): Promise<void> {
  try {
    const apiPostRes = await startApiPost<ApiPostBody>(
      "/api/orgenhet/",
      signal,
      payload
    );
    const httpOkRes = await handleHttpResponse(apiPostRes);
    const jsonOkRes = await validateJson(httpOkRes);
    const out: CreatedId = (jsonOkRes as any).id;
    setOut(lookups.createResolved(out));
  } catch (err) {
    if (err instanceof DOMException) {
      return; // abortController
    }
    console.error(err);
    setOut(lookups.createError(err.message));
  }
}

export default function usePostOrgEnhet(
  payload: ApiPostBody | null
): [LookupState<CreatedId>, () => void] {
  const [out, setOut] = useState<LookupState<CreatedId>>(lookups.createWaiting());
  const [toggle, flipToggle] = useReducer((toggle: boolean) => !toggle, false);
  useEffect(() => {
    if (!payload){ 
      setOut({lookupState: "WAITING"});
      return;
    }
    setOut(lookups.createPending());
    const abortController = new AbortController();
    const signal = abortController.signal;

    doFetch(payload, signal, setOut);
    return function (): void {
      abortController.abort();
    };
  }, [payload, toggle]);

  return [out, flipToggle];
}
