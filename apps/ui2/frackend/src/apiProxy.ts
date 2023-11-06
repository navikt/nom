import { addProxyHandler } from "@navikt/backend-for-frontend-utils";
import { Express } from "express";

import config from "./config.js";

export const setupNomApiProxy = (app: Express) =>
  addProxyHandler(app, {
    ingoingUrl: "/nom-api",
    outgoingUrl: config.proxy.nomApiUrl,
    scope: config.proxy.nomApiScope,
    flow: "ON_BEHALF_OF",
  });

export const setupTeamcatApiProxy = (app: Express) =>
  addProxyHandler(app, {
    ingoingUrl: "/teamcat",
    outgoingUrl: config.proxy.teamcatApiUrl,
    scope: config.proxy.teamcatApiScope,
    flow: "ON_BEHALF_OF",
  });
