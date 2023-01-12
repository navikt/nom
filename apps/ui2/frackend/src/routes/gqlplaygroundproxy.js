const graphiqlSrc = {
    reactdev: "https://unpkg.com/react@16.14.0/umd/react.development.js",
    reactdomdev: "https://unpkg.com/react-dom@16.14.0/umd/react-dom.development.js",
    graphiqlcss: "https://unpkg.com/graphiql@2.2.0/graphiql.min.css",
    graphiqljs: "https://unpkg.com/graphiql@2.2.0/graphiql.min.js",
    graphiqljsMap: "https://unpkg.com/graphiql@2.2.0/graphiql.min.js.map",
};

const extractPath = (url) => url.substring("https://unpkg.com".length);

let vendorStorage = {};

async function initVendorStorage() {
    try {
        const fetches = {};
        for (const s of Object.keys(graphiqlSrc)) {
            fetches[s] = fetch(graphiqlSrc[s]);
        }
        await Promise.allSettled(Object.values(fetches));

        const bodies = {};
        for (const s of Object.keys(fetches)) {
            bodies[s] = (await fetches[s]).text();
        }
        await Promise.allSettled(Object.values(bodies));

        for (const s of Object.keys(bodies)) {
            vendorStorage[s] = await bodies[s];
        }
    } catch (e) {
        vendorStorage = {};
        console.error("Could not vendor graphql js and css from unpkg.com", e);
    }
}

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
    graphiqlSrc.reactdev
)}"></script>
    <script crossorigin src="/vendor${extractPath(
    graphiqlSrc.reactdomdev
)}"></script>
    <link rel="stylesheet" href="/vendor${extractPath(
    graphiqlSrc.graphiqlcss
)}"/><title></title>
</head>

<body>
<div id="graphiql">Loading...</div>
<script src="/vendor${extractPath(
    graphiqlSrc.graphiqljs
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

    ReactDOM.render(
        React.createElement(GraphiQL, {
            fetcher: gqlFetcher,
            defaultVariableEditorOpen: true,
            headerEditorEnabled: true,
            shouldPersistHeaders: true
        }),
        document.getElementById('graphiql'),
    );
</script>
</body>
</html>
`;

export function setupGqlPlaygroundProxy(app) {
    app.get("/nom-gql-playground", (req, res) => {
        res.send("<h1>Playground is replaced</h1><p style='font-size: 200%'>Use <a href='/nom-gql-graphiql'>graphiql</a> instead</</p>")
    });

    app.get("/nom-gql-graphiql", (req, res) => {
        if (!vendorStorage) {
            res.status(404).send("graphiql currently unavailable");
        }
        res.send(htmlForGqlGraphiql);
    });

    Object.keys(graphiqlSrc).forEach((s) => {
        const path = extractPath(graphiqlSrc[s]);
        app.get("/vendor" + path, (req, res) => {
            const mimeType = path.endsWith(".css")
                ? "text/css"
                : path.endsWith(".map")
                ? "appllication/json"
                : "application/javascript";
            res.type(mimeType).send(vendorStorage[s]);
        });
    });
}
