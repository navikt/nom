import { css } from "@emotion/react";
import {
  ArrowRightLeftIcon,
  Buildings2FillIcon,
  PlusCircleIcon,
  TrashIcon,
} from "@navikt/aksel-icons";
import {
  BodyShort,
  Button,
  ExpansionCard,
  Heading,
  HGrid,
  HStack,
  LinkCard,
  Search,
  VStack,
} from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import React, { useState } from "react";

import {
  getLedereForOrgEnhet,
  getOrgEnheterNavidentKanRedigere,
} from "~/api/react-query-endpoints";
import { useHasAdminAccess } from "~/api/useGetSession";
import ContentWrapper from "~/components/ContentWrapper";
import { Divider } from "~/components/Divider";
import { LabelWithValue } from "~/components/LabelWithValue";
import { LoaderWithText } from "~/components/LoaderWithText";
import { SmartLink } from "~/components/navigation/SmartLink";
import { PageBanner } from "~/components/PageBanner";
import { CompactOrganisasjonHierarki } from "~/pages/ResourcePage/CompactOrganisasjonHierarki";
import { EnhetKontekstInfo } from "~/pages/ResourcePage/Masseflytting/Masseflytting";
import type { MinimalRessurs, OrgEnhet } from "~/types/api-models";
import { formatDisplayDate, formatVisningsnavn } from "~/utils";

export const OrganisasjonMastringForside = () => {
  const hasAdminAccess = useHasAdminAccess();

  if (!hasAdminAccess) {
    return;
  }

  return (
    <>
      <PageBanner text="Organisasjonsadministrasjon dashboard" />

      <ContentWrapper>
        <VStack gap="12">
          <AdministreringAvOrgEnheterSeksjon />

          <DineOrgEnheterSeksjon />
        </VStack>
      </ContentWrapper>
    </>
  );
};

function AdministreringAvOrgEnheterSeksjon() {
  const hasAdminAccess = useHasAdminAccess();

  return (
    <VStack gap="6">
      <Heading level="2" size="large">
        Administrering av organisasjonsenheter
      </Heading>

      <HGrid columns="50% 50%" gap="4">
        <NavigationButton
          disabled={!hasAdminAccess}
          href="/opprett-organisasjonsenhet"
          icon={<PlusCircleIcon fontSize="1.5rem" />}
        >
          Opprett ny enhet
        </NavigationButton>
        <NavigationButton
          disabled
          icon={<ArrowRightLeftIcon fontSize="1.5rem" />}
        >
          Flytt medarbeidere mellom enheter
        </NavigationButton>
        <NavigationButton disabled icon={<TrashIcon fontSize="1.5rem" />}>
          Legg ned en organisasjonsenhet
        </NavigationButton>
      </HGrid>
    </VStack>
  );
}

function ExternalLinkCards() {
  return (
    <HGrid columns="auto auto" gap="4">
      <ExternalLinkCard description="Gå til Mine tilganger" href="">
        Sett opp tilganger til en organisasjonsenhet
      </ExternalLinkCard>
      <ExternalLinkCard description="Gå til NORG" href="">
        Endring av informasjon om Nav-kontor
      </ExternalLinkCard>
      <ExternalLinkCard description="Gå til Unit4" href="">
        Registrér relaterte ting i nytt LPS
      </ExternalLinkCard>
      <ExternalLinkCard description="Send inn skjema i Porten" href="">
        Bestill endring av fysisk brukersted
      </ExternalLinkCard>
    </HGrid>
  );
}

function NavigationButton({
  children,
  href,
  icon,
  disabled = false,
}: {
  children: React.ReactNode;
  href?: string;
  icon?: any;
  disabled?: boolean;
}) {
  const buttonElement = (
    <Button
      css={css`
        justify-content: space-between;
        padding: var(--a-spacing-5);
        border-radius: 10px;
        background: var(--a-white);
        width: 100%;
      `}
      disabled={disabled}
      icon={icon}
      iconPosition="right"
      variant="secondary"
    >
      <span
        css={css`
          display: flex;
          width: 100%;
          font-size: var(--a-font-size-heading-small);
          line-height: var(--a-font-line-height-heading-small);
        `}
      >
        {children}
      </span>
    </Button>
  );

  if (!!href && !disabled) {
    return (
      <SmartLink
        css={css`
          width: 100%;

          :hover {
            width: 100%;
          }
        `}
        href={href}
      >
        {buttonElement}
      </SmartLink>
    );
  }

  return <>{buttonElement}</>;
}

function ExternalLinkCard({
  children,
  description,
  href,
}: {
  children: React.ReactNode;
  description: string;
  href: string;
}) {
  return (
    <LinkCard
      css={css`
        background: var(--a-surface-action-subtle);
      `}
    >
      <LinkCard.Title>
        <LinkCard.Anchor href={href} rel="noopener noreferrer" target="_blank">
          {children}
        </LinkCard.Anchor>
      </LinkCard.Title>
      <LinkCard.Description>{description}</LinkCard.Description>
    </LinkCard>
  );
}

function DineOrgEnheterSeksjon() {
  const hasAdminAccess = useHasAdminAccess();

  const [perspectiveSelected, setPerspectiveSelected] = useState("");

  const handlePerpectiveSelect = (input: string) =>
    setPerspectiveSelected(input);

  const getOrgEnheterNavidentKanRedigereQuery = useQuery({
    queryKey: getOrgEnheterNavidentKanRedigere.queryKey(perspectiveSelected),
    queryFn: () =>
      getOrgEnheterNavidentKanRedigere.queryFn(perspectiveSelected),
    enabled: !!perspectiveSelected,
  });

  const redigerbareOrgEnheter = getOrgEnheterNavidentKanRedigereQuery?.data;

  const sorterteOrgEnheter = redigerbareOrgEnheter?.sort((a, b) =>
    a.navn.localeCompare(b.navn),
  );

  return (
    <VStack gap="6">
      {hasAdminAccess && (
        <div
          css={css`
            width: 35%;
          `}
        >
          <form
            onSubmit={(e) => {
              e.preventDefault();
            }}
          >
            <Search
              autoComplete="off"
              hideLabel={false}
              label="Se perspektivet til ressurs"
              onSearchClick={handlePerpectiveSelect}
            />
          </form>
        </div>
      )}

      <Heading level="2" size="large">
        Dine organisasjonsenheter
      </Heading>

      {getOrgEnheterNavidentKanRedigereQuery.isLoading ? (
        <LoaderWithText>Laster...</LoaderWithText>
      ) : (
        <DineOrgEnheter orgEnheter={sorterteOrgEnheter} />
      )}
    </VStack>
  );
}

function DineOrgEnheter({
  orgEnheter,
}: {
  orgEnheter: OrgEnhet[] | undefined;
}) {
  const [searchInput, setSearchInput] = useState("");

  const filteredOrgEnheter = orgEnheter?.filter((orgEnhet) =>
    orgEnhet.navn.toLowerCase().includes(searchInput.toLowerCase()),
  );

  const handleClear = async () => {
    setSearchInput("");
  };

  const handleSearch = async (input: string) => {
    setSearchInput(input);
  };

  return (
    <VStack gap="4">
      <div
        css={css`
          width: 35%;
        `}
      >
        <form
          onSubmit={(e) => {
            e.preventDefault();
          }}
        >
          <Search
            autoComplete="off"
            clearButton
            hideLabel={false}
            label="Søk blant enheter"
            onClear={handleClear}
            onSearchClick={handleSearch}
            size="medium"
          />
        </form>
      </div>

      {filteredOrgEnheter?.map((orgEnhet) => (
        <OrgEnhetKort key={orgEnhet.id} orgEnhet={orgEnhet} />
      ))}
    </VStack>
  );
}

function OrgEnhetKort({ orgEnhet }: { orgEnhet: OrgEnhet }) {
  const testId: string[] = [orgEnhet.id];

  const ledereForOrgEnhetQuery = useQuery({
    queryKey: getLedereForOrgEnhet.queryKey(testId),
    queryFn: () => getLedereForOrgEnhet.queryFn(testId),
  });

  const ledereForOrgEnhetResponse = ledereForOrgEnhetQuery?.data ?? undefined;

  const ledereForOrgEnhet = ledereForOrgEnhetResponse?.[orgEnhet.id] ?? [];

  return (
    <ExpansionCard aria-label={orgEnhet.navn}>
      <ExpansionCard.Header>
        <HStack align="center" gap="space-16" wrap={false}>
          <div>
            <Buildings2FillIcon fontSize="3rem" title="a11y-title" />
          </div>
          <div>
            <ExpansionCard.Title>{orgEnhet.navn}</ExpansionCard.Title>
            <ExpansionCard.Description>
              {`Leder: ${
                orgEnhet?.ledere?.length > 0
                  ? formatVisningsnavn(orgEnhet.ledere[0].visningsnavn)
                  : ""
              }`}
            </ExpansionCard.Description>
          </div>
        </HStack>
      </ExpansionCard.Header>

      <ExpansionCard.Content>
        <VStack gap="4">
          <EnhetKontekstInfo displayLeder={false} orgEnhet={orgEnhet} />

          <HStack gap="4">
            <LabelWithValue
              label="Startdato"
              value={formatDisplayDate(orgEnhet.gyldigFom)}
            />
            <LabelWithValue
              label="Sluttdato"
              value={formatDisplayDate(orgEnhet.gyldigTom)}
            />
            <LabelWithValue label="Org.-nivå" value={orgEnhet.orgNiv} />
          </HStack>

          <HvemKanAdministrereSection kanAdministrere={ledereForOrgEnhet} />

          <div>
            <Divider size="small" />

            <CompactOrganisasjonHierarki orgEnhet={orgEnhet} />
          </div>
        </VStack>
      </ExpansionCard.Content>
    </ExpansionCard>
  );
}

function HvemKanAdministrereSection({
  kanAdministrere,
}: {
  kanAdministrere: MinimalRessurs[];
}) {
  return (
    <div>
      <BodyShort>
        <b>Administratorer for denne enheten:</b>
      </BodyShort>

      {kanAdministrere.length > 0 ? (
        <VStack>
          {kanAdministrere.map((ressurs, index) => (
            <SmartLink
              href={`/ressurs/${ressurs.navident}`}
              key={`${ressurs.navident}${index}`}
            >
              {ressurs.visningsnavn}
            </SmartLink>
          ))}
        </VStack>
      ) : (
        <BodyShort>Ingen administratorer registrert.</BodyShort>
      )}
    </div>
  );
}
