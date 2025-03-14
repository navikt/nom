import { createFileRoute } from "@tanstack/react-router";

import {
  CreateSpecialResourceReceipt,
  validateCreateSpecialReceipt,
} from "~/pages/CreateResourcePage/components/CreateSpecialResourceReceipt";

export const Route = createFileRoute("/ressurs/specialreceipt")({
  component: CreateSpecialResourceReceipt,
  validateSearch: validateCreateSpecialReceipt,
});
