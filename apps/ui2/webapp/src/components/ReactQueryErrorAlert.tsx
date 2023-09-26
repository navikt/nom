import { Alert } from "@navikt/ds-react";
import type { AxiosError } from "axios";

export function ReactQueryErrorAlert({
  error,
}: {
  error: AxiosError<Error> | null;
}) {
  if (error === null) {
    return <></>;
  }
  return <Alert variant="error">{error.message}</Alert>;
}
