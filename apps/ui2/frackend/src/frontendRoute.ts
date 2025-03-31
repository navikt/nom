import path from "node:path";

import { addServeSpaHandler, serveViteMode } from "@navikt/vite-mode";
import AdmZip from "adm-zip";
import express, { Express } from "express";
import NodeCache from "node-cache";

const bundleCache = new NodeCache({
  stdTTL: 60 * 60 * 24, // 24 hours
});

type CachedZipBundle = {
  bundle: AdmZip;
  etag: string;
};

const branchNameRegexp = /^[0-9a-zA-Z_-]+$/;

function extractBranchFromCookie(cookieString: string | undefined) {
  if (!cookieString) {
    return;
  }
  const regexp = /display-branch=((\S|-)+(;|$))/;

  const maybeMatch = cookieString.match(regexp);
  return maybeMatch === null ? undefined : maybeMatch[1];
}

// get a zipped webapp bundle from CDN by branch name. With caching and check on etag for updates. Returns `undefined` if not found
async function spaCdnBundleByBranchName(
  validatedBranchName: string | undefined,
) {
  if (validatedBranchName !== undefined) {
    const url = `https://cdn.nav.no/org/nom-ui2/webapp-bundle/${validatedBranchName}/dist.zip`;
    const zipBundle = bundleCache.get<CachedZipBundle>(validatedBranchName);
    if (zipBundle !== undefined) {
      const urlHeadResponse = await fetch(url, { method: "HEAD" });
      const currentEtag =
        (urlHeadResponse.status === 200 &&
          urlHeadResponse.headers.get("etag")) ||
        "no-etag";
      if (zipBundle.etag === currentEtag) {
        return zipBundle.bundle;
      }
    }
    const resp = await fetch(url);
    if (resp.status === 200) {
      const contents = await resp.bytes();
      const zipBundle = new AdmZip(Buffer.from(contents));
      if (zipBundle) {
        bundleCache.set<CachedZipBundle>(validatedBranchName, {
          bundle: zipBundle,
          etag: resp.headers.get("etag") || Math.random().toString(),
        });
        return zipBundle;
      }
    }
  }
  return;
}

// Serve files from the ZIP based on the URL
function zippedBundleHandler(admZip: AdmZip) {
  return (
    request: express.Request,
    response: express.Response,
    next: express.NextFunction,
  ) => {
    const requestedPath =
      request.path === "" || request.path === "/"
        ? "index.html"
        : request.path.slice(1); // Remove leading slash
    const entry = admZip.getEntries().find((element) => {
      return element.entryName.slice("dist/".length) === requestedPath;
    });

    const out = entry ?? admZip.getEntry("dist/index.html");

    if (out) {
      response.setHeader("Content-Type", getContentType(out.entryName));
      response.send(out.getData());
      return;
    }
    next();
  };
}

// Helper function to determine content type based on file extension
function getContentType(fileName: string) {
  const extension = path.extname(fileName).toLowerCase();
  switch (extension) {
    case ".html": {
      return "text/html";
    }
    case ".css": {
      return "text/css";
    }
    case ".js": {
      return "application/javascript";
    }
    case ".svg": {
      return "image/svg+xml";
    }
    default: {
      return "application/octet-stream";
    }
  }
}

function setupStaticRoutesWithBranchSwap(app: Express) {
  const defaultStaticHandler = express.static("./public", { index: false });

  app.get("/display-branch/:name", (request, response) => {
    const branchName = request.params.name;
    if (branchName === "main") {
      response.clearCookie("display-branch").redirect("/");
    } else {
      bundleCache.del(branchName);
      response
        .cookie("display-branch", branchName, {
          httpOnly: false,
          secure: false,
          sameSite: "lax",
          path: "/",
        })
        .redirect("/");
    }
  });

  app.use(async (request, response, next) => {
    const branchNameMaybe = extractBranchFromCookie(request.header("cookie"));
    if (!branchNameMaybe) {
      defaultStaticHandler(request, response, next);
      return;
    }
    const isBranchNameOkFormat = branchNameRegexp.test(branchNameMaybe);
    if (!isBranchNameOkFormat) {
      response.status(400).send({
        status: 400,
        message: "Bad format for branchname: " + branchNameMaybe,
      });
      return;
    }
    const okFormattedBranchname = branchNameMaybe;
    const maybeBundle = await spaCdnBundleByBranchName(okFormattedBranchname);

    const maybeHandler = maybeBundle
      ? zippedBundleHandler(maybeBundle)
      : undefined;

    if (maybeHandler !== undefined) {
      maybeHandler(request, response, next);
      return;
    }
    const txt = `<a href=/display-branch/main>Vis main-branch webapp (Ingen opplastet webapp på CDN for branch ${okFormattedBranchname})</a>`;
    response.setHeader("Content-Type", "text/html");
    response.send(txt);
    return;
  });

  // When deployed, the built frontend is copied into the public directory. If running BFF locally the directory will not exist.
  const spaFilePath = path.resolve("./public", "index.html");

  serveViteMode(app, {});
  addServeSpaHandler(app, spaFilePath);
}

export function setupStaticRoutes(app: Express) {
  const enableBundleFromCDN =
    (process.env.ENABLE_CDN_BUNDLES &&
      process.env.ENABLE_CDN_BUNDLES === "true") ??
    false;
  if (enableBundleFromCDN) {
    setupStaticRoutesWithBranchSwap(app);
  } else {
    // default setup
    app.use(express.static("./public", { index: false }));

    // When deployed, the built frontend is copied into the public directory. If running BFF locally the directory will not exist.
    const spaFilePath = path.resolve("./public", "index.html");

    serveViteMode(app, {});
    addServeSpaHandler(app, spaFilePath);
  }
}
