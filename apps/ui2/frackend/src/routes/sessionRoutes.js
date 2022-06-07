export function setupSessionRoutes(app) {
    app.get('/sessionInfo', (req, res) => {
        const name = req.session?.passport?.user?.claims?.name
        res.json( name ? name : "" )
    })
}