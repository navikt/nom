import { Express } from "express";

const graphiqlSource: Record<string, string> = {
  reactdev: "https://unpkg.com/react@18/umd/react.production.min.js",
  reactdomdev: "https://unpkg.com/react-dom@18/umd/react-dom.production.min.js",
  graphiqljs: "https://unpkg.com/graphiql/graphiql.min.js",
  graphiqlcss: "https://unpkg.com/graphiql/graphiql.min.css",
  graphiqlpluginexplorerjs:
    "https://unpkg.com/@graphiql/plugin-explorer/dist/index.umd.js",
  graphiqlpluginexplorercss:
    "https://unpkg.com/@graphiql/plugin-explorer/dist/style.css",
};

const extractPath = (url: string) => url.slice("https://unpkg.com".length);

// eslint-disable-next-line @typescript-eslint/no-explicit-any
let vendorStorage: Record<string, any> = {};

async function initVendorStorage() {
  try {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const fetches: Record<string, any> = {};
    for (const s of Object.keys(graphiqlSource)) {
      fetches[s] = fetch(graphiqlSource[s]);
    }
    await Promise.allSettled(Object.values(fetches));

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const bodies: Record<string, any> = {};
    for (const s of Object.keys(fetches)) {
      try {
        // eslint-disable-next-line unicorn/no-await-expression-member
        bodies[s] = (await fetches[s]).text();
      } catch (error) {
        console.error(
          { err: error, graphqlsrc: s, graphqlsrcUrl: graphiqlSource[s] },
          "Error during vendoring of graphiql",
        );
      }
    }
    await Promise.allSettled(Object.values(bodies));

    for (const s of Object.keys(bodies)) {
      vendorStorage[s] = await bodies[s];
    }
  } catch (error) {
    vendorStorage = {};
    console.error("Could not vendor graphql js and css from unpkg.com", error);
  }
}

// eslint-disable-next-line unicorn/prefer-top-level-await
initVendorStorage();

const htmlForGqlGraphiql = `<!DOCTYPE html>
<html lang="en">
<head>
    <title>NOM GraphiQL</title>
    <style>
        body {
            height: 100%;
            margin: 0;
            width: 100%;
            overflow: hidden;
        }

        #graphiql {
            height: 100vh;
        }
    </style>
    <script src="/vendor${extractPath(
      graphiqlSource.reactdev,
    )}" crossorigin></script>
    <script src="/vendor${extractPath(
      graphiqlSource.reactdomdev,
    )}" crossorigin></script>
    <script src="/vendor${extractPath(
      graphiqlSource.graphiqljs,
    )}" type="application/javascript"></script>
    <link rel="stylesheet" href="/vendor${extractPath(
      graphiqlSource.graphiqlcss,
    )}" />
    <script src="/vendor${extractPath(
      graphiqlSource.graphiqlpluginexplorerjs,
    )}" crossorigin></script>
    <link rel="stylesheet" href="/vendor${extractPath(
      graphiqlSource.graphiqlpluginexplorercss,
    )}"/>
</head>
<body>
<div id="graphiql">Loading...</div>
<script>
    const params = new URLSearchParams(window.location.search);
    const path = params.get("path") || "/frackend/nom-api/graphql";
    const url = location.protocol + "//" + location.host + path;
    const wsPath = params.get("wsPath") || "/frackend/nom-api/graphql";
    const wsProtocol = location.protocol === 'https:' ? 'wss:' : 'ws:';
    const subscriptionUrl = wsProtocol + "//" + location.host + wsPath;
    const gqlFetcher = GraphiQL.createFetcher({'url': url, 'subscriptionUrl': subscriptionUrl});
    const explorerPlugin = GraphiQLPluginExplorer.explorerPlugin();
    const xsrfToken = document.cookie.match(new RegExp('(?:^| )XSRF-TOKEN=([^;]+)'));
    const headers = xsrfToken ? '{ "X-XSRF-TOKEN" : "' + xsrfToken[1] + '" }' : '{}';
    ReactDOM.render(
        React.createElement(GraphiQL, {
            fetcher: gqlFetcher,
            defaultVariableEditorOpen: true,
            headerEditorEnabled: true,
            shouldPersistHeaders: true,
            headers: headers,
            plugins: [explorerPlugin]
        }),
        document.getElementById('graphiql'),
    );
</script>
</body>
</html>
`;

export function setupGqlPlaygroundProxy(app: Express) {
  // TODO: delete after some time has passed and using "/playground" is standard.
  app.get("/nom-gql-playground", (request, response) => {
    return response.redirect("/playground");
  });

  app.get("/nom-gql-graphiql", (request, response) => {
    return response.redirect("/playground");
  });

  app.get("/playground", (request, response) => {
    if (!vendorStorage) {
      response.status(404).send("graphiql currently unavailable");
      return;
    }
    response.send(htmlForGqlGraphiql);
  });

  for (const s of Object.keys(graphiqlSource)) {
    const path = extractPath(graphiqlSource[s]);
    app.get("/vendor" + path, (request, response) => {
      const out = vendorStorage[s];
      if (!out) {
        response.send(404).send(`${s} not found`);
        return;
      }
      const mimeType = getMimeType(path);
      response.type(mimeType).send(out);
    });
  }
}

function getMimeType(path: string) {
  if (path.endsWith(".css")) {
    return "text/css";
  }
  if (path.endsWith(".map")) {
    return "application/json";
  }

  return "application/javascript";
}
