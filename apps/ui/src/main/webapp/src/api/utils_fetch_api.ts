import fetch_configs from "./utils_fetch_configs";

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

export async function validateGraphql(json: any): Promise<Record<string, any>> {
  if (json.errors) {
    console.error({ graphqlFeilmelding: json.errors });
    throw new Error("Graphl-feil");
  } else {
    return json;
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

export async function performFetch<T>(
  url: string,
  abortSignal: AbortSignal,
  fetchConfig: RequestInit,
  data?: T
): Promise<Response> {
  try {
    const body: any = data;
    const response = await fetch(url, {
      ...fetchConfig,
      signal: abortSignal,
      body: body,
    });
    return response;
  } catch (err) {
    console.error(err);
    throw err;
  }
}

interface GraphqlResponse {
  data: any;
  errors: any;
}
