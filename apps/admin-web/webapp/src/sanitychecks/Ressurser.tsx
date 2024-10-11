import { Heading, List, Loader } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";

import { getRessurserSanityCheck } from "~/api/queries.ts";

export const Ressurser = () => {
  const ressurser = useQuery(getRessurserSanityCheck());

  if (ressurser.isLoading) {
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

  return (
    <List as="ul" title="Ressurser">
      <List.Item>
        Totalt antall ressurser: {ressurser.data?.totaltAntallRessurser}
      </List.Item>
      <List.Item>
        Antall aktive OT: {ressurser.data?.antallAktiveOrgtilknytninger}
      </List.Item>
      <List.Item>
        Antall ressurs med aktiv OT:{" "}
        {ressurser.data?.ressurserMedAktiveOrgtilknytninger}
      </List.Item>
      <List.Item>
        Antall ressurser uten aktiv OT:{" "}
        {ressurser.data?.ressurserUtenAktiveOrgtilknytninger}
      </List.Item>
      <List.Item>
        Antall ressurs med aktiv OT for enhet som er inaktiv eller uten
        organisering:{" "}
        {
          ressurser.data
            ?.ressurserMedAktiveOrgtilknytningerForEnhetSomErInaktivEllerUtenOrganisering
        }
      </List.Item>
      <List.Item>
        Antall ressurser med OT utover enhetens gyldige periode:{" "}
        {ressurser.data?.ressursMedTilknytningerUtoverEnhetGyldigPeriode}
      </List.Item>
    </List>
  );
};
