import { css } from "@emotion/react";
import { ChevronDownDoubleIcon } from "@navikt/aksel-icons";
import {
  BodyShort,
  Button,
  Dropdown,
  Heading,
  HGrid,
  Link,
  Loader,
  ReadMore,
  VStack,
} from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import { Link as ReactRouterLink } from "@tanstack/react-router";
import partition from "lodash/partition";
import React, { useState } from "react";

import {
  downloadExcelKontaktPersonserForOrgenhetByOrgenhetId,
  getOrgEnheterSomKanHaRessurser,
  getRessurserByListNavident,
  getTeamcatSection,
  harLederTilgangForOrgenhetOptions,
  isOrgEnhetArbeidsomraade,
  isOrgEnhetDriftsenhet,
} from "~/api/react-query-endpoints";
import { useHasAdminAccess, useHasWriteAccess } from "~/api/useGetSession";
import { LabelWithValue } from "~/components/LabelWithValue";
import { SmartLink } from "~/components/navigation/SmartLink";
import { OrgEnhetStatistikk } from "~/components/statistikk/OrgEnhetStatistikk";
import { UserImage } from "~/components/UserImage";
import { ResourceLink } from "~/pages/OrganisasjonsHierarkiPage/components/CardViews";
import {
  isPeriodActive,
  nomDateIsTodayOrAfter,
} from "~/pages/ResourcePage/RessursComponents/dolUtils";
import building from "~/resources/building.svg";
import type { OrgEnhet, Ressurs } from "~/types/api-models";
import { IdentType, NomNivaa, OrgEnhetsType, Sektor } from "~/types/api-models";
import {
  formatDisplayDate,
  isEksternSamarbeidspartnerOrgEnhet,
  isInDevelopment,
} from "~/utils";

export const ValgtOrgEnhet = ({
  orgEnhet,
  hideRessurser,
}: {
  orgEnhet: OrgEnhet;
  hideRessurser?: boolean;
}) => {
  const inDevelopment = isInDevelopment();

  const ressurserNavidents = orgEnhet.ressursKoblinger
    .filter((orgtilknytning) =>
      isPeriodActive(orgtilknytning.gyldigFom, orgtilknytning.gyldigTom),
    )
    .map((orgtilknytning) => orgtilknytning.navident);

  const hentedeRessurserQuery = useQuery({
    queryKey: getRessurserByListNavident.queryKey(ressurserNavidents),
    queryFn: () => getRessurserByListNavident.queryFn(ressurserNavidents),
    enabled: ressurserNavidents.length > 0,
  });

  const hentedeRessurser =
    hentedeRessurserQuery.data?.filter((ressurs) =>
      nomDateIsTodayOrAfter(ressurs.sluttdato),
    ) ?? [];

  const isOrgEnhetArbeidsomraadeQuery = useQuery({
    queryKey: isOrgEnhetArbeidsomraade.queryKey(orgEnhet.id),
    queryFn: () => isOrgEnhetArbeidsomraade.queryFn(orgEnhet.id),
    enabled: !!orgEnhet.id,
  });

  const isOrgEnhetDriftsenhetQuery = useQuery({
    queryKey: isOrgEnhetDriftsenhet.queryKey(orgEnhet.id),
    queryFn: () => isOrgEnhetDriftsenhet.queryFn(orgEnhet.id),
    enabled: !!orgEnhet.id,
  });

  const enheterSomKanHaRessurser = useQuery(getOrgEnheterSomKanHaRessurser);

  const valgtOrgenhetKanHaRessurser = (
    enheterSomKanHaRessurser.data || []
  ).some((enhet) => enhet.id === orgEnhet.id);

  const eksternSamarbeidspartner = isEksternSamarbeidspartnerOrgEnhet(
    orgEnhet.id,
  );

  const visTabellKnapp =
    (isOrgEnhetDriftsenhetQuery.data ?? false) || eksternSamarbeidspartner;

  const visRessursAdministeringKnapper =
    isOrgEnhetArbeidsomraadeQuery.data ?? false;

  const loading =
    hentedeRessurserQuery.isLoading ||
    isOrgEnhetDriftsenhetQuery.isLoading ||
    enheterSomKanHaRessurser.isLoading;

  const teamkatalogSeksjon = useQuery({
    queryKey: getTeamcatSection.queryKey(orgEnhet.id),
    queryFn: () => getTeamcatSection.queryFn(orgEnhet.id),
    enabled:
      orgEnhet.orgEnhetsType === OrgEnhetsType.DIR ||
      orgEnhet.orgEnhetsType === OrgEnhetsType.DIREKTORAT ||
      orgEnhet.orgEnhetsType === OrgEnhetsType.KLAGEINSTANS,
  }).data;

  const tkatLenke = inDevelopment
    ? `https://teamkatalog.ekstern.dev.nav.no/area/${teamkatalogSeksjon?.id}`
    : `https://teamkatalog.nav.no/area/${teamkatalogSeksjon?.id}`;

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
              columns="max-content max-content"
              css={css`
                row-gap: var(--a-spacing-2);
              `}
              gap="4"
            >
              <LabelWithValue copyButton label="ID" value={orgEnhet.id} />
              <LabelWithValue
                label="Remedy-enhet"
                value={orgEnhet.remedyEnhetId}
              />
              {orgEnhet.navn.toLowerCase() !== "nav" && (
                <LabelWithValue
                  label="Gyldig f.o.m."
                  value={formatDisplayDate(orgEnhet.gyldigFom)}
                />
              )}
              <LabelWithValue
                label="Gyldig t.o.m."
                value={formatDisplayDate(orgEnhet.gyldigTom)}
              />
              <LabelWithValue
                label="Enhetstype"
                value={
                  orgEnhet.orgEnhetsType
                    ? OrgEnhetsTypeNameMapping[orgEnhet.orgEnhetsType]
                    : undefined
                }
              />
              <LabelWithValue
                label="NOM-nivå"
                value={
                  orgEnhet.nomNivaa
                    ? NomNivaaNameMapping[orgEnhet.nomNivaa]
                    : undefined
                }
              />
              <LabelWithValue
                copyButton
                label="Unit4-ID"
                value={orgEnhet.agressoId}
              />
              {orgEnhet.orgNiv !== "ORGNIV0" &&
                orgEnhet.orgNiv !== "ORGENHET" && (
                  <LabelWithValue label="Unit4-nivå" value={orgEnhet.orgNiv} />
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
              navident={orgEnhet.leder?.navident}
              navn={orgEnhet.leder?.visningsnavn}
              size="90px"
            />
            <OrgenhetLederLink orgEnhet={orgEnhet} />
          </div>
        </div>

        {!!teamkatalogSeksjon && (
          <SmartLink
            css={css`
              margin-top: var(--a-spacing-4);
            `}
            externalIcon
            href={tkatLenke}
          >
            Se seksjon i Teamkatalogen
          </SmartLink>
        )}

        {loading ? (
          <Loader
            css={css`
              margin: var(--a-spacing-8) auto auto;
            `}
            size="3xlarge"
          />
        ) : (
          <MedarbeidereList
            orgEnhet={orgEnhet}
            ressurser={hideRessurser ? [] : hentedeRessurser}
            valgtOrgenhetKanHaRessurser={valgtOrgenhetKanHaRessurser}
            visRessursAdministeringKnapper={visRessursAdministeringKnapper}
            visTabellKnapp={visTabellKnapp ?? false}
          />
        )}

        <OrgEnhetStatistikk
          css={css`
            margin-top: var(--a-spacing-6);
          `}
          expandable
          header="Statistikk"
          orgEnhetId={orgEnhet.id}
        />
      </div>
    </div>
  );
};

function MedarbeidereList({
  orgEnhet,
  ressurser,
  visRessursAdministeringKnapper,
  visTabellKnapp,
  valgtOrgenhetKanHaRessurser,
}: {
  orgEnhet: OrgEnhet;
  ressurser: Ressurs[];
  visRessursAdministeringKnapper: boolean;
  visTabellKnapp: boolean;
  valgtOrgenhetKanHaRessurser: boolean;
}) {
  const [isDownloading, setIsDownloading] = useState(false);

  if (ressurser.length === 0) {
    return visTabellKnapp ? (
      <>
        <div
          css={css`
            display: flex;
            justify-content: end;
            align-items: center;
            margin-top: var(--a-spacing-6);
          `}
        >
          <DropdownMeny
            orgEnhet={orgEnhet}
            setIsDownloading={setIsDownloading}
            valgtEnhetKanHaRessurser={valgtOrgenhetKanHaRessurser}
            visRessursAdministeringKnapper={visRessursAdministeringKnapper}
          />
        </div>

        {isDownloading && (
          <div
            css={css`
              display: flex;
              flex-direction: column;
              align-items: center;
              margin: auto;
              gap: var(--a-spacing-2);
            `}
          >
            <Loader size="3xlarge" />
            <BodyShort size="large">Eksporterer kontaktpersoner...</BodyShort>
          </div>
        )}
      </>
    ) : (
      <></>
    );
  }

  ressurser.sort((a, b) =>
    a.visningsnavn.fornavn.localeCompare(b.visningsnavn.fornavn),
  );

  const [roboter, mennesker] = partition(
    ressurser,
    (ressurs) => ressurs.identType === IdentType.ROBOT,
  );

  const [eksterneRessurser, interneRessurser] = partition(
    mennesker,
    (ressurs) => ressurs.sektor === Sektor.EKSTERN,
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
        <Heading
          css={css`
            align-self: start;
          `}
          level="3"
          size="medium"
        >
          Medarbeidere i {orgEnhet.navn}
        </Heading>

        {visTabellKnapp && (
          <DropdownMeny
            orgEnhet={orgEnhet}
            setIsDownloading={setIsDownloading}
            valgtEnhetKanHaRessurser={valgtOrgenhetKanHaRessurser}
            visRessursAdministeringKnapper={visRessursAdministeringKnapper}
          />
        )}
      </div>

      {isDownloading && (
        <div
          css={css`
            display: flex;
            flex-direction: column;
            align-items: center;
            margin: auto;
            gap: var(--a-spacing-2);
          `}
        >
          <Loader size="3xlarge" />
          <BodyShort size="large">Eksporterer kontaktpersoner...</BodyShort>
        </div>
      )}

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
                  <ResourceLink key={ressurs.navident} ressurs={ressurs} />
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
                    <ResourceLink key={ressurs.navident} ressurs={ressurs} />
                  );
                })}
              </div>
            </ReadMore>
          </div>
        )}

        {roboter.length > 0 && (
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
              header={"Roboter"}
            >
              <div
                css={css`
                  margin-top: var(--a-spacing-4);
                  display: grid;
                  grid-template-columns: repeat(2, 1fr);
                  gap: var(--a-spacing-4);
                `}
              >
                {roboter.map((ressurs) => {
                  return (
                    <ResourceLink key={ressurs.navident} ressurs={ressurs} />
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

function DropdownMeny({
  orgEnhet,
  visRessursAdministeringKnapper,
  setIsDownloading,
  valgtEnhetKanHaRessurser,
}: {
  orgEnhet: OrgEnhet;
  visRessursAdministeringKnapper: boolean;
  setIsDownloading: (value: boolean) => void;
  valgtEnhetKanHaRessurser: boolean;
}) {
  const hasAdminAccess = useHasAdminAccess();
  const hasWriteAccess = useHasWriteAccess();

  const hasWriteAccessForOrgenhet =
    useQuery(harLederTilgangForOrgenhetOptions(orgEnhet.id)).data ?? false;

  const excelFilNavn = () => {
    return "kontaktpersoner for orgenhet - " + orgEnhet?.navn + ".xlsx";
  };

  const handleDownloadKontaktpersonerExcel = async () => {
    try {
      setIsDownloading(true);
      await downloadExcelKontaktPersonserForOrgenhetByOrgenhetId(
        orgEnhet.id,
        excelFilNavn(),
      );
    } finally {
      setIsDownloading(false);
    }
  };

  const adminOrWriteForOrgenhet =
    (hasWriteAccess && hasWriteAccessForOrgenhet) || hasAdminAccess;

  const eksternSamarbeidspartner = isEksternSamarbeidspartnerOrgEnhet(
    orgEnhet.id,
  );

  const visOpprettMedarbeidere =
    visRessursAdministeringKnapper && adminOrWriteForOrgenhet;

  const visMasseflytting =
    visRessursAdministeringKnapper &&
    valgtEnhetKanHaRessurser &&
    hasAdminAccess;

  const visKontaktpersonEksport =
    adminOrWriteForOrgenhet && !eksternSamarbeidspartner;

  return (
    <div
      css={css`
        display: grid;
      `}
    >
      <Dropdown>
        <Button
          as={Dropdown.Toggle}
          icon={<ChevronDownDoubleIcon title="Alternativer" />}
          size="small"
          variant="secondary"
        >
          Alternativer
        </Button>

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

            {visOpprettMedarbeidere && (
              <>
                <Dropdown.Menu.Divider />
                <Dropdown.Menu.GroupedList.Item
                  as={Link}
                  href="/ressurs/create/1"
                >
                  Opprett medarbeider
                </Dropdown.Menu.GroupedList.Item>
              </>
            )}

            {visMasseflytting && (
              <>
                <Dropdown.Menu.Divider />
                <Dropdown.Menu.GroupedList.Item
                  as={Link}
                  href={`/masseflytting/${orgEnhet.id}`}
                >
                  Flytt én eller flere medarbeidere
                </Dropdown.Menu.GroupedList.Item>
              </>
            )}

            {visKontaktpersonEksport && (
              <>
                <Dropdown.Menu.Divider />
                <Dropdown.Menu.GroupedList.Item
                  as={Link}
                  onClick={handleDownloadKontaktpersonerExcel}
                >
                  Eksportér kontaktpersoner til Excel
                </Dropdown.Menu.GroupedList.Item>
              </>
            )}
          </Dropdown.Menu.GroupedList>
        </Dropdown.Menu>
      </Dropdown>
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
  const firstLeder = orgEnhet.leder;
  const { navident, visningsnavn } = firstLeder ?? {};

  if (!navident) {
    return <span>Ingen leder</span>;
  }

  return (
    <span>
      Leder:{" "}
      <ReactRouterLink params={{ id: navident }} search={{}} to="/ressurs/$id">
        <Link as="span">{visningsnavn}</Link>
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
  [OrgEnhetsType.NAV_KONTOR]: "Nav-kontor",
  [OrgEnhetsType.TILTAK]: "Tiltak",
  [OrgEnhetsType.NAV_OKONOMITJENESTE]: "Økonomi",
};

export const NomNivaaNameMapping = {
  [NomNivaa.LINJEENHET]: "Linjeenhet",
  [NomNivaa.DRIFTSENHET]: "Driftsenhet",
  [NomNivaa.ARBEIDSOMRAADE]: "Arbeidsområde",
};
