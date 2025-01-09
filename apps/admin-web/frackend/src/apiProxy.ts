import { getToken, requestOboToken } from "@navikt/oasis";
import { Express, NextFunction, Request, Response } from "express";
import { createProxyMiddleware } from "http-proxy-middleware";

import config from "./config.js";

type ProxyOptions = {
  ingoingUrl: string;
  outgoingUrl: string;
  scope: string;
};

export const setupNomAdminApiProxy = (app: Express) =>
  addProxyHandler(app, {
    ingoingUrl: "/frackend/admin-api",
    outgoingUrl: config.proxy.nomAdminApiUrl,
    scope: config.proxy.nomAdminApiScope,
  });

export const setupNomApiProxy = (app: Express) =>
  addProxyHandler(app, {
    ingoingUrl: "/frackend/api",
    outgoingUrl: config.proxy.nomApiUrl,
    scope: config.proxy.nomApiScope,
  });

export const setupNomRemedyProxy = (app: Express) =>
  addProxyHandler(app, {
    ingoingUrl: "/frackend/remedy",
    outgoingUrl: config.proxy.nomRemedyUrl,
    scope: config.proxy.nomRemedyScope,
  });

export function addProxyHandler(
  server: Express,
  { ingoingUrl, outgoingUrl, scope }: ProxyOptions,
) {
  server.use(
    ingoingUrl,
    async (request: Request, response: Response, next: NextFunction) => {
      const token = getToken(request);
      if (!token) {
        return response.status(401).send();
      }
      const obo = await requestOboToken(token, scope);
      if (obo.ok) {
        request.headers["obo-token"] = obo.token;
        return next();
      } else {
        console.log("OBO-exchange failed", obo.error);
        return response.status(403).send();
      }
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
