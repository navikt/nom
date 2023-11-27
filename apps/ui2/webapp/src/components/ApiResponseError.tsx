import { Alert } from "@navikt/ds-react";
import type { AxiosError } from "axios";
import React from "react";

import type { StandardResponse } from "~/types/api-models";

export function ApiResponseError({
  error,
}: {
  error: AxiosError<StandardResponse> | null;
}) {
  const hasMessage = error?.response?.data.message;
  if (!hasMessage) {
    return <></>;
  }

  return <Alert variant="error">{hasMessage}</Alert>;
}
