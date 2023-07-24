import { Express } from "express";

const graphiqlSource: Record<string, string> = {
  reactdev: "https://unpkg.com/react@18.2.0/umd/react.development.js",
  reactdomdev:
    "https://unpkg.com/react-dom@18.2.0/umd/react-dom.development.js",
  graphiqlcss: "https://unpkg.com/graphiql@2.2.0/graphiql.min.css",
  graphiqlcssMap: "https://unpkg.com/graphiql@2.2.0/graphiql.min.css.map",
  graphiqljs: "https://unpkg.com/graphiql@2.2.0/graphiql.min.js",
  graphiqljsMap: "https://unpkg.com/graphiql@2.2.0/graphiql.min.js.map",
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
<head title="NOM graphiql">
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
    <script crossorigin src="/vendor${extractPath(
      graphiqlSource.reactdev,
    )}"></script>
    <script crossorigin src="/vendor${extractPath(
      graphiqlSource.reactdomdev,
    )}"></script>
    <link rel="stylesheet" href="/vendor${extractPath(
      graphiqlSource.graphiqlcss,
    )}"/><title></title>
</head>

<body>
<div id="graphiql">Loading...</div>
<script src="/vendor${extractPath(
  graphiqlSource.graphiqljs,
)}" type="application/javascript"></script>
<script>
    const params = new URLSearchParams(window.location.search);

    const path = params.get("path") || "/nom-api/graphql";
    const url = location.protocol + "//" + location.host + path;

    const wsPath = params.get("wsPath") || "/nom-api/graphql";
    const wsProtocol = location.protocol === 'https:' ? 'wss:' : 'ws:';
    const subscriptionUrl = wsProtocol + "//" + location.host + wsPath;

    const gqlFetcher = GraphiQL.createFetcher({
        url,
        subscriptionUrl,
    });

    const root = ReactDOM.createRoot(document.getElementById('graphiql'));
    root.render(
      React.createElement(GraphiQL, {
            fetcher: gqlFetcher,
            defaultVariableEditorOpen: true,
            headerEditorEnabled: true,
            shouldPersistHeaders: true
        })
    )
</script>
</body>
</html>
`;

export function setupGqlPlaygroundProxy(app: Express) {
  app.get("/nom-gql-playground", (request, response) => {
    response.send(
      "<h1>Playground is replaced</h1><p style='font-size: 200%'>Use <a href='/nom-gql-graphiql'>graphiql</a> instead</</p>",
    );
  });

  app.get("/nom-gql-graphiql", (request, response) => {
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
