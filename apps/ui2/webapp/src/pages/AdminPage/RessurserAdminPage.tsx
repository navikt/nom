import "react-json-view-lite/dist/index.css";

import { css } from "@emotion/react";
import type { SortState } from "@navikt/ds-react";
import {
  Alert,
  BodyShort,
  Button,
  Checkbox,
  Chips,
  DatePicker,
  Loader,
  Pagination,
  Popover,
  Table,
  useDatepicker,
} from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import {
  Link as ReactRouterLink,
  useNavigate,
  useSearch,
} from "@tanstack/react-router";
import { isAfter, isBefore, maxTime } from "date-fns";
import sortBy from "lodash/sortBy";
import type { ReactNode } from "react";
import { useEffect, useRef, useState } from "react";
import { JsonView } from "react-json-view-lite";

import type { RessursFields } from "~/api/react-query-endpoints";
import {
  getAlleRessurserGraphql,
  getUnderliggendeOrgenheter,
} from "~/api/react-query-endpoints";
import {
  BasicSelect,
  SelectLayoutWrapper,
} from "~/components/select/CustomSelectComponents";
import { OrgEnhetSearch } from "~/pages/AdminPage/components/OrgEnhetSearch";
import {
  orgRoute,
  ressurserAdminRoute,
  ressursRoute,
} from "~/reactRouterRoutes";
import { Sektor } from "~/types/api-models";
import { formatDateWithoutTimezone, isGyldighetsPeriodeActive } from "~/utils";

export function RessurserAdminPage() {
  const [sort, setSort] = useState<SortState | undefined>({
    orderBy: "navident",
    direction: "ascending",
  });

  const ressursFilterValues = useSearch({ from: ressurserAdminRoute.id });
  // Using react query instead of apollo because it runs  more smoothly
  const alleRessurser = useQuery<RessursFields[], unknown>({
    ...getAlleRessurserGraphql,
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

  useEffect(() => {
    setPage(1);
  }, [ressursFilterValues]);

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

  const sortedRessurser = sort
    ? sortRessurser({ ressurser: filteredRessurser, sort })
    : filteredRessurser;

  const ressurserToDisplay = sortedRessurser.slice(
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
      <Table onSortChange={(sortKey) => handleSort(sortKey)} sort={sort}>
        <Table.Header>
          <Table.Row>
            <Table.DataCell />
            <Table.ColumnHeader scope="col" sortKey="navident" sortable>
              Navident
            </Table.ColumnHeader>
            <Table.ColumnHeader scope="col" sortKey="visningsnavn" sortable>
              Visningsnavn
            </Table.ColumnHeader>
            <Table.ColumnHeader scope="col" sortKey="sektor" sortable>
              Sektor
            </Table.ColumnHeader>
            <Table.ColumnHeader scope="col" sortKey="orgTilknytning" sortable>
              Org Tilknytning
            </Table.ColumnHeader>
            <Table.ColumnHeader scope="col" sortKey="ledere" sortable>
              Ledere
            </Table.ColumnHeader>
            <Table.ColumnHeader scope="col" sortKey="startdato" sortable>
              Startdato
            </Table.ColumnHeader>
            <Table.ColumnHeader scope="col" sortKey="sluttdato" sortable>
              Sluttdato
            </Table.ColumnHeader>
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
              <Table.DataCell>{ressurs.startdato}</Table.DataCell>
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

  const pluralDisplay = (
    <div
      css={css`
        display: flex;
        flex-direction: row;
        gap: var(--a-spacing-2);
      `}
    >
      <OrgEnhetLink orgenhet={activeOrgtilknytninger[0]?.orgEnhet} />
      <ExtraRowContent
        content={activeOrgtilknytninger.slice(1).map((orgTilknytning) => (
          <OrgEnhetLink
            key={orgTilknytning.orgEnhet.id}
            orgenhet={orgTilknytning.orgEnhet}
          />
        ))}
      />
    </div>
  );

  return (
    <Table.DataCell>
      <Pluralize
        numberOfElements={activeOrgtilknytninger.length}
        pluralForm={pluralDisplay}
        singleForm={
          <OrgEnhetLink orgenhet={activeOrgtilknytninger[0]?.orgEnhet} />
        }
      />
    </Table.DataCell>
  );
}

function OrgEnhetLink({
  orgenhet,
}: {
  orgenhet?: { id: string; navn: string };
}) {
  return orgenhet ? (
    <ReactRouterLink params={{ id: orgenhet.id }} to={orgRoute.to}>
      {orgenhet.navn}
    </ReactRouterLink>
  ) : undefined;
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

  const pluralDisplay = (
    <div
      css={css`
        display: flex;
        flex-direction: row;
        gap: var(--a-spacing-2);
      `}
    >
      <RessursLink ressurs={activeLedere[0]?.ressurs} />
      <ExtraRowContent
        content={activeLedere.slice(1).map((leder) => (
          <RessursLink key={leder.ressurs.navident} ressurs={leder.ressurs} />
        ))}
      />
    </div>
  );

  return (
    <Table.DataCell>
      <Pluralize
        numberOfElements={activeLedere.length}
        pluralForm={pluralDisplay}
        singleForm={<RessursLink ressurs={activeLedere[0]?.ressurs} />}
      />
    </Table.DataCell>
  );
}

function RessursLink({
  ressurs,
}: {
  ressurs?: { navident: string; visningsnavn: string };
}) {
  return ressurs ? (
    <ReactRouterLink params={{ id: ressurs.navident }} to={ressursRoute.to}>
      {`${ressurs.visningsnavn} (${ressurs.navident})`}
    </ReactRouterLink>
  ) : undefined;
}

function ExtraRowContent({ content }: { content: ReactNode[] }) {
  const buttonReference = useRef<HTMLButtonElement>(null);
  const [showExtra, setShowExtra] = useState(false);

  return (
    <>
      <Button
        onClick={() => setShowExtra(!showExtra)}
        ref={buttonReference}
        size="xsmall"
      >
        +1
      </Button>
      <Popover
        anchorEl={buttonReference.current}
        onClose={() => setShowExtra(false)}
        open={showExtra}
      >
        <Popover.Content>{content}</Popover.Content>
      </Popover>
    </>
  );
}

function Pluralize({
  numberOfElements,
  singleForm,
  pluralForm,
}: {
  numberOfElements: number;
  singleForm?: ReactNode;
  pluralForm?: ReactNode;
}) {
  if (numberOfElements === 1) {
    return singleForm;
  }
  if (numberOfElements > 1) {
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
      <DateSelect filterKey="startDato" label="Start dato" />
      <DateSelect filterKey="sluttDato" label="Slutt dato" />
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
        <BodyShort size="small">Inkluder inaktive ressurser</BodyShort>
      </Checkbox>
    </div>
  );
}

function DateSelect({
  label,
  filterKey,
}: {
  label: string;
  filterKey: "startDato" | "sluttDato";
}) {
  const ressursFilterValues = useSearch({ from: ressurserAdminRoute.id });
  const navigate = useNavigate();

  const defaultValue = ressursFilterValues[filterKey] as string;

  const { datepickerProps, inputProps, setSelected } = useDatepicker({
    defaultSelected: defaultValue ? new Date(defaultValue) : undefined,
    onDateChange: (date) =>
      navigate({
        search: {
          ...ressursFilterValues,
          [filterKey]: date ? formatDateWithoutTimezone(date) : undefined,
        },
      }),
  });

  useEffect(() => {
    const selectedValue = ressursFilterValues[filterKey];
    setSelected(selectedValue ? new Date(selectedValue) : undefined);
  }, [ressursFilterValues[filterKey]]);

  return (
    <DatePicker {...datepickerProps}>
      <DatePicker.Input {...inputProps} label={label} />
    </DatePicker>
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
  return <JsonView data={ressurs} />;
}

function applyFilter(
  ressurs: RessursFields,
  filter: RessursFilterValues,
  underLiggendeOrgenheter: string[],
) {
  return (
    orgEnhetMatches(ressurs, filter, underLiggendeOrgenheter) &&
    sektorMatches(ressurs, filter) &&
    includeInactiveMatches(ressurs, filter) &&
    startDatoMatches(ressurs, filter) &&
    sluttDatoMatches(ressurs, filter)
  );
}

function includeInactiveMatches(
  ressurs: RessursFields,
  filter: RessursFilterValues,
) {
  return (
    filter.includeInactive ||
    isGyldighetsPeriodeActive(undefined, ressurs.sluttdato)
  );
}

function startDatoMatches(ressurs: RessursFields, filter: RessursFilterValues) {
  return filter.startDato
    ? isAfter(new Date(ressurs.startdato), new Date(filter.startDato))
    : true;
}

function sluttDatoMatches(ressurs: RessursFields, filter: RessursFilterValues) {
  return filter.sluttDato
    ? isBefore(
        new Date(ressurs.sluttdato ?? maxTime),
        new Date(filter.sluttDato),
      )
    : true;
}

function sektorMatches(ressurs: RessursFields, filter: RessursFilterValues) {
  return filter.sektor ? ressurs.sektor.includes(filter.sektor) : true;
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

function sortRessurser({
  ressurser,
  sort,
}: {
  ressurser: RessursFields[];
  sort: SortState;
}) {
  const { orderBy, direction } = sort;

  const sortedMembersAscending = sortRessurserAscending(ressurser, orderBy);

  const reversed = direction === "descending";

  return reversed ? sortedMembersAscending.reverse() : sortedMembersAscending;
}

function sortRessurserAscending(ressurser: RessursFields[], orderBy: string) {
  if (
    ["navident", "visningsnavn", "startdato", "sluttdato"].includes(orderBy)
  ) {
    return sortBy(ressurser, orderBy);
  }

  if (orderBy === "sektor") {
    return sortBy(ressurser, (ressurs) => ressurs.sektor[0]);
  }

  if (orderBy === "orgTilknytning") {
    return sortBy(
      ressurser,
      (ressurs) => ressurs.orgTilknytning[0]?.orgEnhet.navn,
    );
  }

  if (orderBy === "ledere") {
    return sortBy(
      ressurser,
      (ressurs) => ressurs.ledere[0]?.ressurs.visningsnavn,
    );
  }
  return ressurser;
}

export type RessursFilterValues = {
  orgEnhet?: string;
  sektor?: string;
  includeUnderliggendeOrgEnheter?: boolean;
  includeInactive?: boolean;
  startDato?: string;
  sluttDato?: string;
};
