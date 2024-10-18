import { Button, Heading, List, Loader } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import { useState } from "react";

import { getRessurserSanityCheck } from "~/api/queries.ts";
import type { RessursOrgtilknytningKoblingInfo } from "~/types/nom-admin/nom-admin-models.ts";

export const Ressurser = () => {
  const [fetchData, setFetchData] = useState(false);
  const {
    data: ressurser,
    isLoading,
    isError,
  } = useQuery<RessursOrgtilknytningKoblingInfo>({
    queryKey: ["API", "RESSURSERSANITYCHECK"],
    queryFn: async (): Promise<RessursOrgtilknytningKoblingInfo> =>
      getRessurserSanityCheck(),
    enabled: fetchData,
  });

  if (isLoading) {
    return (
      <>
        <Heading
          size="medium"
          style={{ display: "grid", justifyContent: "center" }}
        >
          Laster ressurser sanity check...
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
  if (ressurser === undefined && !isError) {
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
          Ingen data eller ikke hentet
        </Heading>
        <div
          style={{
            justifyContent: "center",
            display: "flex",
            paddingTop: "50px",
          }}
        >
          <Button onClick={() => setFetchData(true)}>Hent data</Button>
        </div>
      </div>
    );
  }

  return (
    <List as="ul" title="Ressurser">
      <List.Item>
        Totalt antall ressurser: {ressurser?.totaltAntallRessurser}
      </List.Item>
      <List.Item>
        Antall aktive OT: {ressurser?.antallAktiveOrgtilknytninger}
      </List.Item>
      <List.Item>
        Antall ressurs med aktiv OT:{" "}
        {ressurser?.ressurserMedAktiveOrgtilknytninger}
      </List.Item>
      <List.Item>
        Antall ressurser uten aktiv OT:{" "}
        {ressurser?.ressurserUtenAktiveOrgtilknytninger}
      </List.Item>
      <List.Item>
        Antall ressurs med aktiv OT for enhet som er inaktiv eller uten
        organisering:{" "}
        {
          ressurser?.ressurserMedAktiveOrgtilknytningerForEnhetSomErInaktivEllerUtenOrganisering
        }
      </List.Item>
      <List.Item>
        Antall ressurser med OT utover enhetens gyldige periode:{" "}
        {ressurser?.ressursMedTilknytningerUtoverEnhetGyldigPeriode}
      </List.Item>
    </List>
  );
};
