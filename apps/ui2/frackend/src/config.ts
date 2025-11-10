function requireEnvironment(environmentName: string) {
  const environmentContent = process.env[environmentName];
  if (!environmentContent) {
    throw new Error(
      "Missing environment variable with name: " + environmentName,
    );
  }
  return environmentContent;
}

const azureAd = {
  clientId: requireEnvironment("AZURE_APP_CLIENT_ID"),
  issuer: requireEnvironment("AZURE_OPENID_CONFIG_ISSUER"),
  tokenEndpoint: requireEnvironment("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),
  jwk: requireEnvironment("AZURE_APP_JWK"),
  openIdJwkUris: requireEnvironment("AZURE_OPENID_CONFIG_JWKS_URI"),
};

const proxy = {
  nomApiScope: requireEnvironment("NOM_API_SCOPE"),
  nomApiUrl: requireEnvironment("NOM_API_URL"),
  teamcatApiScope: requireEnvironment("TEAM_CATALOG_SCOPE"),
  teamcatApiUrl: requireEnvironment("TEAM_CATALOG_API_URL"),
  nomRemedyIntegrasjonApiScope: requireEnvironment(
    "NOM_REMEDY_INTEGRASJON_API_SCOPE",
  ),
  nomRemedyIntegrasjonApiUrl: requireEnvironment(
    "NOM_REMEDY_INTEGRASJON_API_URL",
  ),
  paarorendeApiScope: requireEnvironment("PAARORENDE_API_SCOPE"),
  paarorendeApiUrl: requireEnvironment("PAARORENDE_API_URL"),

  nomAzureApiScope: requireEnvironment("NOM_AZURE_SCOPE"),
  nomAzureApiUrl: requireEnvironment("NOM_AZURE_URL"),

  orgEnhetKostnadsstedMappingApiScope: requireEnvironment("ORGENHET_KOSTNADSSTED_MAPPING_API_SCOPE"),
  orgEnhetKostnadsstedMappingApiUrl: requireEnvironment("ORGENHET_KOSTNADSSTED_MAPPING_API_URL"),
};

const unleash = {
  url: requireEnvironment("UNLEASH_SERVER_API_URL"),
  token: requireEnvironment("UNLEASH_SERVER_API_TOKEN"),
};

const app = {
  nodeEnv: requireEnvironment("NODE_ENV"),
  host: requireEnvironment("EXPRESS_HOST"),
  port: Number(requireEnvironment("EXPRESS_PORT")),
};

export default { azureAd, proxy, app, unleash };
