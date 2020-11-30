const { createProxyMiddleware } = require("http-proxy-middleware");

const BACKEND_PORT = "8080";

module.exports = function (app) {
  app.use(
    "/api",
    createProxyMiddleware({
      target: "http://localhost:" + BACKEND_PORT,
      changeOrigin: true,
    })
  );
  app.use("/login", createProxyMiddleware({
    target: "http://localhost:" + BACKEND_PORT,
      changeOrigin: true
  }));

  app.use("/secret", createProxyMiddleware({
    target: "http://localhost:" + BACKEND_PORT,
    changeOrigin: true
}));
};
