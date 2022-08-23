function gqlPlayground(){

    return `<!DOCTYPE html>
    <html lang="en">

    <head>
        <meta charset="utf-8"/>
        <meta name="viewport"
              content="user-scalable=no, initial-scale=1.0, minimum-scale=1.0, maximum-scale=1.0, minimal-ui">
        <title>Playground</title>
        <link rel="stylesheet" href="/nom-api/vendor/playground/static/css/index.css"/>
        <link rel="shortcut icon" href="/nom-api/vendor/playground/favicon.png"/>
        <script src="/nom-api/vendor/playground/static/js/middleware.js"></script>
    </head>

    <body>
    <div id="root">
        <img src="/nom-api/vendor/playground/logo.png" alt="Logo">
        <div class="loading"> Loading
            <span class="title">GraphQL Playground</span>
        </div>
    </div>

    <script>
        let csrf = null;
        let properties = {"enabled":true,"endpoint":"/nom-api/graphql","subscriptionEndpoint":"/subscriptions","tabs":[{"endpoint":"/nom-api/graphql","query":'#',"name":"Mytab","variables":null,"responses":null,"headers":null}]};
        properties["settings"] = {
                "request.credentials": "include"
        }
        if (!properties.headers) {
            properties.headers = {};
        }
        if (csrf) {
            properties.headers[csrf.headerName] = csrf.token;
        }
        if (properties.tabs) {
            properties.tabs.forEach(
                    tab => tab.headers = Object.assign({}, properties.headers || {}, tab.headers || {}));
        }
        properties.setTitle = false;
        window.addEventListener('load', function (event) {
            GraphQLPlayground.init(document.getElementById('root'), properties)
        });
    </script>
    </body>

    </html>
`
}

export function setupGqlPlaygroundProxy(app){
    app.get('/nom-gql-playground', (req, res, next) => {
        const sesCookieStr = req.headers.cookie.split(";").find(it => it.startsWith(" nom-ui-session") || it.startsWith("nom-ui-session")).split("=")[1]
        const out = gqlPlayground()
        res.send(
            out
        );
    });
}

