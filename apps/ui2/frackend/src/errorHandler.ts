import axios from "axios";
import type { NextFunction, Request, Response } from "express";

export function errorHandling(
  error: Error & { status: number },
  request: Request,
  response: Response,
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  next: NextFunction,
) {
  if (axios.isAxiosError(error)) {
    console.error(error.response?.data);
  }
  if (error.status && ("" + error.status).startsWith("4")) {
    return response.status(error.status).json({
      error: "Client error",
    });
  }
  return response.status(500).json({
    error: "Internal server error",
  });
}
