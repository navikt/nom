import { css } from "@emotion/css";
import { Back } from "@navikt/ds-icons";
import { BodyShort, Heading } from "@navikt/ds-react";
import type { RefObject } from "react";
import { useEffect, useRef } from "react";
import { Link } from "react-router-dom";

import type {
  Organisering,
  OrgEnhetsLeder as LederOrgEnhet,
  Ressurs,
} from "../../api/generated/graphql_generated";
import type { CurrentEnhet } from "../../pages/OrganisasjonHierarkiPage";
import { agressoIdDataToUrl } from "../../resources/utils/url_utils";
import { sortItems } from "../../resources/utils/utils";
import CustomLinkPanel2, { LinkPanelType } from "../CustomLinkPanel2";
import EnhetInfo from "./EnhetInfo";
import RessursInfo from "./RessursInfo";

const scrollToReference = (reference: RefObject<HTMLDivElement>) => {
  if (reference.current) {
    window.scrollTo(0, reference.current.offsetTop - 100);
  }
};

const linkStyles = css({
  textDecoration: "none",
  color: "var(--navds-global-color-gray-900)",
});

const ressursLinkStyles = css({
  display: "flex",
  marginTop: "30px",
  fontWeight: "600",
  textDecoration: "none",
  color: "var(--navds-global-color-gray-900)",
});
const dividerStyles = css({
  background: "var(--navds-global-color-blue-800)",
  height: "20px",
  width: "2px",
  border: "1px solid var(--navds-global-color-blue-800)",
  margin: "0 auto",
});
const Divider = () => <div className={dividerStyles}></div>;

type OrgHierarkiListProperties = {
  currentEnhet: CurrentEnhet;
  underEnheter: Organisering[];
  overEnheter: Organisering[];
  ressurs?: Ressurs;
};

const OrgHierarkiList = (properties: OrgHierarkiListProperties) => {
  const referenceDiv = useRef<HTMLDivElement>(null);
  const { currentEnhet, underEnheter, overEnheter, ressurs } = properties;

  const sortedUnderEnheter =
    underEnheter.length > 0
      ? [...underEnheter].sort((a, b) =>
          sortItems(a.organisasjonsenhet.navn, b.organisasjonsenhet.navn)
        )
      : [];
  const sortedKoblinger = currentEnhet
    ? [...currentEnhet.koblinger]
        .sort((a, b) =>
          sortItems(a.ressurs.visningsNavn || "", b.ressurs.visningsNavn || "")
        )
        .filter(
          (v, index, a) =>
            a.findIndex((v2) => v2.ressurs.navIdent === v.ressurs.navIdent) ===
            index
        )
    : [];

  const getFornavnAndEtternavnForLeder = (leder: LederOrgEnhet) => {
    if (!leder) return "Leder ikke funnet";
    return (
      (leder.ressurs.fornavn || "") + " " + (leder.ressurs.etternavn || "")
    );
  };

  useEffect(() => {
    if (overEnheter.length > 0) scrollToReference(referenceDiv);
  }, []);

  console.log(currentEnhet);

  return (
    <div>
      <div>
        {/* Liste av alle overenheter */}
        {overEnheter.map((oe: Organisering) => (
          <div
            key={agressoIdDataToUrl(
              oe.organisasjonsenhet.agressoId,
              oe.organisasjonsenhet.orgNiv,
              oe.organisasjonsenhet.navn
            )}
          >
            <Link
              className={linkStyles}
              to={
                "/org/" +
                agressoIdDataToUrl(
                  oe.organisasjonsenhet.agressoId,
                  oe.organisasjonsenhet.orgNiv,
                  oe.organisasjonsenhet.navn
                )
              }
            >
              <CustomLinkPanel2 type={LinkPanelType.OVERENHET}>
                <EnhetInfo
                  leder={
                    oe.organisasjonsenhet.leder[0]?.ressurs as
                      | Ressurs
                      | undefined
                  }
                  lederNavn={
                    getFornavnAndEtternavnForLeder(
                      oe.organisasjonsenhet.leder[0]
                    ) || "Mangler Navn"
                  }
                  title={oe.organisasjonsenhet.navn}
                  type={LinkPanelType.OVERENHET}
                />
              </CustomLinkPanel2>
            </Link>
            <Divider />
          </div>
        ))}
      </div>

      {/* Den valgte enheten - når det ikke vises et ressurskort*/}
      {currentEnhet && !ressurs && (
        <div ref={referenceDiv}>
          <CustomLinkPanel2 type={LinkPanelType.CURRENT}>
            <EnhetInfo
              ident={currentEnhet?.leder[0]?.ressurs?.navIdent}
              leder={currentEnhet?.leder[0]?.ressurs as Ressurs}
              lederNavn={
                currentEnhet
                  ? getFornavnAndEtternavnForLeder(
                      currentEnhet?.leder[0] as LederOrgEnhet
                    )
                  : "Mangler navn"
              }
              title={currentEnhet?.navn || "Feil i henting av navn"}
              type={LinkPanelType.CURRENT}
            />
          </CustomLinkPanel2>
        </div>
      )}

      {/* Den valgte enheten - vises som en overenhet når det vises et ressurskort - gjøres om til en lenke siden den ikke finnses i overenhetlista */}
      {currentEnhet && ressurs && (
        <Link
          className={linkStyles}
          to={
            "/org/" +
            agressoIdDataToUrl(
              currentEnhet.agressoId,
              currentEnhet.orgNiv,
              currentEnhet.navn
            )
          }
        >
          <CustomLinkPanel2 type={LinkPanelType.OVERENHET}>
            <EnhetInfo
              leder={currentEnhet?.leder[0]?.ressurs as Ressurs}
              lederNavn={
                currentEnhet
                  ? getFornavnAndEtternavnForLeder(
                      currentEnhet?.leder[0] as LederOrgEnhet
                    )
                  : "Mangler navn"
              }
              title={currentEnhet?.navn || "Feil i henting av navn"}
              type={LinkPanelType.OVERENHET}
            />
          </CustomLinkPanel2>
        </Link>
      )}

      {/* Liste av underenheter */}
      {underEnheter.length > 0 && (
        <div
          className={css({
            display: "grid",
            gridTemplateColumns: underEnheter.length > 5 ? "1fr 1fr" : "1fr",
            columnGap: "10px",
          })}
        >
          {sortedUnderEnheter.map((ue: Organisering) => (
            <div
              key={agressoIdDataToUrl(
                ue.organisasjonsenhet.agressoId,
                ue.organisasjonsenhet.orgNiv,
                ue.organisasjonsenhet.navn
              )}
            >
              <Link
                className={linkStyles}
                to={
                  "/org/" +
                  agressoIdDataToUrl(
                    ue.organisasjonsenhet.agressoId,
                    ue.organisasjonsenhet.orgNiv,
                    ue.organisasjonsenhet.navn
                  )
                }
              >
                <CustomLinkPanel2 type={LinkPanelType.LARGE}>
                  <EnhetInfo
                    ident={ue.organisasjonsenhet.leder[0]?.ressurs?.navIdent}
                    leder={
                      ue.organisasjonsenhet.leder[0]?.ressurs as
                        | Ressurs
                        | undefined
                    }
                    lederNavn={getFornavnAndEtternavnForLeder(
                      ue.organisasjonsenhet.leder[0]
                    )}
                    title={ue.organisasjonsenhet.navn}
                    type={LinkPanelType.LARGE}
                  />
                </CustomLinkPanel2>
              </Link>
            </div>
          ))}
        </div>
      )}

      {/* Liste av ressurser */}
      {underEnheter.length === 0 && currentEnhet && !ressurs && (
        <div
          className={css({
            display: "grid",
            gridTemplateColumns:
              currentEnhet.koblinger.length > 5 ? "1fr 1fr" : "1fr",
            columnGap: "10px",
          })}
        >
          {sortedKoblinger.map((k) => {
            const agrIdUrl =
              agressoIdDataToUrl(
                currentEnhet.agressoId,
                currentEnhet.orgNiv,
                currentEnhet.navn
              ) + `/${k.ressurs.navIdent}`;
            return (
              <Link
                className={linkStyles}
                key={agrIdUrl}
                to={"/org/" + agrIdUrl}
              >
                <CustomLinkPanel2 type={LinkPanelType.KOBLING}>
                  <Heading className={css({ width: "100%" })} size="medium">
                    {k.ressurs?.fornavn && k.ressurs?.etternavn
                      ? k.ressurs.fornavn + " " + k.ressurs.etternavn
                      : "Mangler navn"}
                  </Heading>
                </CustomLinkPanel2>
              </Link>
            );
          })}
        </div>
      )}

      {/* Visning av en ressurs */}
      {underEnheter.length === 0 && currentEnhet && ressurs && (
        <>
          <RessursInfo ressurs={ressurs} />

          <Link
            to={
              "/org/" +
              agressoIdDataToUrl(
                currentEnhet.agressoId,
                currentEnhet.orgNiv,
                currentEnhet.navn
              )
            }
          >
            <div className={ressursLinkStyles}>
              <Back className="navds-link-panel__chevron" />
              <BodyShort className={css({ fontWeight: "600" })} size="medium">
                Se alle ressurser i enheten
              </BodyShort>
            </div>
          </Link>
        </>
      )}
    </div>
  );
};

export default OrgHierarkiList;
