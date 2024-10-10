import { List } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";

import { getAlleEnheter } from "~/api/queries.ts";

export const AntallOrgenheter = () => {
  const antallOrgenheter = useQuery(getAlleEnheter());
  return (
    <List as="ul" title="Antall OrgEnheter, med dato">
      <List.Item>
        Totalt antall enheter: {antallOrgenheter.data?.totaltAntallEnheter}
      </List.Item>
      <List.Item>
        Antall enheter med startdato:{" "}
        {antallOrgenheter.data?.antallEnheterMedStartdato}
      </List.Item>
      <List.Item>
        Antall enheter med sluttdato:{" "}
        {antallOrgenheter.data?.antallEnheterMedSluttdato}
      </List.Item>
      <List.Item>
        Antall enheter med sluttdato før startdato:{" "}
        {antallOrgenheter.data?.antallEnheterMedSluttdatoFoerStartdato}
      </List.Item>
    </List>
  );
};
