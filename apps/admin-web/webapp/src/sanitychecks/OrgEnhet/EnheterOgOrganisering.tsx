import { List } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";

import { enheterOgOrganisering } from "~/api/queries.ts";

export const EnheterOgOrganisering = () => {
  const orgenheterOgOrganisering = useQuery(enheterOgOrganisering());
  return (
    <List as="ul" title="Enheter og Organisering">
      <List.Item>
        Totalt antall aktive enheter:{" "}
        {orgenheterOgOrganisering.data?.aktiveEnheter}
      </List.Item>
      <List.Item>
        Antall aktive organiseringer:{" "}
        {orgenheterOgOrganisering.data?.aktiveOrganiseringer}
      </List.Item>
      <List.Item>
        Antall over enheter: {orgenheterOgOrganisering.data?.antallOverEnheter}
      </List.Item>
      <List.Item>
        Antall under enheter:{" "}
        {orgenheterOgOrganisering.data?.antallUnderEnheter}
      </List.Item>
      <List.Item>
        Antall aktive enheter uten over enheter:{" "}
        {orgenheterOgOrganisering.data?.aktiveEnheterUtenOverEnheter}
      </List.Item>
      <List.Item>
        Antall aktive enheter uten under enheter:{" "}
        {orgenheterOgOrganisering.data?.aktiveEnheterUtenUnderEnheter}
      </List.Item>
      <List.Item>
        Antall aktive enheter med under enheter, men uten over enheter:{" "}
        {
          orgenheterOgOrganisering.data
            ?.aktiveEnheterMedUnderEnheterUtenOverEnheter
        }
      </List.Item>
      <List.Item>
        Antall aktive enheter uten noen organisering:{" "}
        {orgenheterOgOrganisering.data?.aktiveEnheterUtenNoenOrganisering}
      </List.Item>
    </List>
  );
};
