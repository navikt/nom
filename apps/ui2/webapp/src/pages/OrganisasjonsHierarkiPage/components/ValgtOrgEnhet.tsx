import { css } from "@emotion/react";
import { MenuElipsisHorizontalCircleIcon } from "@navikt/aksel-icons";
import {
  BodyShort,
  Button,
  Dropdown,
  Heading,
  HGrid,
  Link,
  ReadMore,
  VStack,
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
import type { OrgEnhet, Ressurs } from "~/types/api-models";
import { NomNivaa, OrgEnhetsType, Sektor } from "~/types/api-models";
import { formatDisplayDate, formatVisningsnavn } from "~/utils";

export const ValgtOrgEnhet = ({
  ressurser,
  orgEnhet,
}: {
  ressurser: string[];
  orgEnhet: OrgEnhet;
}) => {
  const isOrgEnhetArbeidsomraadeQuery = (id: string) =>
    useQuery({
      queryKey: isOrgEnhetArbeidsomraade.queryKey(id as string),
      queryFn: () => isOrgEnhetArbeidsomraade.queryFn(id as string),
      enabled: !!id,
    });

  const hentedeRessurser =
    useQuery({
      queryKey: getRessurserForOrgenhet.queryKey(orgEnhet.id),
      queryFn: () => getRessurserForOrgenhet.queryFn(ressurser),
    }).data ?? [];

  const visTabellKnapp = isOrgEnhetArbeidsomraadeQuery(orgEnhet.id).data;

  return (
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
              {orgEnhet.gyldigFom && orgEnhet.navn !== "NAV" && (
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

        <MedarbeidereList
          orgEnhet={orgEnhet}
          ressurser={hentedeRessurser}
          visTabellKnapp={visTabellKnapp ?? false}
        />
      </div>
    </div>
  );
};

function MedarbeidereList({
  orgEnhet,
  ressurser,
  visTabellKnapp,
}: {
  orgEnhet: OrgEnhet;
  ressurser: Ressurs[];
  visTabellKnapp: boolean;
}) {
  if (ressurser.length === 0) {
    return visTabellKnapp ? (
      <div
        css={css`
          display: flex;
          justify-content: end;
          align-items: center;
          margin-top: var(--a-spacing-6);
        `}
      >
        <DropdownMeny orgEnhet={orgEnhet} />
      </div>
    ) : (
      <></>
    );
  }

  const [eksterneRessurser, interneRessurser] = partition(
    ressurser,
    (ressurs) => ressurs.sektor.includes(Sektor.EKSTERN),
  );

  interneRessurser.sort((a, b) =>
    a.visningsnavn.fornavn.localeCompare(b.visningsnavn.fornavn),
  );
  eksterneRessurser.sort((a, b) =>
    a.visningsnavn.fornavn.localeCompare(b.visningsnavn.fornavn),
  );

  return (
    <>
      <div
        css={css`
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-top: var(--a-spacing-6);
        `}
      >
        <Heading level="3" size="medium">
          Medarbeidere i {orgEnhet.navn}
        </Heading>

        <DropdownMeny orgEnhet={orgEnhet} />
      </div>
      <VStack gap="4">
        {interneRessurser.length > 0 && (
          <div>
            <div
              css={css`
                display: flex;
                margin-bottom: var(--a-spacing-5);
                align-items: center;
              `}
            >
              <Heading level="4" size="small">
                Interne medarbeidere
              </Heading>
            </div>
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
          <div>
            <ReadMore
              css={css`
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
              header={"Eksterne medarbeidere"}
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
          </div>
        )}
      </VStack>
    </>
  );
}

function DropdownMeny({ orgEnhet }: { orgEnhet: OrgEnhet }) {
  return (
    <Dropdown>
      <Button
        as={Dropdown.Toggle}
        css={css`
          color: black;
        `}
        icon={<MenuElipsisHorizontalCircleIcon title="Rediger" />}
        variant="tertiary"
      />
      <Dropdown.Menu
        css={css`
          width: 460px;
        `}
      >
        <Dropdown.Menu.GroupedList>
          <Dropdown.Menu.GroupedList.Heading>
            Administrering av medarbeidere
          </Dropdown.Menu.GroupedList.Heading>
          <Dropdown.Menu.GroupedList.Item
            as={Link}
            href={`/org/table/${orgEnhet.id}`}
          >
            Tabellvisning av ansatte i denne enheten og underliggende enheter
          </Dropdown.Menu.GroupedList.Item>
          <Dropdown.Menu.Divider />
          <Dropdown.Menu.GroupedList.Item as={Link} href="/ressurs/create/1">
            Opprett medarbeider
          </Dropdown.Menu.GroupedList.Item>
        </Dropdown.Menu.GroupedList>
      </Dropdown.Menu>
    </Dropdown>
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
