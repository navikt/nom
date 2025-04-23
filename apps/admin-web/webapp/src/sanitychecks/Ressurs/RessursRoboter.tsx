import { Table } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";

import { getRobotRessurser } from "~/api/queries.ts";
import { NomLink } from "~/components/NomLink.tsx";
import { useTableSort } from "~/hooks/useTableSort.ts";
import { EmptyTable } from "~/sanitychecks/components/EmptyTable.tsx";
import { LoadingTable } from "~/sanitychecks/components/LoadingTable.tsx";
import type {
  RobotOrgtilknytning,
  RobotRessurs,
} from "~/types/nom-admin/nom-admin-models.ts";

export function RessursRoboter() {
  const {
    data: robotRessurser = [],
    isLoading,
    isError,
  } = useQuery<RobotRessurs[], Error>({
    queryKey: ["API", "RESSURS_ROBOTER"],
    queryFn: async (): Promise<RobotRessurs[]> => getRobotRessurser(),
  });

  const { data, sort, handleSort } = useTableSort(
    robotRessurser ?? [],
    "navident",
  );

  if (isLoading) {
    return <LoadingTable />;
  }

  if (robotRessurser.length === 0 && !isError) {
    return <EmptyTable />;
  }

  return (
    <div>
      <Table onSortChange={(sortKey) => handleSort(sortKey)} sort={sort}>
        <Table.Header>
          <Table.ColumnHeader scope="col" sortKey="navident" sortable>
            Navident
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col">Visningsnavn</Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="sektor" sortable>
            Sektor
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="startdato" sortable>
            Startdato
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="sluttdato" sortable>
            Sluttdato
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col">Orgtilknytninger</Table.ColumnHeader>
        </Table.Header>
        <Table.Body>
          {data.map((robot) => (
            <Table.Row key={robot.navident} shadeOnHover={false}>
              <Table.DataCell>
                <NomLink path={`ressurs/${robot.navident}`}>
                  {robot.navident}
                </NomLink>
              </Table.DataCell>
              <Table.DataCell>{robot.visningsnavn}</Table.DataCell>
              <Table.DataCell>{robot.sektor}</Table.DataCell>
              <Table.DataCell>{robot.startdato}</Table.DataCell>
              <Table.DataCell>{robot.sluttdato}</Table.DataCell>
              <Table.DataCell>
                {robot.orgtilknytninger?.map((orgtilknytning) => (
                  <RobotOrgtilknytningComposite
                    key={robot.navident + orgtilknytning.orgenhetId}
                    ot={orgtilknytning}
                  />
                ))}
              </Table.DataCell>
            </Table.Row>
          ))}
        </Table.Body>
      </Table>
    </div>
  );
}

function RobotOrgtilknytningComposite({ ot }: { ot: RobotOrgtilknytning }) {
  return (
    <div>
      <NomLink path={`org/${ot.orgenhetId}`}>{ot.orgenhetId}</NomLink>
      {ot.orgenhetNavn} [{ot.gyldigFom}:{ot.gyldigTom ?? " "}]
    </div>
  );
}
