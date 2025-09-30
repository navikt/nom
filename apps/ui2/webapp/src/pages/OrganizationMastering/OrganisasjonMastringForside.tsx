import { css } from "@emotion/react";
import {
  ArrowRightLeftIcon,
  Buildings2FillIcon,
  PlusCircleIcon,
  TrashIcon,
} from "@navikt/aksel-icons";
import {
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

import { getOrgEnhetList } from "~/api/react-query-endpoints";
import { useHasAdminAccess } from "~/api/useGetSession";
import ContentWrapper from "~/components/ContentWrapper";
import { Divider } from "~/components/Divider";
import { LabelWithValue } from "~/components/LabelWithValue";
import { SmartLink } from "~/components/navigation/SmartLink";
import { PageBanner } from "~/components/PageBanner";
import { CompactOrganisasjonHierarki } from "~/pages/ResourcePage/CompactOrganisasjonHierarki";
import { EnhetKontekstInfo } from "~/pages/ResourcePage/Masseflytting/Masseflytting";
import type { OrgEnhet } from "~/types/api-models";
import { formatDisplayDate, formatVisningsnavn } from "~/utils";

export const OrganisasjonMastringForside = () => {
  const hasAdminAccess = useHasAdminAccess();

  if (!hasAdminAccess) {
    return;
  }

  return (
    <>
      <PageBanner text="Organisasjonsadmin dashboard" />

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
  return (
    <VStack gap="6">
      <Heading level="2" size="large">
        Administrering av organisasjonsenheter
      </Heading>

      <HGrid columns="50% 50%" gap="4">
        <NavigationButton
          disabled
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
  const orgEnhetIdList = ["pe225k", "ve565g", "ty476u", "ta261a", "ka383w"];

  const orgEnhetQuery = useQuery({
    queryKey: getOrgEnhetList.queryKey(orgEnhetIdList),
    queryFn: () => getOrgEnhetList.queryFn(orgEnhetIdList),
    enabled: orgEnhetIdList.length > 0,
  });

  const orgEnheter = orgEnhetQuery?.data;

  const sorterteOrgEnheter = orgEnheter?.sort((a, b) =>
    a.navn.localeCompare(b.navn),
  );

  return (
    <VStack gap="6">
      <Heading level="2" size="large">
        Dine organisasjonsenheter
      </Heading>

      <DineOrgEnheter orgEnheter={sorterteOrgEnheter} />
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

  return (
    <VStack gap="4">
      <div
        css={css`
          width: 35%;
        `}
      >
        <Search
          clearButton
          label="Finn enhet"
          onChange={(input) => setSearchInput(input)}
          size="medium"
          value={searchInput}
          variant="simple"
        />
      </div>

      {filteredOrgEnheter?.map((orgEnhet) => (
        <OrgEnhetKort key={orgEnhet.id} orgEnhet={orgEnhet} />
      ))}
    </VStack>
  );
}

function OrgEnhetKort({ orgEnhet }: { orgEnhet: OrgEnhet }) {
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
              {`Leder: ${formatVisningsnavn(orgEnhet.ledere[0].visningsnavn)}`}
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

          <div>
            <Divider size="small" />

            <CompactOrganisasjonHierarki orgEnhet={orgEnhet} />
          </div>
        </VStack>
      </ExpansionCard.Content>
    </ExpansionCard>
  );
}
