import { css } from "@emotion/css";
import { Heading } from "@navikt/ds-react";
import type { RefObject } from "react";
import React, { Fragment } from "react";
import { Link } from "react-router-dom";

import type { Hierarchy } from "../../../api/transformations/hierarchy";
import UserImage from "../../../components/UserImage";
import { lederStyling, panelCss } from "../styles";

export const FocusedRessursView = ({
  focusedRessurs,
  scrollReference,
}: {
  focusedRessurs: Hierarchy.FocusedRessurs;
  scrollReference: RefObject<HTMLDivElement>;
}) => (
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
);
