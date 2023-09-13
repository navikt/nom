import { addProxyHandler } from "@navikt/express-obo-token-exchange";
import { Express } from "express";

import config from "./config.js";

export const setupNomApiProxy = (app: Express) =>
  addProxyHandler(
    app,
    "/nom-api",
    config.proxy.nomApiUrl,
    config.proxy.nomApiScope,
  );

export const setupTeamcatApiProxy = (app: Express) =>
  addProxyHandler(
    app,
    "/teamcat",
    config.proxy.teamcatApiUrl,
    config.proxy.teamcatApiScope,
  );
