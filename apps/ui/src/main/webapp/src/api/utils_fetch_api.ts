export const FETCH_CONFIG_GET: RequestInit = {
  method: "GET",
  mode: "cors",
  cache: "no-cache",
  credentials: "same-origin",
  headers: {
    "Content-Type": "application/json",
  },
};

export const API_URL = (function (): string {
  // return "https://nom-ui.intern.dev.nav.no/";
  return "";
})();

export const FETCH_CONFIG_POST: RequestInit = {
  method: "POST",
  mode: "cors",
  cache: "no-cache",
  credentials: "same-origin",
  headers: {
    "Content-Type": "application/json",
  },
};

export async function startApiGet(
  url: string,
  abortSignal: AbortSignal
): Promise<Response> {
  try {
    const response = await fetch(url, {
      ...FETCH_CONFIG_GET,
      signal: abortSignal,
    });
    return response;
  } catch (err) {
    console.error(err);
    throw err;
  }
}

export async function startApiPost<T>(
  url: string,
  abortSignal: AbortSignal,
  data: T
): Promise<Response> {
  try {
    const body = JSON.stringify(data);
    // console.log({x: data});
    const response = await fetch(url, {
      ...FETCH_CONFIG_POST,
      signal: abortSignal,
      body: body,
    });
    return response;
  } catch (err) {
    console.error(err);
    throw err;
  }
}

export async function handleHttpResponse(
  response: Response
): Promise<Response> {
  if (response.ok) {
    if (response.status !== 200) {
      throw new Error(
        'TODO: Implementere håndtering av "OK, men ikke 200" http statuskode ' +
          response.status
      );
    }
    return response;
  } else {
    console.error({ responseWithHttpError: response.clone() });
    throw new Error(response.status + ": " + response.statusText);
  }
}

export async function validateJson(
  response: Response
): Promise<Record<string, any>> {
  try {
    const responseClone = response.clone();
    const json = await responseClone.json();
    return json;
  } catch (err) {
    const failingJson = await response.text();
    console.error({ feilformatertJson: failingJson });
    throw new Error("Fikk feilformatert JSON fra server");
  }
}
