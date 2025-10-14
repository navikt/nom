import express from "express";

export function setupPlayground(app: express.Express) {
  app.use("/playground", express.static("./graphiqlbundle"));
}

export function setupActuators(app: express.Express) {
  app.get("/internal/health/liveness", (request, response) => {
    response.send({
      status: "UP",
    });
  });
  console.log("Liveness available on /internal/health/liveness");

  app.get("/internal/health/readiness", (request, response) => {
    response.send({
      status: "UP",
    });
  });
  console.log("Readiness available on /internal/health/readiness");
}
