import { css } from "@emotion/react";
import { TableIcon } from "@navikt/aksel-icons";
import {
  BodyShort,
  Button,
  Heading,
  HGrid,
  Link,
  ReadMore,
} from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import { Link as ReactRouterLink } from "@tanstack/react-router";
import partition from "lodash/partition";
import React from "react";

import {
  getRessurserForOrgenhet,
  isOrgEnhetArbeidsomraade,
} from "~/api/react-query-endpoints";
import { UserImage } from "~/components/UserImage";
import { ResourceLink } from "~/pages/OrganisasjonsHierarkiPage/components/CardViews";
import building from "~/resources/building.svg";
import type { OrgEnhet } from "~/types/api-models";
import { NomNivaa, OrgEnhetsType, Sektor } from "~/types/api-models";
import { formatDisplayDate, formatVisningsnavn } from "~/utils";

const isOrgEnhetArbeidsomraadeQuery = (id: string) =>
  useQuery({
    queryKey: isOrgEnhetArbeidsomraade.queryKey(id as string),
    queryFn: () => isOrgEnhetArbeidsomraade.queryFn(id as string),
    enabled: !!id,
  });

export const ValgtOrgEnhet = ({
  ressurser,
  orgEnhet,
}: {
  ressurser: string[];
  orgEnhet: OrgEnhet;
}) => (
  <div
    css={css`
      display: grid;
      border-radius: var(--a-spacing-2);
      background-color: var(--nom-secondary-hover);
      border: 1px solid var(--a-gray-400);
    `}
  >
    <TopBorder />
    <div
      css={css`
        display: flex;
        flex-direction: column;
        gap: var(--a-spacing-6);
        padding: var(--a-spacing-6);
      `}
    >
      <div
        css={css`
          display: flex;
          justify-content: space-between;
          align-items: flex-start;
        `}
      >
        <div>
          <Heading level="2" size="large" spacing>
            {orgEnhet.navn}
          </Heading>
          <HGrid
            columns={2}
            css={css`
              row-gap: var(--a-spacing-2);
            `}
            gap="4"
          >
            {orgEnhet.id && (
              <BodyShort size="small">
                ID: <b>{orgEnhet.id}</b>
              </BodyShort>
            )}
            {orgEnhet.remedyEnhetId && (
              <BodyShort size="small">
                Remedy-enhet: <b>{orgEnhet.remedyEnhetId}</b>
              </BodyShort>
            )}
            {orgEnhet.gyldigFom && (
              <BodyShort size="small">
                Gyldig f.o.m.: <b>{formatDisplayDate(orgEnhet.gyldigFom)}</b>
              </BodyShort>
            )}
            {orgEnhet.gyldigTom && (
              <BodyShort size="small">
                Gyldig t.o.m.: <b>{formatDisplayDate(orgEnhet.gyldigTom)}</b>
              </BodyShort>
            )}
            {orgEnhet.orgEnhetsType && (
              <BodyShort size="small">
                Enhetstype:{" "}
                <b>{OrgEnhetsTypeNameMapping[orgEnhet.orgEnhetsType]}</b>
              </BodyShort>
            )}
            {orgEnhet.nomNivaa && (
              <BodyShort size="small">
                NOM-nivå: <b>{NomNivaaNameMapping[orgEnhet.nomNivaa]}</b>
              </BodyShort>
            )}
            {orgEnhet.agressoId && (
              <BodyShort size="small">
                Agresso-ID: <b>{orgEnhet.agressoId}</b>
              </BodyShort>
            )}
          </HGrid>
        </div>

        <div
          css={css`
            display: flex;
            flex-direction: column;
            gap: var(--a-spacing-4);
            align-items: center;
          `}
        >
          <UserImage
            navident={orgEnhet.ledere[0]?.navident}
            navn={formatVisningsnavn(orgEnhet.ledere[0]?.visningsnavn)}
            size="90px"
          />
          <OrgenhetLederLink orgEnhet={orgEnhet} />
        </div>
      </div>
      {isOrgEnhetArbeidsomraadeQuery(orgEnhet.id).data?.valueOf() && (
        <div
          css={css`
            display: flex;
            justify-content: end;
            align-items: end;
            flex-wrap: wrap;
            gap: 1rem;
          `}
        >
          <Link href={`/org/table/${orgEnhet.id}`}>
            <Button icon={<TableIcon />} size="medium" variant="primary">
              Tabellvisning
            </Button>
          </Link>
        </div>
      )}
      <MedarbeidereList navidenter={ressurser} orgEnhet={orgEnhet} />
    </div>
  </div>
);

function MedarbeidereList({
  orgEnhet,
  navidenter,
}: {
  orgEnhet: OrgEnhet;
  navidenter: string[];
}) {
  const hentedeRessurser =
    useQuery({
      queryKey: getRessurserForOrgenhet.queryKey(orgEnhet.id),
      queryFn: () => getRessurserForOrgenhet.queryFn(navidenter),
    }).data ?? [];

  if (hentedeRessurser.length === 0) {
    return <></>;
  }

  const [eksterneRessurser, interneRessurser] = partition(
    hentedeRessurser,
    (ressurs) => ressurs.sektor.includes(Sektor.EKSTERN),
  );

  return (
    <div>
      {interneRessurser.length > 0 && (
        <div>
          <Heading level="3" size="small" spacing>
            Medarbeidere i {orgEnhet.navn}
          </Heading>
          <div
            css={css`
              display: grid;
              grid-template-columns: repeat(2, 1fr);
              gap: var(--a-spacing-4);
            `}
          >
            {interneRessurser.map((ressurs) => {
              return (
                <ResourceLink
                  key={ressurs.navident}
                  navident={ressurs.navident}
                />
              );
            })}
          </div>
        </div>
      )}

      {eksterneRessurser.length > 0 && (
        <ReadMore
          css={css`
            margin-top: var(--a-spacing-4);

            .navds-read-more__button {
              color: var(--a-text-default);
              font-size: var(--a-font-size-heading-small);
              font-weight: var(--a-font-weight-bold);
            }

            .navds-read-more__content {
              margin-left: 0;
              padding-left: 0;
              border-left: none;
            }
          `}
          defaultOpen
          header={"Eksterne medarbeidere i " + orgEnhet.navn}
        >
          <div
            css={css`
              margin-top: var(--a-spacing-4);
              display: grid;
              grid-template-columns: repeat(2, 1fr);
              gap: var(--a-spacing-4);
            `}
          >
            {eksterneRessurser.map((ressurs) => {
              return (
                <ResourceLink
                  key={ressurs.navident}
                  navident={ressurs.navident}
                />
              );
            })}
          </div>
        </ReadMore>
      )}
    </div>
  );
}

function TopBorder() {
  return (
    <div
      css={css`
        display: flex;
        gap: var(--a-spacing-3);
        outline: 2px solid var(--a-lightblue-800);
        outline-offset: -1px;
        background-color: var(--a-lightblue-800);
        border-radius: 4px 4px 0 0;
        color: white;
        padding: var(--a-spacing-1) var(--a-spacing-3);
      `}
    >
      <img alt="" src={building} />
      Organisasjonsenhet
    </div>
  );
}

function OrgenhetLederLink({ orgEnhet }: { orgEnhet: OrgEnhet }) {
  const firstLeder = orgEnhet.ledere[0];
  const { navident, visningsnavn } = firstLeder ?? {};

  if (!navident) {
    return <span>Ingen leder</span>;
  }

  return (
    <span>
      Leder:{" "}
      <ReactRouterLink params={{ id: navident }} search={{}} to="/ressurs/$id">
        <Link as="span">{formatVisningsnavn(visningsnavn)}</Link>
      </ReactRouterLink>
    </span>
  );
}

export const OrgEnhetsTypeNameMapping = {
  [OrgEnhetsType.ARBEIDSLIVSSENTER]: "Arbeidslivssenter",
  [OrgEnhetsType.NAV_ARBEID_OG_YTELSER]: "Arbeid og ytelser",
  [OrgEnhetsType.ARBEIDSRAADGIVNING]: "Arbeidsrådgivning",
  [OrgEnhetsType.DIREKTORAT]: "Direktorat",
  [OrgEnhetsType.DIR]: "DIR",
  [OrgEnhetsType.FYLKE]: "Fylke",
  [OrgEnhetsType.NAV_FAMILIE_OG_PENSJONSYTELSER]: "Familie- og pensjonsytelser",
  [OrgEnhetsType.HJELPEMIDLER_OG_TILRETTELEGGING]:
    "Hjelpemidler og tilrettelegging",
  [OrgEnhetsType.KLAGEINSTANS]: "Klageinstans",
  [OrgEnhetsType.NAV_KONTAKTSENTER]: "Kontaktsenter",
  [OrgEnhetsType.KONTROLL_KONTROLLENHET]: "Kontroll",
  [OrgEnhetsType.NAV_KONTOR]: "NAV-kontor",
  [OrgEnhetsType.TILTAK]: "Tiltak",
  [OrgEnhetsType.NAV_OKONOMITJENESTE]: "Økonomi",
};

export const NomNivaaNameMapping = {
  [NomNivaa.LINJEENHET]: "Linjeenhet",
  [NomNivaa.DRIFTSENHET]: "Driftsenhet",
  [NomNivaa.ARBEIDSOMRAADE]: "Arbeidsområde",
};
