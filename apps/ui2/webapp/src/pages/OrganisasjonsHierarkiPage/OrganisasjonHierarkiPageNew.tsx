import { css } from "@emotion/css";
import { Alert, Heading } from "@navikt/ds-react";
import partition from "lodash/partition";
import type { RefObject } from "react";
import { Fragment } from "react";
import React from "react";
import { useParams } from "react-router";
import { Link } from "react-router-dom";

import * as Gq from "../../api/generated/graphql_generated";
import type { Hierarchy } from "../../api/transformations/hierarchy";
import {
  focusedEnhetHierarchy,
  focusedRessursHierarchy,
} from "../../api/transformations/hierarchy";
import ContentWrapper from "../../components/ContentWrapper";
import { peopleFilledIcon } from "../../components/Images";
import { PageBanner } from "../../components/PageBanner";
import { agressoUrlToData } from "../../resources/utils/url_utils";
import { BackFromRessurs } from "./components/BackFromRessurs";
import { Enhet } from "./components/Enhet";
import { FocusedRessursView } from "./components/FocusedRessursView";
import { HorizontallyAlignedPanel } from "./components/HorizontallyAlignedPanel";
import { OverEnhet } from "./components/OverEnhet";
import { ValgtEnhet } from "./components/ValgtEnhet";
import { commonPanelCss, hierarkiCss, lederStyling, panelCss } from "./styles";

const scrollToReference = (reference: RefObject<HTMLDivElement>) => {
  if (reference.current) {
    window.scrollTo(0, reference.current.offsetTop - 100);
  }
};

function sortUnderenheterForHierarchyFrontPage(
  orgHierarchy: Hierarchy.OrgHierarchy
) {
  if (orgHierarchy.enhet.name !== "NAV") {
    return orgHierarchy.underEnheter;
  }

  const [direktoratet, linjene] = partition(
    orgHierarchy.underEnheter,
    (enhet) => enhet.name === "Arbeids- og velferdsdirektoratet"
  );

  return [...direktoratet, ...linjene];
}

const OrganisasjonHierarkiPageInner = (properties: {
  data: Hierarchy.OrgHierarchy;
  focusedRessurs?: Hierarchy.OrgHierarchyWithFocusedRessurs;
}) => {
  const overEnheter =
    properties.focusedRessurs?.overEnheter || properties.data.overEnheter;
  const scrollReference = React.useRef<HTMLDivElement>(null);
  React.useEffect(() => {
    if (overEnheter.length > 0) scrollToReference(scrollReference);
  }, []);

  const underEnheter = sortUnderenheterForHierarchyFrontPage(properties.data);
  const valgtEnhet = properties.data.enhet;
  const ressurser = properties.data.ressurser;

  const focusedRessurs = properties.focusedRessurs?.focusedRessurs;

  return (
    <div
      className={css`
        display: flex;
        flex-direction: column;
      `}
    >
      {overEnheter.map((overEnhet) => (
        <OverEnhet key={overEnhet.url} overEnhet={overEnhet} />
      ))}

      {focusedRessurs && (
        <>
          <FocusedRessursView
            focusedRessurs={focusedRessurs}
            scrollReference={scrollReference}
          />
          <BackFromRessurs
            ressursIsLeder={ressurser.length === 0}
            valgtEnhetUrl={valgtEnhet.url}
          />
        </>
      )}
      {!focusedRessurs && (
        <>
          {valgtEnhet && (
            <ValgtEnhet
              scrollReference={scrollReference}
              valgtEnhet={valgtEnhet}
            />
          )}
          <div
            className={css(
              hierarkiCss.underEnheter,
              underEnheter.length > 5 && hierarkiCss.dynamicSecondColumn
            )}
          >
            {underEnheter.map((underEnhet, index) => (
              <Fragment key={underEnhet.url}>
                {valgtEnhet.name === "NAV" && index === 0 && (
                  <Heading size="small">Direktoratet</Heading>
                )}
                {valgtEnhet.name === "NAV" && index === 1 && (
                  <Heading size="small">Linjene</Heading>
                )}
                <Link className={hierarkiCss.link} to={underEnhet.url}>
                  <Enhet navIdentForImg={underEnhet.ledere[0]?.navident}>
                    <Heading size={"medium"}>{underEnhet.name}</Heading>
                    <span>
                      Leder:{" "}
                      <span className={lederStyling.lederTekst}>
                        {underEnhet.ledere[0]?.name || "Leder ikke funnet"}
                      </span>
                    </span>
                  </Enhet>
                </Link>
              </Fragment>
            ))}
          </div>
          <div
            className={css(
              hierarkiCss.underEnheter,
              ressurser.length > 5 && hierarkiCss.dynamicSecondColumn
            )}
          >
            {ressurser.map((ressurs) => (
              <Link
                className={hierarkiCss.link}
                key={ressurs.navident}
                to={valgtEnhet.url + "/" + ressurs.navident}
              >
                <HorizontallyAlignedPanel
                  className={css(commonPanelCss, panelCss.fullHeight)}
                  iconSrc={peopleFilledIcon}
                >
                  <Heading size="medium">{ressurs.name}</Heading>
                </HorizontallyAlignedPanel>
              </Link>
            ))}
          </div>
        </>
      )}
    </div>
  );
};

function OrganisasjonsHierarkiPageOuter() {
  const { id, resourceId } = useParams();
  const agrId = agressoUrlToData(id || "");

  const hentHierarkiQuery = Gq.useHentHierarkiQuery({
    variables: {
      agressoId: agrId.agressoId,
      orgNiv: agrId.agressoNiva,
    },
    errorPolicy: "all",
  });

  const ressursQry = Gq.useHentRessursQuery({
    variables: {
      navident: resourceId,
    },
    skip: !resourceId,
  });

  const gotHierarkiData = !!(
    hentHierarkiQuery.data && !hentHierarkiQuery.loading
  );
  const gotRessursData =
    !resourceId || !!(ressursQry.data?.ressurs && !ressursQry.loading);

  if (hentHierarkiQuery.error) {
    console.error(hentHierarkiQuery.error, "GRAPHQL ERROR");
  }
  if (ressursQry.error) {
    console.error(ressursQry.error, "GRAPHQL ERROR");
  }

  const enhetHierarchy = gotHierarkiData
    ? focusedEnhetHierarchy(hentHierarkiQuery)
    : undefined;
  const enhetHierarchyOk = enhetHierarchy && enhetHierarchy.kind !== "err";

  const focusedResourceHierarchy =
    resourceId && gotRessursData && enhetHierarchyOk
      ? focusedRessursHierarchy(enhetHierarchy, ressursQry)
      : undefined;
  const focusedResourceHierarchyOk =
    focusedResourceHierarchy && focusedResourceHierarchy?.kind !== "err";

  const missingData =
    !hentHierarkiQuery.loading &&
    !ressursQry.loading &&
    (!gotRessursData || !gotRessursData);

  const showAlert =
    hentHierarkiQuery.error ||
    ressursQry.error ||
    (!hentHierarkiQuery.loading && !enhetHierarchyOk) ||
    (resourceId && !ressursQry.loading && !focusedResourceHierarchyOk);

  return (
    <>
      <PageBanner text="NAV organisasjon" />
      <ContentWrapper>
        <div
          className={css`
            display: grid;
          `}
        >
          {showAlert && (
            <>
              <Alert variant={"info"}>Uventet data i graphql-respons</Alert>
              <hr />
            </>
          )}
          {!gotHierarkiData && !gotRessursData && (
            <>
              <div>Loading...</div>
            </>
          )}
          {missingData && (
            <Alert variant={"warning"}>
              Fant ikke data på IDer <br />
              {JSON.stringify(
                resourceId
                  ? { orgenhetId: id, ressursId: resourceId }
                  : { orgenhetId: id },
                undefined,
                2
              )}
            </Alert>
          )}
          {gotHierarkiData && gotRessursData && enhetHierarchyOk && (
            <OrganisasjonHierarkiPageInner
              data={enhetHierarchy}
              focusedRessurs={
                focusedResourceHierarchyOk
                  ? focusedResourceHierarchy
                  : undefined
              }
            />
          )}
        </div>
      </ContentWrapper>
    </>
  );
}

export default OrganisasjonsHierarkiPageOuter;
