import { Heading, Loader } from "@navikt/ds-react";
export function LoadingTable() {
  return (
    <>
      <Heading
        align="center"
        size="medium"
        style={{ display: "grid", justifyContent: "center" }}
      >
        Laster inn tabellen...
        <br />
        Dette kan ta opptil et minutt...
      </Heading>
      <div
        style={{
          display: "grid",
          justifyContent: "center",
          paddingTop: "30px",
        }}
      >
        <Loader size="3xlarge" variant="neutral" />
      </div>
    </>
  );
}
