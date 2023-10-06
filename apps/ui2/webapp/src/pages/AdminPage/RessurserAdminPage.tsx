import "react-json-view-lite/dist/index.css";

import { css } from "@emotion/react";
import {
  Alert,
  Checkbox,
  Chips,
  Loader,
  Pagination,
  Table,
} from "@navikt/ds-react";
import { useNavigate, useSearch } from "@tanstack/react-router";
import { useState } from "react";
import { allExpanded, defaultStyles, JsonView } from "react-json-view-lite";
import { useQuery } from "react-query";

import { Sektor } from "~/api/generated/graphql_generated";
import type { RessursFields } from "~/api/react-query-endpoints";
import {
  getAlleRessurser,
  getUnderliggendeOrgenheter,
} from "~/api/react-query-endpoints";
import {
  BasicSelect,
  SelectLayoutWrapper,
} from "~/components/select/CustomSelectComponents";
import { OrgEnhetSearch } from "~/pages/AdminPage/components/OrgEnhetSearch";
import { ressurserAdminRoute } from "~/reactRouterRoutes";
import { isGyldighetsPeriodeActive } from "~/utils";

export function RessurserAdminPage() {
  const ressursFilterValues = useSearch({ from: ressurserAdminRoute.id });
  // Using react query instead of apollo because it runs  more smoothly
  const alleRessurser = useQuery<RessursFields[], unknown>({
    ...getAlleRessurser,
  });

  const underliggendeOrgenheter = useQuery<string[], unknown>({
    queryKey: getUnderliggendeOrgenheter.queryKey(
      ressursFilterValues.orgEnhet as string,
    ),
    queryFn: () =>
      getUnderliggendeOrgenheter.queryFn(
        ressursFilterValues.orgEnhet as string,
      ),
    enabled: Boolean(ressursFilterValues.orgEnhet),
  });

  const [page, setPage] = useState(1);

  if (alleRessurser.isLoading) {
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
        <RessursFilter />
        <FilterChips />
        <span>Laster ressurser ...</span>
      </div>
    );
  }

  if (alleRessurser.data === undefined) {
    return (
      <div
        css={css`
          display: flex;
          flex-direction: column;
          align-items: center;
          padding-top: 200px;
        `}
      >
        <Alert variant="error">Noe gikk galt</Alert>
      </div>
    );
  }

  const rowsPerPage = 50;
  const filteredRessurser = alleRessurser.data.filter((ressurs) =>
    applyFilter(
      ressurs,
      ressursFilterValues,
      underliggendeOrgenheter.data ?? [],
    ),
  );

  const ressurserToDisplay = filteredRessurser.slice(
    (page - 1) * rowsPerPage,
    page * rowsPerPage,
  );

  return (
    <div
      css={css`
        padding: 2rem;
        background: white;
        display: flex;
        flex-direction: column;
        gap: var(--a-spacing-4);
      `}
    >
      <RessursFilter />
      <FilterChips />
      <b>Treff: {filteredRessurser.length}</b>
      <Table>
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell />
            <Table.ColumnHeader scope="col">Navident</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Visningsnavn</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Sektor</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Org Tilknytning</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Ledere</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Sluttdato</Table.ColumnHeader>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {ressurserToDisplay.map((ressurs, index) => (
            <Table.ExpandableRow
              content={<FullRessursInfo ressurs={ressurs} />}
              key={index}
            >
              <Table.HeaderCell scope="row">
                {ressurs.navident}
              </Table.HeaderCell>
              <Table.DataCell>{ressurs.visningsnavn}</Table.DataCell>
              <Table.DataCell>{ressurs.sektor}</Table.DataCell>
              <OrgTilknytningDataCell
                orgTilknytninger={ressurs.orgTilknytning}
              />
              <LedereDataCell ledere={ressurs.ledere} />
              <Table.DataCell>{ressurs.sluttdato}</Table.DataCell>
            </Table.ExpandableRow>
          ))}
        </Table.Body>
      </Table>
      <Pagination
        count={Math.ceil(filteredRessurser.length / rowsPerPage)}
        css={css`
          padding: 2rem;
        `}
        hidden={filteredRessurser.length <= rowsPerPage}
        onPageChange={setPage}
        page={page}
      />
    </div>
  );
}

function OrgTilknytningDataCell({
  orgTilknytninger,
}: {
  orgTilknytninger: {
    orgEnhet: {
      id: string;
      navn: string;
    };
    gyldigFom: string;
    gyldigTom: string;
  }[];
}) {
  const activeOrgtilknytninger = orgTilknytninger.filter((orgTilknytning) =>
    isGyldighetsPeriodeActive(
      orgTilknytning.gyldigFom,
      orgTilknytning.gyldigTom,
    ),
  );

  return (
    <Table.DataCell>
      {pluralize(
        activeOrgtilknytninger.length,
        activeOrgtilknytninger[0]?.orgEnhet.navn,
        `${activeOrgtilknytninger[0]?.orgEnhet.navn} (+${
          activeOrgtilknytninger.length - 1
        })`,
      )}
    </Table.DataCell>
  );
}

function LedereDataCell({
  ledere,
}: {
  ledere: {
    ressurs: {
      navident: string;
      visningsnavn: string;
    };
    gyldigFom: string;
    gyldigTom: string;
  }[];
}) {
  const activeLedere = ledere.filter((leder) =>
    isGyldighetsPeriodeActive(leder.gyldigFom, leder.gyldigTom),
  );

  return (
    <Table.DataCell>
      {pluralize(
        activeLedere.length,
        `${activeLedere[0]?.ressurs.visningsnavn} (${activeLedere[0]?.ressurs.navident})`,
        `${activeLedere[0]?.ressurs.visningsnavn} (${activeLedere[0]?.ressurs
          .navident}) (+${activeLedere.length - 1})`,
      )}
    </Table.DataCell>
  );
}

function pluralize(
  numberElements: number,
  singleForm?: string,
  pluralForm?: string,
) {
  if (numberElements === 1) {
    return singleForm;
  }
  if (numberElements > 1) {
    return pluralForm;
  }
}

function RessursFilter() {
  const ressursFilterValues = useSearch({ from: ressurserAdminRoute.id });
  const navigate = useNavigate();

  return (
    <div
      css={css`
        display: flex;
        gap: var(--a-spacing-4);
        align-items: end;
      `}
    >
      <OrgEnhetSearch />
      <SektorSelect />
      <Checkbox
        checked={ressursFilterValues.includeInactive}
        onChange={() =>
          navigate({
            search: {
              ...ressursFilterValues,
              includeInactive: !ressursFilterValues.includeInactive,
            },
          })
        }
      >
        <small>Inkluder inaktive ressurser</small>
      </Checkbox>
    </div>
  );
}

function SektorSelect() {
  const ressursFilterValues = useSearch({ from: ressurserAdminRoute.id });
  const navigate = useNavigate();

  const options = Object.entries(Sektor).map((entry) => ({
    label: entry[1],
    value: entry[1],
  }));

  const selectedOption = ressursFilterValues.sektor
    ? options.find((option) => option.value === ressursFilterValues.sektor)
    : undefined;

  return (
    <SelectLayoutWrapper htmlFor="Sektor" label="Sektor">
      <BasicSelect
        css={css`
          min-width: 200px;
        `}
        inputId="Sektor"
        onChange={(option) =>
          navigate({
            search: { ...ressursFilterValues, sektor: option?.value },
          })
        }
        options={options}
        value={selectedOption ?? null}
      />
    </SelectLayoutWrapper>
  );
}

function FilterChips() {
  const navigate = useNavigate();
  const ressursFilterValues = useSearch({ from: ressurserAdminRoute.id });
  return (
    <Chips>
      {Object.entries(ressursFilterValues).map((entry) => {
        const [key, value] = entry;
        if (
          ["includeUnderliggendeOrgEnheter", "includeInactive"].includes(key) &&
          !value
        ) {
          return;
        }
        return (
          <Chips.Removable
            key={key}
            onClick={() =>
              navigate({ search: { ...ressursFilterValues, [key]: undefined } })
            }
          >{`${key}: ${value}`}</Chips.Removable>
        );
      })}
    </Chips>
  );
}

function FullRessursInfo({ ressurs }: { ressurs: RessursFields }) {
  return (
    <JsonView
      data={ressurs}
      shouldExpandNode={allExpanded}
      style={defaultStyles}
    />
  );
}

function applyFilter(
  ressurs: RessursFields,
  filter: RessursFilterValues,
  underLiggendeOrgenheter: string[],
) {
  return (
    orgEnhetMatches(ressurs, filter, underLiggendeOrgenheter) &&
    gyldighetsPeriodeMatches(ressurs, filter)
  );
}

function gyldighetsPeriodeMatches(
  ressurs: RessursFields,
  filter: RessursFilterValues,
) {
  return (
    filter.includeInactive ||
    isGyldighetsPeriodeActive(undefined, ressurs.sluttdato)
  );
}

function orgEnhetMatches(
  ressurs: RessursFields,
  filter: RessursFilterValues,
  underLiggendeOrgenheter: string[],
) {
  const orgEnhetMatch = filter.orgEnhet
    ? ressurs.orgTilknytning
        .filter((orgTilknytning) =>
          isGyldighetsPeriodeActive(
            orgTilknytning.gyldigFom,
            orgTilknytning.gyldigTom,
          ),
        )
        .some((orgTilknytning) => {
          const orgEnhetMatch = orgTilknytning.orgEnhet.id === filter.orgEnhet;
          const underLiggendeMatch = filter.includeUnderliggendeOrgEnheter
            ? underLiggendeOrgenheter.includes(orgTilknytning.orgEnhet.id)
            : false;
          return orgEnhetMatch || underLiggendeMatch;
        })
    : true;

  const underliggendeOrgEnhetMatch =
    Boolean(filter.includeUnderliggendeOrgEnheter) &&
    ressurs.orgTilknytning.some((orgTilknytning) =>
      underLiggendeOrgenheter.includes(orgTilknytning.orgEnhet.id),
    );

  return orgEnhetMatch || underliggendeOrgEnhetMatch;
}

export type RessursFilterValues = {
  orgEnhet?: string;
  sektor?: string;
  includeUnderliggendeOrgEnheter?: boolean;
  includeInactive?: boolean;
};
