import { css } from "@emotion/react";
import {
  PadlockLockedFillIcon,
  PadlockUnlockedFillIcon,
} from "@navikt/aksel-icons";
import type { SortState } from "@navikt/ds-react";
import {
  Button,
  Loader,
  Pagination,
  Select,
  Table,
  TextField,
  UNSAFE_Combobox,
} from "@navikt/ds-react";
import axios from "axios";
import { isBefore } from "date-fns";
// eslint-disable-next-line lodash/import-scope,you-dont-need-lodash-underscore/is-string,you-dont-need-lodash-underscore/uniq
import { isString, uniq } from "lodash";
import sortBy from "lodash/sortBy";
import { useEffect, useState } from "react";
import { useMutation } from "react-query";
import { useSearchParams } from "react-router-dom";

import { apolloClient } from "~/api/apolloclient";
import type { OrgEnhet } from "~/api/generated/graphql_generated";
import {
  NomNivaa,
  OrgEnhetsType,
  useGetAllOrgenheterAdminQuery,
} from "~/api/generated/graphql_generated";
import { showToast } from "~/hooks/showToast";

type AdminFilter = {
  id?: string;
  orgEnhetsType?: string;
  navn?: string;
  orgNiv?: string;
  remedyEnhetId?: string;
  nomNivaa?: string;
};

export function AdminPage() {
  const getAllOrgenheterQuery = useGetAllOrgenheterAdminQuery();
  const [filter, setFilter] = useState<AdminFilter>({});
  const [page, setPage] = useState(1);
  const [sort, setSort] = useState<SortState | undefined>({
    orderBy: "id",
    direction: "ascending",
  });

  const rowsPerPage = 50;

  useEffect(() => {
    setPage(1);
  }, [filter]);

  const handleSort = (sortKey: string | undefined) => {
    if (sortKey) {
      setSort({
        orderBy: sortKey,
        direction:
          sort && sortKey === sort.orderBy && sort.direction === "ascending"
            ? "descending"
            : "ascending",
      });
    } else {
      // eslint-disable-next-line unicorn/no-useless-undefined
      setSort(undefined);
    }
  };

  const orgEnheter =
    getAllOrgenheterQuery.data?.orgEnheter
      .map(({ orgEnhet }) => orgEnhet as OrgEnhet)
      .filter((oe) => oe.leder.length > 0)
      .filter(
        (oe) =>
          oe.gyldigTom === null || isBefore(new Date(), new Date(oe.gyldigTom)),
      ) ?? [];

  if (getAllOrgenheterQuery.loading) {
    return (
      <div
        css={css`
          display: flex;
          flex-direction: column;
          align-items: center;
          padding-top: 200px;
        `}
      >
        <Loader size="large" />
        <span>Laster orgenheter ...</span>
      </div>
    );
  }

  if (orgEnheter.length === 0) {
    return;
  }

  const filteredOrgEnheter = filterOrgEnheter(orgEnheter, filter);

  const sortedOrgEnheter = sort
    ? sortOrgEnheter({ orgEnheter: filteredOrgEnheter, sort })
    : filteredOrgEnheter;

  const orgEnheterToDisplay = sortedOrgEnheter.slice(
    (page - 1) * rowsPerPage,
    page * rowsPerPage,
  );

  const existingOrgNivs = uniq(orgEnheter.map((oe) => oe.orgNiv));

  return (
    <div
      css={css`
        padding: 2rem;
        background: white;
      `}
    >
      <div
        css={css`
          display: grid;
          gap: 1rem;
          grid-template-columns: repeat(3, 1fr);
        `}
      >
        <UNSAFE_Combobox
          label="Orgniv"
          onToggleSelected={(option, isSelected) => {
            setFilter((filter) => ({
              ...filter,
              orgNiv: isSelected ? option : undefined,
            }));
          }}
          options={existingOrgNivs}
          shouldAutocomplete={true}
        />
        <UNSAFE_Combobox
          label="OrgEnhetsType"
          onToggleSelected={(option, isSelected) => {
            setFilter((filter) => ({
              ...filter,
              orgEnhetsType: isSelected ? option : undefined,
            }));
          }}
          options={Object.values(OrgEnhetsType)}
          shouldAutocomplete={true}
        />
        <UNSAFE_Combobox
          label="NomNivaa"
          onToggleSelected={(option, isSelected) => {
            setFilter((filter) => ({
              ...filter,
              nomNivaa: isSelected ? option : undefined,
            }));
          }}
          options={Object.values(NomNivaa)}
          shouldAutocomplete={true}
        />
        <TextField
          label="Navn"
          onChange={(event) =>
            setFilter((filter) => ({ ...filter, navn: event.target.value }))
          }
        />
        <TextField
          label="Id"
          onChange={(event) =>
            setFilter((filter) => ({ ...filter, id: event.target.value }))
          }
        />
        <TextField
          label="remedyEnhetId"
          onChange={(event) =>
            setFilter((filter) => ({
              ...filter,
              remedyEnhetId: event.target.value,
            }))
          }
        />
      </div>

      <div
        css={css`
          padding: 1rem 0;
          display: flex;
          align-items: center;
          gap: 1rem;
        `}
      >
        <ToggleEditMode />
        <b>Treff: {sortedOrgEnheter.length}</b>
      </div>
      <Table
        onSortChange={(sortKey) => handleSort(sortKey)}
        size="small"
        sort={sort}
        zebraStripes
      >
        <Table.Header>
          <Table.Row>
            <Table.ColumnHeader scope="col" sortKey="id" sortable>
              Id
            </Table.ColumnHeader>
            <Table.ColumnHeader scope="col" sortKey="orgEnhetsType" sortable>
              OrgEnhetsType
            </Table.ColumnHeader>
            <Table.ColumnHeader scope="col" sortKey="navn" sortable>
              Navn
            </Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Gyldig fra</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Gyldig til</Table.ColumnHeader>
            <Table.ColumnHeader scope="col" sortKey="agressoId" sortable>
              agressoId
            </Table.ColumnHeader>
            <Table.ColumnHeader scope="col" sortKey="orgNiv" sortable>
              Orgniv
            </Table.ColumnHeader>
            <Table.ColumnHeader scope="col" sortKey="remedyEnhetId" sortable>
              remedyEnhetId
            </Table.ColumnHeader>
            <Table.ColumnHeader scope="col" sortKey="nomNivaa" sortable>
              nomNivaa
            </Table.ColumnHeader>
            <Table.ColumnHeader
              scope="col"
              sortKey="agressoOrgenhetType"
              sortable
            >
              agressoOrgenhetType
            </Table.ColumnHeader>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {orgEnheterToDisplay.map((orgEnhet, index) => {
            return (
              <Table.Row key={index}>
                <Table.HeaderCell scope="row">{orgEnhet?.id}</Table.HeaderCell>
                <OrgEnhetsTypeDataCell orgEnhet={orgEnhet} />
                <Table.DataCell>{orgEnhet?.navn}</Table.DataCell>
                <Table.DataCell>{orgEnhet?.gyldigFom}</Table.DataCell>
                <Table.DataCell>{orgEnhet?.gyldigTom}</Table.DataCell>
                <Table.DataCell>{orgEnhet?.agressoId}</Table.DataCell>
                <Table.DataCell>{orgEnhet?.orgNiv}</Table.DataCell>
                <Table.DataCell>{orgEnhet?.remedyEnhetId}</Table.DataCell>
                <Table.DataCell>{orgEnhet?.nomNivaa}</Table.DataCell>
                <Table.DataCell>{orgEnhet?.agressoOrgenhetType}</Table.DataCell>
              </Table.Row>
            );
          })}
        </Table.Body>
      </Table>
      <Pagination
        count={Math.ceil(filteredOrgEnheter.length / rowsPerPage)}
        css={css`
          padding: 2rem;
        `}
        hidden={filteredOrgEnheter.length <= rowsPerPage}
        onPageChange={setPage}
        page={page}
      />
    </div>
  );
}

function OrgEnhetsTypeDataCell({ orgEnhet }: { orgEnhet: OrgEnhet }) {
  const [searchParameters] = useSearchParams();
  const updateOrgEnhetsTypeMutation = useMutation(
    (newOrgEnhetsType: OrgEnhetsType | null) =>
      axios.post("/nom-api/app/orgenhet/orgEnhetsType", {
        nomId: orgEnhet.id,
        orgEnhetsType: newOrgEnhetsType,
      }),
    {
      onSuccess: (_, newOrgEnhetsType) => {
        /*
         When this mutation returns we have only sent a message on kafka to change its type. It is not immediately available in orgEnhet-state.
         Therefore, we do a manual optimistic update of the orgEnhetsType for this orgEnhet in the cache.
        */
        apolloClient.cache.modify({
          id: apolloClient.cache.identify(orgEnhet),
          fields: {
            orgEnhetsType: () => newOrgEnhetsType,
          },
        });
        showToast({ text: "OrgEnhetsType endret" });
      },

      onError: () =>
        showToast({ text: "OrgEnhetsType endring feilet", variant: "error" }),
    },
  );
  return (
    <Table.DataCell>
      <Select
        hideLabel
        label=""
        onChange={(event) => {
          const orgEnhetsType =
            event.target.value === ""
              ? null
              : (event.target.value as OrgEnhetsType);
          updateOrgEnhetsTypeMutation.mutate(orgEnhetsType);
        }}
        readOnly={searchParameters.get("allowEdit") !== "true"}
        size="small"
        value={orgEnhet?.orgEnhetsType ?? ""}
      >
        <option value=""></option>
        {Object.values(OrgEnhetsType).map((orgEnhetsType) => (
          <option key={orgEnhetsType} value={orgEnhetsType}>
            {orgEnhetsType}
          </option>
        ))}
      </Select>
    </Table.DataCell>
  );
}

function ToggleEditMode() {
  const [searchParameters, setSearchParameters] = useSearchParams();

  const allowEdit = searchParameters.get("allowEdit") === "true";

  if (allowEdit) {
    return (
      <Button
        icon={<PadlockUnlockedFillIcon aria-hidden />}
        onClick={() => setSearchParameters({ allowEdit: "false" })}
        variant="secondary"
      />
    );
  }

  return (
    <Button
      icon={<PadlockLockedFillIcon aria-hidden />}
      onClick={() => setSearchParameters({ allowEdit: "true" })}
      variant="secondary"
    />
  );
}

function sortOrgEnheter({
  orgEnheter,
  sort,
}: {
  orgEnheter: OrgEnhet[];
  sort: SortState;
}) {
  const { orderBy, direction } = sort;

  const sortedMembersAscending = sortBy(orgEnheter, orderBy);
  const reversed = direction === "descending";

  return reversed ? sortedMembersAscending.reverse() : sortedMembersAscending;
}

function filterOrgEnheter(orgEnheter: OrgEnhet[], adminFilter: AdminFilter) {
  return orgEnheter.filter((orgEnhet) =>
    Object.entries(adminFilter)
      .filter(([, filterValue]) => filterValue !== undefined)
      .every(([filterKey, filterValue]) => {
        const orgEnhetValue = orgEnhet[filterKey as keyof OrgEnhet];

        if (isString(orgEnhetValue)) {
          return orgEnhetValue
            .toString()
            .toUpperCase()
            .startsWith(filterValue.toUpperCase());
        }
        return orgEnhetValue === filterValue;
      }),
  );
}
