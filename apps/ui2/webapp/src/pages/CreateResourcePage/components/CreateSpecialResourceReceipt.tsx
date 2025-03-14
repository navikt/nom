import { useSearch } from "@tanstack/react-router";

import type { StatligFormValues } from "~/pages/CreateResourcePage/CreateResourcePageRoutes";
import { ReceiptWrapper } from "~/pages/CreateResourcePage/SummaryStep";

export function CreateSpecialResourceReceipt() {
  const { navident } = useSearch({ from: "/ressurs/specialreceipt" });

  return <ReceiptWrapper navident={navident} />;
}

export function validateCreateSpecialReceipt(
  search: Record<string, unknown>,
): Partial<StatligFormValues> & { navident?: string } {
  return {
    navident: String(search.navident),
  };
}
