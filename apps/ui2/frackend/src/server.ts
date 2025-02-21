import express from "express";

import { setupActuators } from "./actuators.js";
import {
  setupNomApiProxy,
  setupNomRemedyIntegrasjonProxy,
  setupPaarorendeProxy,
  setupTeamcatApiProxy,
} from "./apiProxy.js";
import { errorHandling } from "./errorHandler.js";
import { setupStaticRoutes } from "./frontendRoute.js";
import { setupGqlPlaygroundProxy } from "./gqlplaygroundproxy.js";
import { verifyToken } from "./tokenValidation.js";
import { setupUnleashProxy } from "./unleash.js";

const app = express();

// Restricts the server to only accept UTF-8 encoding of bodies
app.use(express.urlencoded({ extended: true }));

setupActuators(app);

app.set("trust proxy", 1);

setupGqlPlaygroundProxy(app);

app.use(verifyToken);
setupNomApiProxy(app);
setupTeamcatApiProxy(app);
setupNomRemedyIntegrasjonProxy(app);
setupPaarorendeProxy(app);
setupUnleashProxy(app);

// Catch all route, må være sist
setupStaticRoutes(app);

app.use(errorHandling);

export default app;
