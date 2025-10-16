import express from "express";

const staticGraphiqlHandler = express.static("./graphiqlbundle");

export function setupPlayground(app: express.Express) {
  app.use("/playground", staticGraphiqlHandler);
}
