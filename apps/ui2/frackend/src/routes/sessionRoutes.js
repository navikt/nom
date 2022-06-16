export function setupSessionRoutes(app) {
  app.get('/sessionInfo', (req, res) => {
    const navIdent = req.session?.passport?.user?.claims?.NAVident

    res.json(navIdent ? navIdent : '')
  })
}
