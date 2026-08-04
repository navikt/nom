import type { NextFunction, Request, Response } from "express";

export function errorHandling(
  error: Error & { status: number },
  request: Request,
  response: Response,
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  next: NextFunction,
) {
  if (error.status && ("" + error.status).startsWith("4")) {
    response.status(error.status).json({
      error: "Client error",
    });
    return;
  }
  response.status(500).json({
    error: "Internal server error",
  });
  return;
}
