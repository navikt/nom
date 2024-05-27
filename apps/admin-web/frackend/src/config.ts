import { requireEnvironment } from "@navikt/backend-for-frontend-utils";

const azureAd = {
  clientId: requireEnvironment("AZURE_APP_CLIENT_ID"),
  issuer: requireEnvironment("AZURE_OPENID_CONFIG_ISSUER"),
  tokenEndpoint: requireEnvironment("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),
  jwk: requireEnvironment("AZURE_APP_JWK"),
  openIdJwkUris: requireEnvironment("AZURE_OPENID_CONFIG_JWKS_URI"),
};

const proxy = {
  nomAdminApiScope: requireEnvironment("NOM_ADMIN_API_SCOPE"),
  nomAdminApiUrl: requireEnvironment("NOM_ADMIN_API_URL"),
};

const app = {
  nodeEnv: requireEnvironment("NODE_ENV"),
  host: requireEnvironment("EXPRESS_HOST"),
  port: Number(requireEnvironment("EXPRESS_PORT")),
};

export default { azureAd, proxy, app };
