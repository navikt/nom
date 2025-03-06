import {
  Button,
  Checkbox,
  HStack,
  Modal,
  Table,
  TextField,
} from "@navikt/ds-react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useRef, useState } from "react";

import {
  masseflyttOrgtilknytninger,
  orgtilknytningerUtenforHierarki,
} from "~/api/apiQueries.ts";
import { NomLink } from "~/components/NomLink.tsx";
import { useTableSort } from "~/hooks/useTableSort.ts";
import { LoadingTable } from "~/sanitychecks/components/LoadingTable.tsx";
import type { OrgtilknytningerUtenforHierarkiResponse } from "~/types/nom-api/nom-api-admin-models.ts";

export function OrgtilknytningerUtenforHierarki() {
  const {
    data: dataset = [],
    isLoading,
    refetch,
  } = useQuery<OrgtilknytningerUtenforHierarkiResponse[], Error>({
    queryKey: ["API", "ORGTILKNYTNINGERUTENFORHIERARKI"],
    queryFn: async (): Promise<OrgtilknytningerUtenforHierarkiResponse[]> =>
      orgtilknytningerUtenforHierarki(),
  });

  const [filter, setFilter] = useState("");
  const [selectedRows, setSelectedRows] = useState<Set<string>>(new Set());
  const ref = useRef<HTMLDialogElement>(null);
  const [nyOrgenhetId, setNyOrgenhetId] = useState("");
  const [mutationStatus, setMutationStatus] = useState("");
  const [beholdTilganger, setBeholdTilganger] = useState(true);
  const { data, sort, handleSort } = useTableSort(
    dataset.filter((item) => item.faktiskOrgenhetId.includes(filter)),
    "enhetId",
  );

  const selectedAlternativOrgenhetIds = [...selectedRows].map(
    (id) =>
      dataset.find((item) => item.orgtilknytningFid === id)
        ?.alternativOrgenhetId,
  );

  const allSameAlternativOrgenhetId = selectedAlternativOrgenhetIds.every(
    (id) => id === selectedAlternativOrgenhetIds[0],
  );

  const masseflyttMutation = useMutation({
    mutationFn: ({
      nyOrgenhetId,
      beholdTilganger,
    }: {
      nyOrgenhetId: string;
      beholdTilganger: boolean;
    }) =>
      masseflyttOrgtilknytninger({
        orgtilknytningerFid: [...selectedRows],
        nyOrgenhetId,
        beholdTilganger,
      }),
    onSuccess: () => {
      setMutationStatus("Orgtilknytninger flyttet");
    },
    onError: () => {
      setMutationStatus("Noe feilet under flytting av orgtilknytninger");
    },
  });

  if (isLoading) {
    return <LoadingTable />;
  }

  function selectRow(OTer: OrgtilknytningerUtenforHierarkiResponse) {
    const newSelectedRows = new Set(selectedRows);
    if (newSelectedRows.has(OTer.orgtilknytningFid)) {
      newSelectedRows.delete(OTer.orgtilknytningFid);
    } else {
      newSelectedRows.add(OTer.orgtilknytningFid);
    }
    setSelectedRows(newSelectedRows);
  }

  return (
    <div>
      <div style={{ display: "flex", gap: "16px" }}>
        <TextField
          label="Filter by orgenhet"
          onChange={(e) => {
            setFilter(e.target.value);
            selectedRows.clear();
          }}
          style={{ width: "80px", paddingBottom: "8px" }}
          type="text"
          value={filter}
        />
        <TextField
          label="Selekterte rader"
          style={{ width: "80px", paddingBottom: "8px" }}
          type="text"
          value={selectedRows.size.toString()}
        />
        <Button
          disabled={!allSameAlternativOrgenhetId || selectedRows.size === 0}
          onClick={() => {
            setNyOrgenhetId(selectedAlternativOrgenhetIds[0] || "");
            ref.current?.showModal();
          }}
        >
          Flytt orgtilknytninger
        </Button>
      </div>
      <Table onSortChange={(sortKey) => handleSort(sortKey)} sort={sort}>
        <Table.Header>
          <Table.ColumnHeader scope="col">
            <input
              checked={selectedRows.size === data.length}
              onChange={(e) => {
                if (e.target.checked) {
                  setSelectedRows(
                    new Set(data.map((OTer) => OTer.orgtilknytningFid)),
                  );
                } else {
                  setSelectedRows(new Set());
                }
              }}
              type="checkbox"
            />
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="navident" sortable>
            Nav ident
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="navn" sortable>
            Navn
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="sektor" sortable>
            Sektor
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="otFid" sortable>
            Orgtilknytning
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="enhetId" sortable>
            Faktisk enhet id
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="enhetNavn" sortable>
            Faktisk enhet navn
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="remedyEnhet" sortable>
            Faktisk remedy enhet
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="gyldigFom" sortable>
            Gyldig FOM
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="gyldigTom" sortable>
            Gyldig TOM
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="otSektor" sortable>
            OT Sektor
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="otTilknytningstype" sortable>
            OT tilkn.type
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="altEnhetId" sortable>
            Alternativ enhet id
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="altEnhetNavn" sortable>
            Alternativ enhet navn
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="altRemedyEnhet" sortable>
            Alternativ remedy enhet
          </Table.ColumnHeader>
        </Table.Header>
        <Table.Body>
          {data.map((OTer) => (
            <Table.Row
              key={OTer.orgtilknytningFid}
              shadeOnHover={false}
              style={{
                backgroundColor: selectedRows.has(OTer.orgtilknytningFid)
                  ? "#e6f7ff"
                  : "transparent",
              }}
            >
              <Table.DataCell>
                <input
                  checked={selectedRows.has(OTer.orgtilknytningFid)}
                  onChange={(e) => {
                    e.stopPropagation();
                    const newSelectedRows = new Set(selectedRows);
                    if (e.target.checked) {
                      newSelectedRows.add(OTer.orgtilknytningFid);
                    } else {
                      newSelectedRows.delete(OTer.orgtilknytningFid);
                    }
                    setSelectedRows(newSelectedRows);
                  }}
                  type="checkbox"
                />
              </Table.DataCell>
              <Table.DataCell>
                <NomLink path={`ressurs/${OTer.navident}`}>
                  {OTer.navident}
                </NomLink>
              </Table.DataCell>
              <Table.DataCell
                onClick={() => {
                  selectRow(OTer);
                }}
              >
                {OTer.visningsnavn}
              </Table.DataCell>
              <Table.DataCell
                onClick={() => {
                  selectRow(OTer);
                }}
              >
                {OTer.ressursSektor}
              </Table.DataCell>
              <Table.DataCell
                onClick={() => {
                  selectRow(OTer);
                }}
              >
                {OTer.orgtilknytningFid}
              </Table.DataCell>
              <Table.DataCell>
                <input
                  checked={filter === OTer.faktiskOrgenhetId}
                  onChange={(e) => {
                    setSelectedRows(new Set());
                    if (e.target.checked) {
                      setFilter(OTer.faktiskOrgenhetId);
                    } else {
                      setFilter("");
                    }
                  }}
                  style={{ marginRight: "8px" }}
                  type="checkbox"
                />
                <NomLink path={`org/${OTer.faktiskOrgenhetId}`}>
                  {OTer.faktiskOrgenhetId}
                </NomLink>
              </Table.DataCell>
              <Table.DataCell
                onClick={() => {
                  selectRow(OTer);
                }}
              >
                {OTer.faktiskOrgenhetNavn}
              </Table.DataCell>
              <Table.DataCell
                onClick={() => {
                  selectRow(OTer);
                }}
              >
                {OTer.faktiskOrgenhetRemedyEnhet}
              </Table.DataCell>
              <Table.DataCell
                onClick={() => {
                  selectRow(OTer);
                }}
              >
                {OTer.orgtilknytningGyldigFom}
              </Table.DataCell>
              <Table.DataCell
                onClick={() => {
                  selectRow(OTer);
                }}
              >
                {OTer.orgtilknytningGyldigTom}
              </Table.DataCell>
              <Table.DataCell
                onClick={() => {
                  selectRow(OTer);
                }}
              >
                {OTer.orgtilknytningSektor}
              </Table.DataCell>
              <Table.DataCell
                onClick={() => {
                  selectRow(OTer);
                }}
              >
                {OTer.orgtilknytningType}
              </Table.DataCell>
              <Table.DataCell>
                {OTer.alternativOrgenhetId && (
                  <NomLink path={`org/${OTer.alternativOrgenhetId}`}>
                    {OTer.alternativOrgenhetId}
                  </NomLink>
                )}
              </Table.DataCell>
              <Table.DataCell
                onClick={() => {
                  selectRow(OTer);
                }}
              >
                {OTer.alternativOrgenhetNavn}
              </Table.DataCell>
              <Table.DataCell
                onClick={() => {
                  selectRow(OTer);
                }}
              >
                {OTer.alternativOrgenhetRemedyEnhet}
              </Table.DataCell>
            </Table.Row>
          ))}
        </Table.Body>
      </Table>
      <Modal
        header={{ heading: "Masseflytt orgtilknytninger" }}
        onClose={() => {
          if (mutationStatus === "Orgtilknytninger flyttet") {
            setSelectedRows(new Set());
            ref.current?.close();
            setMutationStatus("");
            setBeholdTilganger(true);
            setFilter("");
            refetch();
          }
        }}
        ref={ref}
        width="400px"
      >
        <Modal.Body>
          <TextField
            label={`Flytt ${selectedRows.size} OTer til enhet`}
            onChange={(e) => setNyOrgenhetId(e.target.value)}
            style={{ width: "80px", paddingBottom: "8px" }}
            type="text"
            value={nyOrgenhetId}
          />
          <Checkbox
            checked={beholdTilganger}
            onChange={(e) => setBeholdTilganger(e.target.checked)}
          >
            Behold tilganger
          </Checkbox>
          <HStack gap="2" justify="end">
            <Button
              disabled={nyOrgenhetId === ""}
              onClick={() =>
                masseflyttMutation.mutate({ nyOrgenhetId, beholdTilganger })
              }
              style={{ marginTop: "16px" }}
            >
              Bekreft
            </Button>
            <Button
              onClick={() => ref.current?.close()}
              style={{ marginTop: "16px" }}
            >
              Avbryt
            </Button>
          </HStack>
          <p>{mutationStatus}</p>
        </Modal.Body>
      </Modal>
    </div>
  );
}
