import { css } from "@emotion/css";
import { Back, Next } from "@navikt/ds-icons";
import { Alert, BodyShort, Heading } from "@navikt/ds-react";
import partition from "lodash/partition";
import type { ReactNode, RefObject } from "react";
import React, { Fragment } from "react";
import { useParams } from "react-router";
import { Link } from "react-router-dom";

import * as Gq from "../api/generated/graphql_generated";
import type { Hierarchy } from "../api/transformations/hierarchy";
import {
  focusedEnhetHierarchy,
  focusedRessursHierarchy,
} from "../api/transformations/hierarchy";
import ContentWrapper from "../components/ContentWrapper";
import { building2Icon, peopleFilledIcon } from "../components/Images";
import { PageBanner } from "../components/PageBanner";
import UserImage from "../components/UserImage";
import { agressoUrlToData } from "../resources/utils/url_utils";

const colorPalette = {
  rim: "var(--navds-global-color-lightblue-800)",
  focusedBackground: "var(--nom-secondary-hover)",
  unfocusedBackground: "var(--navds-global-color-white)",
  panelBorder: "var(--navds-global-color-gray-400)",
  font: "var(--navds-global-color-gray-900)",
};

const BuildingSvg = (properties: { className?: string }) => (
  <svg
    className={properties.className || ""}
    fill="none"
    height="24"
    viewBox="0 0 18 18"
    width="24"
    xmlns="http://www.w3.org/2000/svg"
  >
    <path
      clipRule="evenodd"
      d={`M15.6405 1.611C15.6405 1.611 15.6397 1.6095 15.639 1.6095L14.1397 
        0.11025C14.07 0.03975 13.9747 0 13.875 0H4.125C4.02525 0 3.93 0.03975 3.8595 0.11025L2.3595 1.6095C2.35875
        1.61025 2.35875 1.611 2.358 1.611C2.289 1.68225 2.25 1.7955 2.25 1.875V17.625C2.25 17.832 2.418 18 2.625
        18H7.5V14.625C7.5 14.418 7.668 14.25 7.875 14.25H10.125C10.332 14.25 10.5 14.418 10.5
        14.625V18H15.375C15.582 18 15.75 17.832 15.75 17.625V1.875C15.75 1.79625 15.7095 1.67925 15.6405 1.611ZM6
        12.75H3.75V10.5H6V12.75ZM6 9H3.75V6.75H6V9ZM6 5.25H3.75V3H6
        V5.25ZM10.5 12.75H7.5V10.5H10.5V12.75ZM10.5
        9H7.5V6.75H10.5V9ZM10.5 5.25H7.5V3H10.5V5.25ZM14.25 12.75H12V10.5H14.25V12.75ZM14.25 9H12V6.75H14.25V9ZM14.25
        5.25H12V3H14.25V5.25ZM3.53025 1.5L4.28025 0.75H13.7198L14.4698 1.5H3.53025Z`}
      fillRule="evenodd"
    />
  </svg>
);

const nextChevronCss = css`
  margin: 0 1rem 0 0.2rem;
  height: 2rem;
  width: 2rem;
`;

const commonPanelCss = css`
  border-radius: 5px;
  overflow: hidden;
  display: grid;
  min-height: 1rem;
  min-width: 1rem;
  grid-template-columns: min-content 1fr min-content min-content;
  background-color: ${colorPalette.unfocusedBackground};
  align-items: center;
  border: 1px solid ${colorPalette.panelBorder};

  :hover {
    background-color: ${colorPalette.focusedBackground};

    ${"." + nextChevronCss} {
      transform: translateX(0.6rem);
    }
  }
`;

const panelCss = {
  currentEnhet: css(
    commonPanelCss,
    css`
      grid-template-columns: 1fr;
      grid-template-rows: min-content 1fr;
      margin-bottom: 1rem;
      min-height: 13rem;
      background-color: ${colorPalette.focusedBackground};

      svg {
        // meant for header prefix icon
        fill: ${colorPalette.rim};
        margin: 0 0.2rem 0 0;
      }
    `
  ),
  focusedRessurs: css(css`
    grid-template-columns: 1fr;
    width: 100%;
    //justify-items: center;
    background-color: ${colorPalette.focusedBackground};
    row-gap: 0.5rem;
    min-height: 10rem;
    margin-bottom: 0;
    padding: var(--navds-spacing-8) var(--navds-spacing-8) 0;
  `),
  underEnhet: css(
    commonPanelCss,
    css`
      height: 100%;
      min-height: 5rem;
      word-wrap: anywhere;
    `
  ),
  panelContent: css`
    padding: 1rem;
  `,
  currentEnhetPanelContent: css`
    padding: 0.5rem 2rem;
    display: grid;
    grid-template-columns: 1fr min-content;
  `,
  panelRim: css`
    min-width: 0.5rem;
    min-height: 0.5rem;
    background-color: ${colorPalette.rim};
    display: grid;
    align-items: center;
    justify-items: center;
    height: 100%;
    width: 100%;

    img {
      padding: 0.2rem;
    }
  `,
  textCenter: css`
    text-align: center;
  `,
  textRight: css`
    text-align: right;
  `,
  fullHeight: css`
    height: 100%;
  `,
  labeledItemsPane: css`
    display: grid;
    grid-template-columns: auto;
    column-gap: 1rem;
    row-gap: 1rem;
    width: 100%;
    padding: 1.5rem 0;
    @media (min-width: 80rem) {
      grid-template-columns: auto auto;
      width: auto;
      //span:nth-child(odd){
      //  text-align: right;
      //}
    }
  `,
};

const lederStyling = {
  lederTekst: css`
    color: ${colorPalette.font};
    margin-left: 0.3rem;
  `,
  lederDiv: css`
    display: flex;
    align-items: center;
  `,
};

const hierarkiCss = {
  underEnheter: css`
    display: grid;
    grid-template-rows: min-content;
    column-gap: 1rem;
    row-gap: 1rem;
    grid-template-columns: 1fr;
  `,
  link: css`
    text-decoration: none;
    color: inherit;
    display: grid;
  `,
  dynamicSecondColumn: css`
    @media (min-width: 60rem) {
      grid-template-columns: 1fr 1fr;
    }
  `,
};

function OverenheterDivider() {
  return (
    <div
      className={css`
        background-color: inherit;
        display: grid;
        justify-items: center;
      `}
    >
      <div
        className={css`
          min-height: 1rem;
          height: 1rem;
          background-color: ${colorPalette.rim};
          width: 0.08rem;
        `}
      ></div>
    </div>
  );
}

const HorizontallyAlignedPanel = (properties: {
  children: ReactNode;
  iconSrc: string;
  className: string;
  navidentForImg?: string;
  fornavnAndEtternavnForImg?: string;
}) => (
  <div className={properties.className}>
    <div className={panelCss.panelRim}>
      {properties.iconSrc && (
        <img alt={"src=" + properties.iconSrc} src={properties.iconSrc} />
      )}
    </div>
    <div className={panelCss.panelContent}>{properties.children}</div>
    {properties.navidentForImg && (
      <UserImage
        navIdent={properties.navidentForImg}
        navn={properties.fornavnAndEtternavnForImg}
      />
    )}
    <Next className={nextChevronCss} height={undefined} width={undefined} />
  </div>
);

const Enhet = (properties: {
  children: ReactNode;
  navIdentForImg: string;
  fornavnAndEtternavnForImg?: string;
}) => (
  <HorizontallyAlignedPanel
    className={panelCss.underEnhet}
    fornavnAndEtternavnForImg={properties.fornavnAndEtternavnForImg}
    iconSrc={building2Icon}
    navidentForImg={properties.navIdentForImg}
  >
    {properties.children}
  </HorizontallyAlignedPanel>
);

const Ressurs = (properties: { children: ReactNode }) => (
  <HorizontallyAlignedPanel
    className={css(commonPanelCss, panelCss.fullHeight)}
    iconSrc={peopleFilledIcon}
  >
    {properties.children}
  </HorizontallyAlignedPanel>
);

const OrgenhetLederLink = (properties: {
  navident?: string;
  name: string;
  orgenhetCtxUrl: string;
}) =>
  (properties.navident && (
    <Link
      className={lederStyling.lederTekst}
      to={`${properties.orgenhetCtxUrl}/${properties.navident}`}
    >
      {properties.name}
    </Link>
  )) || <span>Ingen leder</span>;

const BackFromRessurs = (properties: {
  valgtEnhetUrl: string;
  ressursIsLeder?: boolean;
}) => (
  <Link
    className={css`
      display: grid;
      grid-template-columns: min-content auto;
    `}
    to={properties.valgtEnhetUrl}
  >
    <Back className="navds-link-panel__chevron" color={colorPalette.font} />
    <BodyShort
      className={css({ fontWeight: "600", color: colorPalette.font })}
      size="medium"
    >
      {properties.ressursIsLeder
        ? "Se alle underenhetene for enheten "
        : "Se alle ressurser i enheten"}
    </BodyShort>
  </Link>
);

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

const OrganisasjonHierarkiPageInner = function (properties: {
  data: Hierarchy.OrgHierarchy;
  focusedRessurs?: Hierarchy.OrgHierarchyWithFocusedRessurs;
}) {
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
  const valgtEnhetUrl = valgtEnhet.url;

  return (
    <div>
      {overEnheter.map((overEnhet) => {
        return (
          <Fragment key={overEnhet.url}>
            <Link className={hierarkiCss.link} to={overEnhet.url}>
              <Enhet
                fornavnAndEtternavnForImg={overEnhet.ledere[0].name}
                navIdentForImg={overEnhet.ledere[0].navident}
              >
                <Heading size={"medium"}>{overEnhet.name}</Heading>
                <div>
                  Leder:{" "}
                  <span className={lederStyling.lederTekst}>
                    {overEnhet.ledere[0]?.name || "Leder ikke funnet"}
                  </span>
                </div>
              </Enhet>
            </Link>
            <OverenheterDivider />
          </Fragment>
        );
      })}

      {focusedRessurs && (
        <>
          <div className={panelCss.currentEnhet} ref={scrollReference}>
            <div className={panelCss.panelRim} />
            <div
              className={css`
                display: grid;
                grid-template-columns: 1fr min-content;
              `}
            >
              <div className={panelCss.focusedRessurs}>
                <Heading size={"large"}>{focusedRessurs.name}</Heading>
                <div
                  className={css`
                    padding-top: 0.4rem;
                  `}
                >
                  {focusedRessurs.sektor}
                  <strong> ({focusedRessurs.navident})</strong>
                </div>

                <div className={panelCss.labeledItemsPane}>
                  <span>
                    E-post: <strong>{focusedRessurs.email}</strong>
                  </span>
                  <span>
                    Tlf:{" "}
                    <strong>
                      {focusedRessurs.tel.length === 0 ? (
                        "Mangler tlf"
                      ) : (
                        <span>{focusedRessurs.tel}</span>
                      )}
                    </strong>
                  </span>

                  {focusedRessurs.tilknytninger.map((tilknytning) => {
                    const { orgenhet: oe, leder: l } = tilknytning;
                    return (
                      <Fragment key={l.navident + "_" + oe.url}>
                        <span>
                          Org.enhet:
                          <Link className={lederStyling.lederTekst} to={oe.url}>
                            <strong>{oe.name}</strong>
                          </Link>
                        </span>
                        <span>
                          Leder:
                          <Link className={lederStyling.lederTekst} to={l.url}>
                            <strong>{l.name}</strong>
                          </Link>
                        </span>
                      </Fragment>
                    );
                  })}
                </div>
              </div>
              <div
                className={css`
                  padding: var(--navds-spacing-10) var(--navds-spacing-10) 0;
                `}
              >
                <UserImage
                  navIdent={focusedRessurs.navident}
                  navn={focusedRessurs.name}
                  size={"150px"}
                />
              </div>
            </div>
          </div>
          <BackFromRessurs
            ressursIsLeder={ressurser.length === 0}
            valgtEnhetUrl={valgtEnhetUrl}
          />
        </>
      )}
      {!focusedRessurs && (
        <>
          {valgtEnhet && (
            <div className={panelCss.currentEnhet} ref={scrollReference}>
              <div className={panelCss.panelRim}></div>
              <div className={panelCss.currentEnhetPanelContent}>
                <div
                  className={css`
                    display: grid;
                    align-content: center;
                  `}
                >
                  <Heading size={"large"}>
                    <BuildingSvg />
                    {valgtEnhet.name}
                  </Heading>
                  <div className={lederStyling.lederDiv}>
                    <span>
                      Leder:{" "}
                      <OrgenhetLederLink
                        name={valgtEnhet.ledere[0]?.name}
                        navident={valgtEnhet.ledere[0]?.navident}
                        orgenhetCtxUrl={valgtEnhetUrl}
                      />
                    </span>
                  </div>
                </div>
                <UserImage
                  navIdent={valgtEnhet.ledere[0]?.navident}
                  navn={valgtEnhet.ledere[0]?.name}
                  size={"130px"}
                />
              </div>
            </div>
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
                to={valgtEnhetUrl + "/" + ressurs.navident}
              >
                <Ressurs>
                  <Heading
                    className={css`
                      font-size: 24px;
                      font-weight: 600;
                    `}
                    size={"medium"}
                  >
                    {ressurs.name}
                  </Heading>
                </Ressurs>
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
