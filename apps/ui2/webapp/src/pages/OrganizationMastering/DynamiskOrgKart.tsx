import { css } from "@emotion/react";
import { PlusCircleIcon } from "@navikt/aksel-icons";
import { Button, Heading, HStack, VStack } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import React, { useState } from "react";

import { getOrgEnhet, getOrgEnhetList } from "~/api/react-query-endpoints";
import { LoaderWithText } from "~/components/LoaderWithText";
import { SmartLink } from "~/components/navigation/SmartLink";
import { ContainerWithHighlightedTop } from "~/pages/CreateResourcePage/components/ContainerWithHighlightedTop";
import type { OrgEnhet } from "~/types/api-models";

export function DynamiskOrgKart({
  title,
  toppOrgEnhetId,
  opprettKnapp,
}: {
  title?: string;
  toppOrgEnhetId: string;
  opprettKnapp?: boolean;
}) {
  const toppOrgEnhetQuery = useQuery({
    queryKey: getOrgEnhet.queryKey(toppOrgEnhetId),
    queryFn: () => getOrgEnhet.queryFn(toppOrgEnhetId),
  });

  const toppOrgEnhet = toppOrgEnhetQuery.data;

  const underliggendeOrgEnhetIder = toppOrgEnhet?.underordnedeOrgEnhetId;

  const underliggendeOrgEnheterQuery = useQuery({
    queryKey: getOrgEnhetList.queryKey(underliggendeOrgEnhetIder ?? []),
    queryFn: () => getOrgEnhetList.queryFn(underliggendeOrgEnhetIder ?? []),
    enabled:
      !!underliggendeOrgEnhetIder && underliggendeOrgEnhetIder.length > 0,
  });

  const underliggendeOrgEnheter = (
    underliggendeOrgEnheterQuery.data ?? []
  ).sort((a, b) => a.navn.localeCompare(b.navn));

  if (toppOrgEnhetQuery.isLoading || underliggendeOrgEnheterQuery.isLoading) {
    return (
      <ContainerWithHighlightedTop padding={6}>
        <LoaderWithText>Laster...</LoaderWithText>
      </ContainerWithHighlightedTop>
    );
  }

  return (
    <ContainerWithHighlightedTop padding={6}>
      {!!title && (
        <Heading level="2" size="medium">
          {title}
        </Heading>
      )}

      <div>
        <OrgKart
          opprettKnapp={opprettKnapp}
          toppOrgEnhet={toppOrgEnhet}
          toppUnderliggendeOrgenheter={underliggendeOrgEnheter}
        />
      </div>
    </ContainerWithHighlightedTop>
  );
}

function OrgKart({
  toppOrgEnhet,
  toppUnderliggendeOrgenheter,
  opprettKnapp,
}: {
  toppOrgEnhet: OrgEnhet | undefined;
  toppUnderliggendeOrgenheter: OrgEnhet[];
  opprettKnapp?: boolean;
}) {
  const [selectedId, setSelectedId] = useState<string | null>(null);

  return (
    <div
      css={css`
        overflow-x: auto;
        padding-bottom: var(--a-spacing-10);
      `}
    >
      <div
        css={css`
          width: max-content;
          min-width: 100%;
        `}
      >
        <OrgKartLargeCard
          name={toppOrgEnhet?.navn ?? ""}
          onClick={() => setSelectedId(null)}
          selected
        />

        <HStack gap="4" wrap={false}>
          {toppUnderliggendeOrgenheter?.map((orgEnhet) => (
            <div
              css={css`
                flex: 1;
              `}
              key={orgEnhet.id}
            >
              <OrgKartStack
                onClick={() => setSelectedId(orgEnhet.id)}
                opprettKnapp={opprettKnapp}
                orgEnhet={orgEnhet}
                selected={orgEnhet.id === selectedId}
                topmost
              />
            </div>
          ))}
        </HStack>
      </div>
    </div>
  );
}

function OrgKartStack({
  orgEnhet,
  selected,
  onClick,
  topmost,
  opprettKnapp,
}: {
  orgEnhet?: OrgEnhet;
  selected?: boolean;
  onClick?: any;
  topmost?: boolean;
  opprettKnapp?: boolean;
}) {
  const underliggendeOrgEnhetIder = orgEnhet?.underordnedeOrgEnhetId;

  const underliggendeOrgEnheterQuery = useQuery({
    queryKey: getOrgEnhetList.queryKey(underliggendeOrgEnhetIder ?? []),
    queryFn: () => getOrgEnhetList.queryFn(underliggendeOrgEnhetIder ?? []),
    enabled:
      !!underliggendeOrgEnhetIder && underliggendeOrgEnhetIder.length > 0,
  });

  const underliggendeOrgEnheter = (
    underliggendeOrgEnheterQuery.data ?? []
  ).sort((a, b) => a.navn.localeCompare(b.navn));

  const [selectedId, setSelectedId] = useState<string | null>(null);

  const handleSelect = (id: string) => setSelectedId(id);

  return (
    <>
      {(selected || topmost) && <OrgKartLine invisible={!selected} />}

      <OrgKartLargeCard
        name={orgEnhet?.navn ?? ""}
        onClick={() => {
          onClick?.();

          if (!!selectedId) {
            setSelectedId(null);
          }
        }}
        selected={selected}
      />

      {selected &&
        !selectedId &&
        (underliggendeOrgEnheter.length > 0 || opprettKnapp) && (
          <UnderliggendeOrgEnheterSelect
            handleSelect={handleSelect}
            opprettKnapp={opprettKnapp}
            orgEnhetId={orgEnhet?.id ?? ""}
            underliggendeOrgEnheter={underliggendeOrgEnheter}
          />
        )}

      {selected && !!selectedId && (
        <OrgKartStack
          opprettKnapp={opprettKnapp}
          orgEnhet={underliggendeOrgEnheter.find(
            (underliggendeOrgEnhet) => underliggendeOrgEnhet.id === selectedId,
          )}
          selected
        />
      )}
    </>
  );
}

function UnderliggendeOrgEnheterSelect({
  orgEnhetId,
  underliggendeOrgEnheter,
  handleSelect,
  opprettKnapp,
}: {
  orgEnhetId: string;
  underliggendeOrgEnheter: OrgEnhet[];
  handleSelect: any;
  opprettKnapp?: boolean;
}) {
  return (
    <VStack>
      <div
        css={css`
          height: 5px;
          margin-left: 16px;
          border-left: 4px solid #236b7d;
        `}
      />

      {opprettKnapp && (
        <div
          css={css`
            display: flex;
          `}
        >
          <OrgKartTreeStructure
            downward={underliggendeOrgEnheter?.length > 0}
          />

          <div
            css={css`
              width: 100%;
              margin-top: 5px;
              margin-bottom: 5px;
            `}
          >
            <OrgKartOpprettButton orgEnhetId={orgEnhetId} />
          </div>
        </div>
      )}

      {underliggendeOrgEnheter.map((underliggendeOrgEnhet, index) => (
        <div
          css={css`
            display: flex;
          `}
          key={underliggendeOrgEnhet.id}
        >
          <OrgKartTreeStructure
            downward={index < underliggendeOrgEnheter.length - 1}
          />

          <div
            css={css`
              width: 100%;
              margin-top: 5px;
              margin-bottom: 5px;
            `}
          >
            <OrgKartSmallCard
              name={underliggendeOrgEnhet.navn}
              onClick={() => handleSelect?.(underliggendeOrgEnhet.id)}
            />
          </div>
        </div>
      ))}
    </VStack>
  );
}

function OrgKartTreeStructure({ downward }: { downward: boolean }) {
  return (
    <div
      css={css`
        margin-left: 16px;
      `}
    >
      <div
        css={css`
          height: 50%;
          width: 16px;
          border-left: solid 4px #236b7d;
          border-bottom: solid 2px #236b7d;
        `}
      />

      <div
        css={css`
          width: 16px;
          height: 50%;
          border-top: solid 2px #236b7d;
          border-left: ${downward ? "solid 4px #236b7d" : ""};
        `}
      />
    </div>
  );
}

function OrgKartLargeCard({
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
        width: 100%;
        padding: ${selected ? "20px 12px 20px" : "10px"};
        background: #cce2f0;
        border-radius: 5px;
        border-top: ${selected ? "7px solid #236b7d" : "none"};
      `}
      onClick={onClick}
      size="small"
      variant="tertiary"
    >
      {name === "NAV" ? "Nav" : name}
    </Button>
  );
}

function OrgKartSmallCard({ name, onClick }: { name: string; onClick?: any }) {
  return (
    <Button
      css={css`
        width: 100%;
        padding: 8px;
        background: #eef6fc;
        border-radius: 5px;
        border: 1px solid #236b7d;
      `}
      onClick={onClick}
      size="small"
      variant="tertiary"
    >
      {name === "NAV" ? "Nav" : name}
    </Button>
  );
}

function OrgKartOpprettButton({ orgEnhetId }: { orgEnhetId: string }) {
  return (
    <SmartLink href={`/opprett-orgenhet?id=${orgEnhetId}`}>
      <Button
        css={css`
          color: #0067c5;
          width: 100%;
          padding: 8px 16px;
          border-radius: 5px;
          border: 2px solid #0067c5;
          gap: 0;
        `}
        icon={<PlusCircleIcon fontSize="2rem" />}
        iconPosition="right"
        size="small"
        variant="tertiary"
      >
        <span
          css={css`
            display: inline-block;
            white-space: nowrap;
          `}
        >
          Opprett ny
        </span>{" "}
        <span
          css={css`
            display: inline-block;
            white-space: nowrap;
          `}
        >
          enhet her
        </span>
      </Button>
    </SmartLink>
  );
}

function OrgKartLine({ invisible }: { invisible?: boolean }) {
  return (
    <div
      css={css`
        margin: auto;
        width: 4px;
        height: var(--a-spacing-6);
        background: ${invisible ? "transparent" : "#236b7d"};
      `}
    />
  );
}
