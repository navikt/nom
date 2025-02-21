import { Express } from "express";
import { initialize } from "unleash-client";

import config from "./config.js";

const unleash = initialize({
  url: config.unleash.url + "/api",
  appName: "nom-ui2",
  customHeaders: { Authorization: config.unleash.token },
});

export const setupUnleashProxy = (app: Express) => {
  app.get("/frackend/unleash/:flagName", (request, response) => {
    const flagName = request.params.flagName;
    const isUnleashEnabled = unleash.isEnabled(flagName);
    response.send({ isUnleashEnabled });
  });
};
