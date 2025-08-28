import { FileExcelIcon } from "@navikt/aksel-icons";
import { Button } from "@navikt/ds-react";

import { exportSanityCheckToExcel } from "~/utils.ts";

export function AntallOgExcelEksportKnapp({
  antall,
  exportName,
  exportType,
}: {
  antall: number | undefined;
  exportName: string | undefined;
  exportType: "orgEnhet" | "ressurs" | "orgtilknytning" | "RemedyCompare";
}) {
  const handleExport = async () => {
    await exportSanityCheckToExcel(exportType, exportName);
  };

  return (
    <div style={{ display: "flex", justifyContent: "space-between" }}>
      <h2 style={{ paddingBottom: "20px" }}>Antall rader: {antall}</h2>
      <Button
        icon={<FileExcelIcon />}
        onClick={handleExport}
        variant="secondary"
      >
        Til Excel
      </Button>
    </div>
  );
}
