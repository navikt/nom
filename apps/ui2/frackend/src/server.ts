import express from "express";

import { setupActuators } from "./actuators.js";
import {
  setupNomApiProxy,
  setupNomRemedyIntegrasjonProxy,
  setupTeamcatApiProxy,
} from "./apiProxy.js";
import { errorHandling } from "./errorHandler.js";
import { setupStaticRoutes } from "./frontendRoute.js";
import { setupGqlPlaygroundProxy } from "./gqlplaygroundproxy.js";
import { verifyJWTToken } from "./tokenValidation.js";

const app = express();

// Restricts the server to only accept UTF-8 encoding of bodies
app.use(express.urlencoded({ extended: true }));

setupActuators(app);

app.set("trust proxy", 1);

setupGqlPlaygroundProxy(app);

app.use(verifyJWTToken);
setupNomApiProxy(app);
setupTeamcatApiProxy(app);
setupNomRemedyIntegrasjonProxy(app);

// Catch all route, må være sist
setupStaticRoutes(app);

app.use(errorHandling);

export default app;
