import { Heading } from "@navikt/ds-react";

export function EmptyTable() {
  return (
    <div>
      <Heading
        size="xlarge"
        style={{
          justifyContent: "center",
          display: "flex",
          paddingTop: "30xp",
        }}
      >
        Listen er tom
      </Heading>
    </div>
  );
}
