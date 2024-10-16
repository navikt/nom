import type { AlertProps } from "@navikt/ds-react";
import { Alert } from "@navikt/ds-react";
import toast from "react-hot-toast";

export function showToast({
  text,
  variant = "success",
}: {
  text: string;
  variant?: AlertProps["variant"];
}) {
  toast(
    <Alert style={{ margin: "-12px -20px" }} variant={variant}>
      {text}
    </Alert>,
  );
}
