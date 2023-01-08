import type { ApolloError } from "@apollo/client";
import type { GraphQLErrors } from "@apollo/client/errors";
import { css } from "@emotion/css";
import { Alert, Heading, Loader } from "@navikt/ds-react";
import partition from "lodash/partition";
import sortBy from "lodash/sortBy";
import uniqBy from "lodash/uniqBy";
import type { RefObject } from "react";
import { Fragment } from "react";
import React from "react";
import { useParams } from "react-router";

import type { OrgEnhet, Ressurs } from "../../api/generated/graphql_generated";
import type { HentHierarkiQueryHookResult } from "../../api/generated/graphql_generated";
import * as Gq from "../../api/generated/graphql_generated";
import ContentWrapper from "../../components/ContentWrapper";
import { PageBanner } from "../../components/PageBanner";
import { BackFromRessurs } from "./components/BackFromRessurs";
import {
  DynamicColumnContainer,
  OrgEnhetView,
  OverEnheterDivider,
  ResourceView,
} from "./components/CardViews";
import { FocusedRessursView } from "./components/FocusedRessursView";
import { ValgtEnhet } from "./components/ValgtEnhet";

const scrollToReference = (reference: RefObject<HTMLDivElement>) => {
  if (reference.current) {
    window.scrollTo(0, reference.current.offsetTop - 100);
  }
};

function sortUnderenheterForHierarchyFrontPage(
  orgHierarchy: HentHierarkiQueryHookResult["data"]
) {
  const underEnheter = (orgHierarchy?.enhetsKoblinger?.underenheter ?? []).map(
    ({ organisasjonsenhet }) => organisasjonsenhet
  );

  const sortedUnderEnheter = sortBy(underEnheter, "navn");

  if (orgHierarchy?.enhet?.navn !== "NAV") {
    return sortedUnderEnheter;
  }

  const [direktoratet, linjene] = partition(
    sortedUnderEnheter,
    (enhet) => enhet?.navn === "Arbeids- og velferdsdirektoratet"
  );

  return [...direktoratet, ...linjene];
}

function getOverEnheterBackToRootRecursively(orgEnhet: OrgEnhet): OrgEnhet[] {
  if (!orgEnhet || !orgEnhet.organiseringer) {
    return [];
  }

  const firstOrganisering = orgEnhet.organiseringer[0]?.organisasjonsenhet;

  return firstOrganisering
    ? [...getOverEnheterBackToRootRecursively(firstOrganisering), orgEnhet]
    : [orgEnhet];
}

const deduplicateGraphqlErrors = (errors: GraphQLErrors) => {
  const result: Map<string, number> = new Map();
  for (const error of errors) {
    const message = error.message;
    result.set(message, (result.get(message) || 0) + 1);
  }
  return result;
};

const graphqlErrorWrapper = (errors: ApolloError, keyPrefix: string) => {
  return (
    <ul>
      {[...deduplicateGraphqlErrors(errors.graphQLErrors)].map(
        (error, index) => (
          <li key={`${keyPrefix}-error-${index}`}>
            {error[0]} ({error[1]})
          </li>
        )
      )}
    </ul>
  );
};

const OrganisasjonHierarkiPageInner = (properties: {
  data: HentHierarkiQueryHookResult["data"];
}) => {
  const { resourceId } = useParams();
  const ressursQuery = Gq.useHentRessursQuery({
    variables: {
      navident: resourceId,
    },
    skip: !resourceId,
    errorPolicy: "all",
  });

  const firstOverEnhet = (
    properties.data?.enhetsKoblinger?.overenheter ?? []
  ).map(({ organisasjonsenhet }) => organisasjonsenhet)[0];
  const overEnheter = firstOverEnhet
    ? getOverEnheterBackToRootRecursively(firstOverEnhet as OrgEnhet)
    : [];
  const underEnheter = sortUnderenheterForHierarchyFrontPage(properties.data);

  const scrollReference = React.useRef<HTMLDivElement>(null);
  React.useEffect(() => {
    if (overEnheter.length > 0) scrollToReference(scrollReference);
  }, []);

  const valgtEnhet = properties.data?.enhet;
  const ressurser = (properties.data?.ressursKoblinger?.koblinger ?? []).map(
    ({ ressurs }) => ressurs
  );

  const currentRessurs = ressursQuery.data?.ressurs;

  // If we are viewing a ressurs we must include valgEnhet in the overEnheter list.
  const allOverEnheter = currentRessurs
    ? [...overEnheter, valgtEnhet as OrgEnhet]
    : overEnheter;

  return (
    <div
      className={css`
        display: flex;
        flex-direction: column;
      `}
    >
      {allOverEnheter.map((overEnhet) => (
        <>
          <OrgEnhetView key={overEnhet.agressoId} orgEnhet={overEnhet} />
          <OverEnheterDivider />
        </>
      ))}

      {ressursQuery.loading && !currentRessurs && (
        <Loader
          className={css`
            align-self: center;
          `}
          size="2xlarge"
        />
      )}

      {ressursQuery.error && (
        <Alert variant="error">
          <Heading level="3" size="small">
            Kunne ikke hente ressurs
          </Heading>
          {graphqlErrorWrapper(ressursQuery.error, "ressursQuery")}
        </Alert>
      )}

      {currentRessurs && (
        <>
          <FocusedRessursView
            focusedRessurs={currentRessurs as Ressurs}
            scrollReference={scrollReference}
          />
          <BackFromRessurs
            ressursIsLeder={ressurser.length === 0}
            valgtEnhetUrl={`/org/${valgtEnhet?.agressoId}/${valgtEnhet?.orgNiv}`}
          />
        </>
      )}
      {!(currentRessurs || ressursQuery.loading) && (
        <>
          {valgtEnhet && (
            <ValgtEnhet
              scrollReference={scrollReference}
              valgtEnhet={valgtEnhet as OrgEnhet}
            />
          )}
          <DynamicColumnContainer
            shouldUseDynamicSecondColumn={underEnheter.length > 5}
          >
            {underEnheter.map((underEnhet, index) => (
              <Fragment key={underEnhet.agressoId}>
                {valgtEnhet?.navn === "NAV" && index === 0 && (
                  <Heading size="small">Direktoratet</Heading>
                )}
                {valgtEnhet?.navn === "NAV" && index === 1 && (
                  <Heading size="small">Linjene</Heading>
                )}
                <OrgEnhetView orgEnhet={underEnhet as OrgEnhet} />
              </Fragment>
            ))}
          </DynamicColumnContainer>

          <DynamicColumnContainer
            shouldUseDynamicSecondColumn={ressurser.length > 5}
          >
            {sortBy(
              uniqBy(ressurser, (ressurs) => ressurs.navident),
              "visningsnavn"
            ).map((ressurs) => (
              <ResourceView
                key={ressurs.navident}
                ressurs={ressurs as Ressurs}
              />
            ))}
          </DynamicColumnContainer>
        </>
      )}
    </div>
  );
};

function OrganisasjonsHierarkiPageOuter() {
  const { agressoId, orgNiv } = useParams();

  const hentHierarkiQuery = Gq.useHentHierarkiQuery({
    variables: {
      agressoId: agressoId ?? "",
      orgNiv: orgNiv ?? "",
    },
    errorPolicy: "all",
  });

  return (
    <>
      <PageBanner text="NAV organisasjon" />
      <ContentWrapper>
        <div
          className={css`
            display: grid;
          `}
        >
          {hentHierarkiQuery.loading && !hentHierarkiQuery.data ? (
            <div
              className={css`
                justify-self: center;
              `}
            >
              <Loader size="3xlarge" variant="neutral" />
            </div>
          ) : undefined}
          {hentHierarkiQuery.error && (
            <Alert variant="error">
              <Heading level="3" size="small">
                Kunne ikke hente hierarki
              </Heading>
            </Alert>
          )}
          {hentHierarkiQuery.data && (
            <OrganisasjonHierarkiPageInner data={hentHierarkiQuery.data} />
          )}
        </div>
      </ContentWrapper>
    </>
  );
}

export default OrganisasjonsHierarkiPageOuter;
