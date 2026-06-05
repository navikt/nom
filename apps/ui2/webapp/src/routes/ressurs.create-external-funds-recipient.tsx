import { createFileRoute } from "@tanstack/react-router";

import { CreateExternalFundsRecipientPage } from "~/pages/CreateResourcePage/CreateExternalFundsRecipientPage";

export const Route = createFileRoute(
  "/ressurs/create-external-funds-recipient",
)({
  component: () => <CreateExternalFundsRecipientPage />,
});
