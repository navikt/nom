/*
 * Experimental method to "move" a SPA webapp from a localhost server to the environment host.
 *
 * Makes it possible to preview webapp changes in a prod environment without having to deploy code.
 */

let postedHtml = {};


export function setupUiDevEndpoint(app) {
    app.get('/uidev', (req, res, next) => {
        const {ses: uidevSession} = req.query
        if(uidevSession){
            if(!postedHtml[uidevSession]){
                res.status(404).send("Error: '" + uidevSession + "' was not a current session. Return to /uidev <a href=\"/uidev\">here</a>");
                return;
            }
            let {html: spaHtml, port: port} = postedHtml[req.query.ses]
            spaHtml = spaHtml.replaceAll(
                `import RefreshRuntime from "/@react-refresh"`,
                `import RefreshRuntime from "` + "http://localhost:"+ port +  `/@react-refresh"`
            )
            spaHtml = spaHtml.replaceAll("href=\"/","href=\"http://localhost:"+ port + "/")
            spaHtml = spaHtml.replaceAll("src=\"/","src=\"http://localhost:" + port + "/")
            res.send(spaHtml)
            return;
        }
        const sessionLinks = Object.keys(postedHtml).map(it => "<a href=\"/uidev?ses=" + it + "\">" + it + " (localhost:" + postedHtml[it].port + ")</a>").join("<br/>")
        res.send(uidevWebHtml(sessionLinks));
    });

    app.post('/uidev', async (req, res, next) => {
        const reqBody = await req.read();
        if(Object.keys(postedHtml).length > 10) {
            res.status(403).send("Too many uidev sessions. Cannot add more")
            return;
        }
        const ses = req.query["ses"] || "default";
        const port = req.query["port"] || "4173"
        postedHtml[ses] = {html: reqBody.toLocaleString(), port};
        res.redirect("/uidev?ses=" + ses);
    })

    app.delete('/uidev',(req,res,next) => {
        const l = Object.keys(postedHtml).length;
        postedHtml = {}
        res.status(200).send("deleted " + l + " html entries.")
    })
}


function uidevWebHtml(sessionLinks) {
    return `<!DOCTYPE html>
            <html lang="en">
            <head>
                <script>
                    function postFromDevServerToEnvServer() {
                        const portnum = document.getElementById("input-portnumber").value
                        const sesName = document.getElementById("input-sesid").value
                        fetch("http://localhost:" + portnum).then(res => {
                            res.text().then(txt => {
                                fetch("/uidev?ses=" + sesName + "&port=" + portnum, {
                                    method: "POST",
                                    body: txt,
                                }).then(it => location.reload())
                            })
                        }).catch((err) => {
                            console.log("Error! fetching from devserver!")
                        })
                    }
                    function clearSessions() {
                        fetch("/uidev", {method: "DELETE"}).then(it => location.reload());
                    }
                    function init() {
                        console.log('${sessionLinks}',"SESSIONLINKS")
                        document.getElementById("div-linkdump").innerHTML = '${sessionLinks}'
                    }
                </script>
                <title>NOM uidev</title>
            </head>
            <body onLoad="init()">
            <div id="div-content">
                Preview server portnumber on localhost: <br/>
                <input id="input-portnumber" value="4173"/> </br>
                Session id: <br/>
                <input id="input-sesid" value="default"/> </br>
                <button onclick="postFromDevServerToEnvServer()">Upload</button>
                <hr/>
                Pick a session here if any are uploaded <br/>
                <div id="div-linkdump">...</div>
                <hr/>
                <button onclick="clearSessions()">Clear all sessions</button>
            </div>

            </body>
            </html>`;
}


export default setupUiDevEndpoint
