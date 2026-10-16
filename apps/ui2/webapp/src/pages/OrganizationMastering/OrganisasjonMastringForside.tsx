import { css } from "@emotion/react";
import {
  ArrowRightLeftIcon,
  ChevronDownIcon,
  ChevronUpIcon,
  PencilIcon,
  PlusCircleIcon,
  TrashIcon,
} from "@navikt/aksel-icons";
import {
  BodyShort,
  Button,
  Heading,
  HGrid,
  HStack,
  LinkCard,
  Search,
  UNSAFE_Combobox,
  VStack,
} from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import React, { useState } from "react";

import {
  getLedereForOrgEnhet,
  getOrgEnhet,
  getOrgEnheterLedetAvOptions,
  getOrgEnheterRessursKanRedigere,
  getOrgEnhetList,
  getRessurs,
  getRessurserWhichCanBeDelegatedTo,
  getTilgangerDelegertTilMegOptions,
} from "~/api/react-query-endpoints";
import {
  useGetSession,
  useHasAdminAccess,
  useHasAdminReadOnlyAccess,
  useHasWriteAccess,
} from "~/api/useGetSession";
import ContentWrapper from "~/components/ContentWrapper";
import { Divider } from "~/components/Divider";
import { EnhetKontekstInfo } from "~/components/EnhetKontekstInfo";
import { LabelWithValue } from "~/components/LabelWithValue";
import { LoaderWithText } from "~/components/LoaderWithText";
import { SmartLink } from "~/components/navigation/SmartLink";
import { PageBanner } from "~/components/PageBanner";
import { useUnleashToggle } from "~/hooks/useUnleashToggle";
import { nomDateIsTodayOrAfter } from "~/pages/ResourcePage/RessursComponents/dolUtils";
import type {
  OrgEnhet,
  RedigeringsTilgangRessursResponse,
} from "~/types/api-models";
import { formatDisplayDate, formatVisningsnavn, navToppnode } from "~/utils";

export const OrganisasjonMastringForside = () => {
  const hasAdminAccess = useHasAdminAccess();
  const hasWriteAccess = useHasWriteAccess();
  const hasAdminReadOnlyAccess = useHasAdminReadOnlyAccess();

  const ledetAvMeg = useGetSession().data?.navident
    ? (useQuery(
        getOrgEnheterLedetAvOptions(useGetSession().data?.navident as string),
      ).data ?? [])
    : [];

  const delegertTilMeg =
    useQuery(getTilgangerDelegertTilMegOptions()).data ?? [];

  const lederEllerDelegert =
    ledetAvMeg?.length > 0 || delegertTilMeg?.length > 0;

  const visInnhold =
    hasAdminAccess ||
    (hasWriteAccess && lederEllerDelegert) ||
    hasAdminReadOnlyAccess;

  if (!visInnhold) {
    return;
  }

  return (
    <>
      <PageBanner text="Administrering av organisasjonsenheter" />

      <ContentWrapper>
        <VStack gap="12">
          <AdministreringAvOrgEnheterSeksjon />

          <MineOrgEnheterSeksjon />
        </VStack>
      </ContentWrapper>
    </>
  );
};

function AdministreringAvOrgEnheterSeksjon() {
  const hasAdminAccess = useHasAdminAccess();

  return (
    <VStack gap="6">
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

function MineOrgEnheterSeksjon() {
  const hasAdminAccess = useHasAdminAccess();
  const hasAdminReadOnlyAccess = useHasAdminReadOnlyAccess();

  const visPerspektivSearch = hasAdminAccess || hasAdminReadOnlyAccess;

  const simpleSearchEnabled = useUnleashToggle(
    "nom-fe.orgmastring.simple-search",
  );
  const advancedSearchEnabled = useUnleashToggle(
    "nom-fe.orgmastring.advanced-search",
  );

  const [perspectiveSelected, setPerspectiveSelected] = useState("");

  const handlePerpectiveSelect = (input: string) =>
    setPerspectiveSelected(input);

  const redigerbareToppnoderQuery = useQuery({
    queryKey: getOrgEnheterRessursKanRedigere.queryKey(
      visPerspektivSearch ? perspectiveSelected : undefined,
      true,
    ),
    queryFn: () =>
      getOrgEnheterRessursKanRedigere.queryFn(
        visPerspektivSearch ? perspectiveSelected : undefined,
        true,
      ),
  });

  const valgtRessursQuery = useQuery({
    queryKey: getRessurs.queryKey(perspectiveSelected),
    queryFn: () => getRessurs.queryFn(perspectiveSelected),
    enabled: !!perspectiveSelected,
  });

  const valgtRessurs = valgtRessursQuery.data;

  const redigerbareToppnoder = redigerbareToppnoderQuery?.data;

  const toppnodeIder =
    redigerbareToppnoder
      ?.filter((orgEnhet) => nomDateIsTodayOrAfter(orgEnhet.gyldigTom))
      .map((orgEnhet) => orgEnhet.id) ?? [];

  const delegerbareRessurserQuery = useQuery({
    queryKey: getRessurserWhichCanBeDelegatedTo.queryKey,
    queryFn: () => getRessurserWhichCanBeDelegatedTo.queryFn(),
    enabled: advancedSearchEnabled && visPerspektivSearch,
  });

  const delegerbareRessurser = delegerbareRessurserQuery?.data;

  const alleOptions =
    delegerbareRessurser?.map((option) => {
      return {
        value: option.navident,
        label: `${option.visningsnavn} (${option.navident})`,
      };
    }) ?? [];

  return (
    <VStack gap="6">
      {visPerspektivSearch && (
        <>
          <VStack
            css={css`
              width: 35%;
            `}
            gap="4"
          >
            {simpleSearchEnabled && (
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
            )}

            {advancedSearchEnabled &&
              (delegerbareRessurserQuery.isLoading ? (
                <LoaderWithText>Laster...</LoaderWithText>
              ) : (
                <UNSAFE_Combobox
                  label="Se perspektivet til ledere eller lederstøtte"
                  onToggleSelected={handlePerpectiveSelect}
                  options={alleOptions}
                  size="medium"
                />
              ))}
          </VStack>

          {!!valgtRessurs && (
            <BodyShort
              css={css`
                margin-bottom: var(--a-spacing-3);
              `}
              size="large"
            >
              Perspektiv:
              <b>
                {" "}
                <SmartLink
                  href={`/ressurs/${valgtRessurs.navident}`}
                  target="_blank"
                >{`${formatVisningsnavn(valgtRessurs.visningsnavn)}`}</SmartLink>
                {` (${valgtRessurs.navident})`}
              </b>
            </BodyShort>
          )}
        </>
      )}

      <Heading level="2" size="large">
        Mine organisasjonsenheter
      </Heading>

      <MineOrgEnheter toppnodeIder={toppnodeIder} />
    </VStack>
  );
}

function MineOrgEnheter({ toppnodeIder }: { toppnodeIder: string[] }) {
  const orgEnheterQuery = useQuery({
    queryKey: getOrgEnhetList.queryKey(toppnodeIder),
    queryFn: () => getOrgEnhetList.queryFn(toppnodeIder),
    enabled: toppnodeIder.length > 0,
  });

  const orgEnheter = orgEnheterQuery?.data;

  if (toppnodeIder.length === 0) {
    return;
  }

  const sorterteOrgEnheter = orgEnheter?.sort((a, b) =>
    a.navn.localeCompare(b.navn, "no"),
  );

  if (orgEnheterQuery.isLoading) {
    return <LoaderWithText>Laster...</LoaderWithText>;
  }

  return (
    <VStack gap="4">
      {sorterteOrgEnheter?.map((orgEnhet) => (
        <UtvidOrgEnhetKort
          key={orgEnhet.id}
          orgEnhet={orgEnhet}
          underenheterDefaultOpen={sorterteOrgEnheter?.length === 1}
        />
      ))}
    </VStack>
  );
}

function UtvidOrgEnhetKort({
  orgEnhet,
  defaultOpen = false,
  underenheterDefaultOpen = false,
}: {
  orgEnhet: OrgEnhet;
  defaultOpen?: boolean;
  underenheterDefaultOpen?: boolean;
}) {
  const hasAdminAccess = useHasAdminAccess();

  const [expanded, setExpanded] = useState(defaultOpen);

  const [underenheterExpanded, setUnderenheterExpanded] = useState(
    underenheterDefaultOpen,
  );

  const ledereForOrgEnhetQuery = useQuery({
    queryKey: getLedereForOrgEnhet.queryKey([orgEnhet.id]),
    queryFn: () => getLedereForOrgEnhet.queryFn([orgEnhet.id]),
  });

  const ledereForOrgEnhetResponse = ledereForOrgEnhetQuery?.data ?? undefined;

  const ledereForOrgEnhet = ledereForOrgEnhetResponse?.[orgEnhet.id] ?? [];

  const underliggendeOrgEnheterQuery = useQuery({
    queryKey: getOrgEnhetList.queryKey(orgEnhet.underordnedeOrgEnhetId),
    queryFn: () => getOrgEnhetList.queryFn(orgEnhet.underordnedeOrgEnhetId),
    enabled: orgEnhet.underordnedeOrgEnhetId.length > 0,
  });

  const underliggendeOrgEnheter = underliggendeOrgEnheterQuery?.data;

  const sorterteOrgEnheter = underliggendeOrgEnheter?.sort((a, b) =>
    a.navn.localeCompare(b.navn, "no"),
  );

  return (
    <div
      css={css`
        width: 100%;
      `}
    >
      <div
        css={css`
          background: #eef6fc;
          border: 2px solid #457c9d;
          border-radius: var(--a-border-radius-large);
          padding: var(--a-spacing-4) var(--a-spacing-6);
          display: block;
        `}
      >
        <div
          css={css`
            width: 100%;
            display: flex;
            justify-content: space-between;
            margin-bottom: ${expanded ? "var(--a-spacing-4)" : "0"};
          `}
        >
          <div>
            <Heading level="3" size="medium">
              {orgEnhet.navn}
            </Heading>

            <BodyShort>
              Leder: <b>{orgEnhet?.leder?.visningsnavn ?? ""}</b>
            </BodyShort>
          </div>

          <Button
            icon={expanded ? <ChevronUpIcon /> : <ChevronDownIcon />}
            iconPosition="right"
            css={css`
              height: fit-content;
              align-self: center;
            `}
            onClick={() => setExpanded(!expanded)}
            type="button"
            variant="tertiary"
          />
        </div>

        {expanded && (
          <VStack gap="4">
            <Divider noMargins />

            <EnhetKontekstInfo
              displayKostnadssted
              displayLeder={false}
              displayUni4Niv
              orgEnhet={orgEnhet}
            />

            <HStack gap="4">
              <LabelWithValue
                label="Startdato"
                value={formatDisplayDate(orgEnhet.gyldigFom)}
              />
              <LabelWithValue
                label="Sluttdato"
                value={formatDisplayDate(orgEnhet.gyldigTom)}
              />
            </HStack>

            <SmartLink href={`/org/${orgEnhet.id}`}>
              Gå til enhetskortet i NOM-hierarkiet
            </SmartLink>

            <HvemKanRedigereSection kanAdministrere={ledereForOrgEnhet} />

            {hasAdminAccess && (
              <HStack
                css={css`
                  margin-left: auto;
                `}
                gap="2"
              >
                <SmartLink
                  href={`/korriger-navn-organisasjonsenhet?id=${orgEnhet.id}`}
                >
                  <Button
                    size="small"
                    icon={<PencilIcon />}
                    variant="primary"
                    iconPosition="right"
                  >
                    Endre navn
                  </Button>
                </SmartLink>

                <SmartLink
                  href={`/korriger-leder-organisasjonsenhet?id=${orgEnhet.id}`}
                >
                  <Button
                    size="small"
                    icon={<PencilIcon />}
                    variant="primary"
                    iconPosition="right"
                  >
                    Endre leder
                  </Button>
                </SmartLink>

                <SmartLink
                  href={`/korriger-organisasjonsenhet?id=${orgEnhet.id}`}
                >
                  <Button
                    size="small"
                    icon={<PencilIcon />}
                    variant="primary"
                    iconPosition="right"
                  >
                    Endre informasjon
                  </Button>
                </SmartLink>

                <SmartLink href={`/endre-overliggende-enhet?id=${orgEnhet.id}`}>
                  <Button
                    size="small"
                    icon={<PencilIcon />}
                    variant="primary"
                    iconPosition="right"
                  >
                    Endre overliggende enhet
                  </Button>
                </SmartLink>

                <SmartLink
                  href={`/avslutt-organisasjonsenhet?id=${orgEnhet.id}`}
                >
                  <Button
                    size="small"
                    icon={<TrashIcon />}
                    variant="primary"
                    iconPosition="right"
                  >
                    Avvikle enhet
                  </Button>
                </SmartLink>
              </HStack>
            )}
          </VStack>
        )}
      </div>

      {!!sorterteOrgEnheter && sorterteOrgEnheter?.length > 0 && (
        <div
          css={css`
            margin-left: var(--a-spacing-8);
            display: flex;
          `}
        >
          {underenheterExpanded ? (
            <VStack
              css={css`
                width: 100%;
              `}
              gap="0"
            >
              <div
                css={css`
                  height: var(--a-spacing-2);
                  border-left: 4px solid #457c9d;
                `}
              />

              {sorterteOrgEnheter.map((underliggendeOrgEnhet, index) => (
                <div
                  css={css`
                    display: flex;
                  `}
                  key={`${orgEnhet.id}${index}`}
                >
                  <TreeStructure
                    downward={index < sorterteOrgEnheter.length - 1}
                  />

                  <div
                    css={css`
                      width: 100%;
                      margin-top: var(--a-spacing-2);
                      margin-bottom: var(--a-spacing-2);
                    `}
                  >
                    <UtvidOrgEnhetKort orgEnhet={underliggendeOrgEnhet} />
                  </div>
                </div>
              ))}
            </VStack>
          ) : (
            <UnderenhetListIllustration
              onExpand={() => setUnderenheterExpanded(!underenheterExpanded)}
              length={sorterteOrgEnheter.length}
            />
          )}
        </div>
      )}
    </div>
  );
}

function HvemKanRedigereSection({
  kanAdministrere,
}: {
  kanAdministrere: RedigeringsTilgangRessursResponse[];
}) {
  const toppnodeOrgEnhetId = navToppnode();

  const toppnodeOrgEnhetQuery = useQuery({
    queryKey: getOrgEnhet.queryKey(toppnodeOrgEnhetId),
    queryFn: () => getOrgEnhet.queryFn(toppnodeOrgEnhetId),
  });

  const toppnodeOrgEnhet = toppnodeOrgEnhetQuery.data;

  const direktorNavident = toppnodeOrgEnhet?.leder.navident;

  const unikeFiltrerteRessurser = Array.from(
    kanAdministrere.reduce((map, ressurs) => {
      const existing = map.get(ressurs.navident);

      if (!existing || (existing.erDelegert && !ressurs.erDelegert)) {
        map.set(ressurs.navident, ressurs);
      }

      return map;
    }, new Map<string, RedigeringsTilgangRessursResponse>()),
    ([, ressurs]) => ressurs,
  ).filter((ressurs) => ressurs.navident !== direktorNavident);

  const ikkeDelegerteRessurser = unikeFiltrerteRessurser.filter(
    (ressurs) => !ressurs.erDelegert,
  );

  const delegerteRessurser = unikeFiltrerteRessurser.filter(
    (ressurs) => ressurs.erDelegert,
  );

  return (
    <VStack gap="2">
      <Heading size="small" level="4">
        Administratorer for enheten:
      </Heading>

      <HStack gap="8">
        <VStack gap="2">
          <BodyShort>
            <b>Leder</b>
          </BodyShort>

          {ikkeDelegerteRessurser.length > 0 ? (
            <VStack>
              {ikkeDelegerteRessurser.map((ressurs, index) => (
                <SmartLink
                  css={css`
                    width: fit-content;
                  `}
                  href={`/ressurs/${ressurs.navident}`}
                  key={`${ressurs.navident}${index}`}
                >
                  {ressurs.visningsnavn}
                </SmartLink>
              ))}
            </VStack>
          ) : (
            <BodyShort>Ingen registrert</BodyShort>
          )}
        </VStack>

        <VStack gap="2">
          <BodyShort>
            <b>Lederstøtte</b>
          </BodyShort>

          {delegerteRessurser.length > 0 ? (
            <VStack>
              {delegerteRessurser.map((ressurs, index) => (
                <SmartLink
                  css={css`
                    width: fit-content;
                  `}
                  href={`/ressurs/${ressurs.navident}`}
                  key={`${ressurs.navident}${index}`}
                >
                  {ressurs.visningsnavn}
                </SmartLink>
              ))}
            </VStack>
          ) : (
            <BodyShort>Ingen registrert</BodyShort>
          )}
        </VStack>
      </HStack>
    </VStack>
  );
}

function UnderenhetListIllustration({
  length,
  onExpand,
}: {
  length: number;
  onExpand: any;
}) {
  return (
    <div
      css={css`
        width: 100%;
      `}
    >
      <div
        css={css`
          height: 22px;
          border-left: 4px solid #457c9d;
        `}
      />

      <div
        css={css`
          margin-left: auto;
          position: relative;
          display: flex;
        `}
      >
        <div
          css={css`
            width: 16px;
          `}
        >
          <div
            css={css`
              height: 50%;
              border-left: 4px solid #457c9d;
              border-bottom: 2px solid #457c9d;
            `}
          />

          <div
            css={css`
              height: 50%;
              border-top: 2px solid #457c9d;
            `}
          />
        </div>

        <div
          css={css`
            width: 100%;
          `}
        >
          <div
            css={css`
              position: absolute;
              inset: 0;
              top: -14px;
              left: 44px;
              background: #eef6fc;
              border: 2px solid #457c9d;
              border-radius: var(--a-border-radius-large);
              z-index: 1;
            `}
          />

          <div
            css={css`
              position: absolute;
              inset: 0;
              top: -7px;
              left: 30px;
              background: #eef6fc;
              border: 2px solid #457c9d;
              border-radius: var(--a-border-radius-large);
              z-index: 2;
            `}
          />

          <div
            css={css`
              position: relative;
              padding: var(--a-spacing-1) var(--a-spacing-4);
              background: #eef6fc;
              border: 2px solid #457c9d;
              border-radius: var(--a-border-radius-large);
              z-index: 3;
            `}
          >
            <div
              css={css`
                display: flex;
                justify-content: space-between;
                align-items: center;
              `}
            >
              <BodyShort
                css={css`
                  font-size: var(--a-font-size-heading-small);
                  font-weight: var(--a-font-weight-bold);
                `}
              >
                {`+ ${length} underenhet${length > 1 ? "er" : ""}`}
              </BodyShort>

              <Button size="small" variant="secondary" onClick={onExpand}>
                Utvid
              </Button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function TreeStructure({ downward }: { downward: boolean }) {
  return (
    <div
      css={css`
        display: flex;
        flex-direction: column;
        height: 100%;
        width: 32px;
      `}
    >
      <div
        css={css`
          flex-shrink: 0;
          height: 54px;
          border-left: 4px solid #457c9d;
          border-bottom: 2px solid #457c9d;
        `}
      />

      <div
        css={css`
          flex: 1;
          border-top: 2px solid #457c9d;
          border-left: ${downward ? "solid 4px #457c9d" : ""};
        `}
      />
    </div>
  );
}
