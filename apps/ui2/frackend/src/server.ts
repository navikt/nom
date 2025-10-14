import express from "express";

import { setupActuators } from "./actuators.js";
import {
  setupNomApiProxy,
  setupNomAzureProxy,
  setupNomRemedyIntegrasjonProxy,
  setupPaarorendeProxy,
  setupTeamcatApiProxy,
} from "./apiProxy.js";
import { errorHandling } from "./errorHandler.js";
import { setupStaticRoutes } from "./frontendRoute.js";
import { setupPlayground } from "./gqlPlayground";
import { verifyToken } from "./tokenValidation.js";
import { setupUnleashProxy } from "./unleash.js";

const app = express();

// Restricts the server to only accept UTF-8 encoding of bodies
app.use(express.urlencoded({ extended: true }));

setupActuators(app);

app.set("trust proxy", 1);

app.use(verifyToken);
setupNomApiProxy(app);
setupTeamcatApiProxy(app);
setupNomAzureProxy(app);
setupNomRemedyIntegrasjonProxy(app);
setupPaarorendeProxy(app);
setupUnleashProxy(app);
setupPlayground(app);

// Catch all route, må være sist
setupStaticRoutes(app);

app.use(errorHandling);

export default app;
