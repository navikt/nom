import {
  getToken,
  requestAzureClientCredentialsToken,
  requestOboToken,
} from "@navikt/oasis";
import { Express, NextFunction, Request, Response } from "express";
import { createProxyMiddleware } from "http-proxy-middleware";

import config from "./config.js";

type TokenExchange = "obo" | "m2m";

type ProxyOptions = {
  ingoingUrl: string;
  outgoingUrl: string;
  scope: string;
  tokenExchange?: TokenExchange;
};

export const setupNomApiProxy = (app: Express) =>
  addProxyHandler(app, {
    ingoingUrl: "/frackend/nom-api",
    outgoingUrl: config.proxy.nomApiUrl,
    scope: config.proxy.nomApiScope,
  });

export const setupTeamcatApiProxy = (app: Express) =>
  addProxyHandler(app, {
    ingoingUrl: "/frackend/teamcat",
    outgoingUrl: config.proxy.teamcatApiUrl,
    scope: config.proxy.teamcatApiScope,
  });

export const setupNomAzureProxy = (app: Express) =>
  addProxyHandler(app, {
    ingoingUrl: "/frackend/nom-azure",
    outgoingUrl: config.proxy.nomAzureApiUrl,
    scope: config.proxy.nomAzureApiScope,
  });

export const setupNomRemedyIntegrasjonProxy = (app: Express) =>
  addProxyHandler(app, {
    ingoingUrl: "/frackend/nom-remedy-integrasjon",
    outgoingUrl: config.proxy.nomRemedyIntegrasjonApiUrl,
    scope: config.proxy.nomRemedyIntegrasjonApiScope,
  });

export const setupPaarorendeProxy = (app: Express) =>
  addProxyHandler(app, {
    ingoingUrl: "/frackend/paarorende",
    outgoingUrl: config.proxy.paarorendeApiUrl,
    scope: config.proxy.paarorendeApiScope,
  });

export function addProxyHandler(
  server: Express,
  { ingoingUrl, outgoingUrl, scope, tokenExchange = "obo" }: ProxyOptions,
) {
  server.use(
    ingoingUrl,
    async (request: Request, response: Response, next: NextFunction) => {
      const token = getToken(request);
      if (!token) {
        response.status(401).send();
        return;
      }
      const downstreamToken =
        tokenExchange === "m2m"
          ? await requestAzureClientCredentialsToken(scope)
          : await requestOboToken(token, scope);
      if (downstreamToken.ok) {
        request.headers["obo-token"] = downstreamToken.token;
        return next();
      }
      if (tokenExchange === "m2m") {
        console.error(
          `Client credentials token request failed for scope ${scope}: ${downstreamToken.error}`,
        );
        response.status(500).send();
        return;
      }
      response.status(403).send();
      return;
    },
    createProxyMiddleware({
      target: outgoingUrl,
      changeOrigin: true,
      logger: console,
      on: {
        proxyReq: (proxyRequest, request) => {
          const obo = request.headers["obo-token"];
          if (obo) {
            proxyRequest.removeHeader("obo-token");
            proxyRequest.removeHeader("cookie");
            proxyRequest.setHeader("Authorization", `Bearer ${obo}`);
          } else {
            console.log(
              `Access token var not present in session for scope ${scope}`,
            );
          }
        },
      },
    }),
  );
}
