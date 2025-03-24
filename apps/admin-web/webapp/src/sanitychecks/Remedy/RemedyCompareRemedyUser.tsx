import { TextField } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import { useState } from "react";

import { getImportedRemedyUser } from "~/api/queries.ts";

export function RemedyCompareRemedyUser() {
  const [navident, setNavident] = useState("");

  const remedyUserQuery = useQuery({
    ...getImportedRemedyUser(navident || ""),
    enabled: navident.length === 7,
    select: (result) => result,
  });

  const importedRemedyUser = remedyUserQuery.data;

  return (
    <div>
      <TextField
        label={"Navident"}
        onChange={(e) => setNavident(e.target.value)}
        style={{ width: "150px", paddingBottom: "8px" }}
        type="text"
        value={navident}
      />
      {importedRemedyUser && (
        <div>
          <h3>Imported Remedy User:</h3>
          <pre>{JSON.stringify(importedRemedyUser, null, 2)}</pre>
        </div>
      )}
    </div>
  );
}
