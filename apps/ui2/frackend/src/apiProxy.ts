import { Express } from "express";

import config from "./config.js";
import { addProxyHandler } from "./vendor/backend-for-frontend-utils/apiProxy.js";

export const setupNomApiProxy = (app: Express) =>
  addProxyHandler(app, {
    ingoingUrl: "/frackend/nom-api",
    outgoingUrl: config.proxy.nomApiUrl,
    scope: config.proxy.nomApiScope,
    flow: "ON_BEHALF_OF",
  });

export const setupTeamcatApiProxy = (app: Express) =>
  addProxyHandler(app, {
    ingoingUrl: "/frackend/teamcat",
    outgoingUrl: config.proxy.teamcatApiUrl,
    scope: config.proxy.teamcatApiScope,
    flow: "ON_BEHALF_OF",
  });

export const setupNomRemedyIntegrasjonProxy = (app: Express) =>
  addProxyHandler(app, {
    ingoingUrl: "/frackend/nom-remedy-integrasjon",
    outgoingUrl: config.proxy.nomRemedyIntegrasjonApiUrl,
    scope: config.proxy.nomRemedyIntegrasjonApiScope,
    flow: "ON_BEHALF_OF",
  });
