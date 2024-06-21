import { NavidentNameLookup } from "~/components/NavidentNameLookup.tsx";
import type { MetaData } from "~/types.ts";

export function EventOrigination({ metadata }: { metadata: MetaData }) {
  return (
    <>
      Utført av{" "}
      <b>
        <NavidentNameLookup
          navident={metadata.inputMetadata.origination.initiatedBy}
        />{" "}
      </b>
      fra <b>{metadata.inputMetadata.origination.initiatedFrom}</b>
    </>
  );
}
