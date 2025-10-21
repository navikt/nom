import { css } from "@emotion/react";
import { PlusCircleIcon } from "@navikt/aksel-icons";
import {
  BodyShort,
  Button,
  Heading,
  HStack,
  Loader,
  VStack,
} from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import React, { useState } from "react";

import { getOrgEnhet, getOrgEnhetList } from "~/api/react-query-endpoints";
import { useHasAdminAccess } from "~/api/useGetSession";
import ContentWrapper from "~/components/ContentWrapper";
import { PageBanner } from "~/components/PageBanner";
import { ContainerWithHighlightedTop } from "~/pages/CreateResourcePage/components/ContainerWithHighlightedTop";
import { OrgEnhetVisning } from "~/pages/ResourcePage/Masseflytting/Masseflytting";
import type { OrgEnhet } from "~/types/api-models";
import { isInDevelopment } from "~/utils";

export const OpprettOrgEnhet = () => {
  const hasAdminAccess = useHasAdminAccess();

  const [selectedOrgEnhet, setSelectedOrgEnhet] = useState<string | null>(null);

  const handleOpprettHerClick = (id: string) => setSelectedOrgEnhet(id);

  if (!hasAdminAccess) {
    return;
  }

  return (
    <>
      <PageBanner text="Opprett organisasjonsenhet" />

      <ContentWrapper>
        <ContainerWithHighlightedTop padding={6}>
          <Heading level="2" size="small">
            1. Plassering
          </Heading>

          <OrgHierarki handleOpprettHerClick={handleOpprettHerClick} />

          {!!selectedOrgEnhet && (
            <OrgEnhetVisningWrapper orgEnhetId={selectedOrgEnhet} />
          )}
        </ContainerWithHighlightedTop>
      </ContentWrapper>
    </>
  );
};

function OrgHierarki({
  handleOpprettHerClick,
}: {
  handleOpprettHerClick?: any;
}) {
  const inDevelopment = isInDevelopment();

  const developmentLenker = ["fa965e", "me177r", "ju219d", "hu797w", "tu734a"];
  const productionLenker = ["bu431e", "ry630r", "te464t", "se232x", "ne994m"];

  const topOrgEnhetIder = inDevelopment ? developmentLenker : productionLenker;

  const topOrgEnhetQuery = useQuery({
    queryKey: getOrgEnhetList.queryKey(topOrgEnhetIder),
    queryFn: () => getOrgEnhetList.queryFn(topOrgEnhetIder),
    enabled: topOrgEnhetIder.length > 0,
  });

  const topOrgEnheter = (topOrgEnhetQuery.data ?? []).sort(
    (a, b) => topOrgEnhetIder.indexOf(a.id) - topOrgEnhetIder.indexOf(b.id),
  );

  const [selectedId, setSelectedId] = useState<string | null>(null);

  const handleClick = (id: string) => setSelectedId(id);

  return (
    <div
      css={css`
        width: 100%;
      `}
    >
      <div
        css={css`
          width: 20%;
          margin: auto auto var(--a-spacing-8);
          padding: 0 var(--a-spacing-2) 0;
        `}
      >
        <OrgHierarkiCard name="Nav" onClick={handleClick} selected />
      </div>

      <HStack gap="4" wrap={false}>
        {topOrgEnheter.map((orgEnhet) => (
          <OrgHierarkiStack
            handleOpprettHerClick={handleOpprettHerClick}
            key={orgEnhet.id}
            onClick={handleClick}
            orgEnhet={orgEnhet}
            selected={orgEnhet.id === selectedId}
          />
        ))}
      </HStack>
    </div>
  );
}

function OrgHierarkiStack({
  orgEnhet,
  onClick,
  selected = false,
  handleOpprettHerClick,
}: {
  orgEnhet?: OrgEnhet;
  onClick?: any;
  selected?: boolean;
  handleOpprettHerClick?: any;
}) {
  const underliggendeOrgEnhetIder = orgEnhet?.underordnedeOrgEnhetId;

  const underliggendeOrgEnheterQuery = useQuery({
    queryKey: getOrgEnhetList.queryKey(underliggendeOrgEnhetIder ?? []),
    queryFn: () => getOrgEnhetList.queryFn(underliggendeOrgEnhetIder ?? []),
    enabled:
      !!underliggendeOrgEnhetIder && underliggendeOrgEnhetIder.length > 0,
  });

  const underliggendeOrgEnheter = underliggendeOrgEnheterQuery.data ?? [];

  const [selectedId, setSelectedId] = useState<string | null>(null);

  const handleClick = (id: string) => setSelectedId(id);

  return (
    <VStack
      css={css`
        width: 100%;
      `}
      gap="0"
    >
      <OrgHierarkiCard
        name={orgEnhet?.navn ?? ""}
        onClick={() => {
          onClick?.(orgEnhet?.id);
          if (!!selectedId) {
            setSelectedId(null);
          }
        }}
        selected={selected}
      />

      {selected && !selectedId && (
        <VStack
          css={css`
            width: 100%;
            margin-top: var(--a-spacing-4);
          `}
          gap="4"
        >
          {underliggendeOrgEnheter.map((underliggendeOrgEnhet) => (
            <OrgHierarkiStack
              key={underliggendeOrgEnhet.id}
              onClick={handleClick}
              orgEnhet={underliggendeOrgEnhet}
            />
          ))}

          <OpprettEnhetHerButton
            onClick={handleOpprettHerClick}
            plassering={orgEnhet?.id ?? ""}
          />
        </VStack>
      )}

      {selected && !!selectedId && (
        <>
          <OrgHierarkiLine />

          <OrgHierarkiStack
            handleOpprettHerClick={handleOpprettHerClick}
            onClick={handleClick}
            orgEnhet={underliggendeOrgEnheter.find(
              (underliggendeOrgEnhet) =>
                underliggendeOrgEnhet.id === selectedId,
            )}
            selected
          />
        </>
      )}
    </VStack>
  );
}

function OrgHierarkiCard({
  name,
  onClick,
  selected = false,
}: {
  name: string;
  onClick?: any;
  selected?: boolean;
}) {
  return (
    <Button
      css={css`
        display: flex;
        justify-content: center;
        width: 100%;
        padding: ${selected ? "20px 10px 20px" : "10px"};
        background: #cce2f0;
        border-radius: 5px;
        border-top: ${selected ? "7px solid #236b7d" : "none"};
        font-size: var(--a-font-size-medium) !important;
      `}
      onClick={onClick}
      size="small"
      variant="tertiary"
    >
      {name}
    </Button>
  );
}

function OpprettEnhetHerButton({
  plassering,
  onClick,
}: {
  plassering: string;
  onClick?: any;
}) {
  return (
    <Button
      css={css`
        justify-content: space-between;
      `}
      icon={<PlusCircleIcon fontSize="36" title="Opprett ny enhet her" />}
      iconPosition="right"
      onClick={() => onClick?.(plassering)}
      variant="secondary"
    >
      Opprett ny
      <br />
      enhet her
    </Button>
  );
}

function OrgHierarkiLine() {
  return (
    <div
      css={css`
        margin: auto;
        width: 4px;
        height: var(--a-spacing-4);
        background: #236b7d;
      `}
    />
  );
}

function OrgEnhetVisningWrapper({ orgEnhetId }: { orgEnhetId?: string }) {
  const orgEnhetQuery = useQuery({
    queryKey: getOrgEnhet.queryKey(orgEnhetId ?? ""),
    queryFn: () => getOrgEnhet.queryFn(orgEnhetId ?? ""),
    enabled: !!orgEnhetId,
  });

  const orgEnhet = orgEnhetQuery.data;

  if (orgEnhetQuery.isLoading) {
    return (
      <div
        css={css`
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: var(--a-spacing-2);
        `}
      >
        <Loader size="3xlarge" />
        <BodyShort size="large">Laster...</BodyShort>
      </div>
    );
  }

  return (
    !!orgEnhet && (
      <div
        css={css`
          background-color: #deecef;
          padding: var(--a-spacing-4) var(--a-spacing-6);
          border-radius: var(--a-border-radius-large);
          display: grid;
        `}
      >
        <OrgEnhetVisning
          heading="Enheten blir opprettet under"
          orgEnhet={orgEnhet}
        />
      </div>
    )
  );
}
