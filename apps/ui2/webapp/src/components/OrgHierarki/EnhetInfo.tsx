import { css } from "@emotion/css";
import { BodyShort, Heading, Label, LinkPanel } from "@navikt/ds-react";
import { Link } from "react-router-dom";

import type { Ressurs } from "../../api/generated/graphql_generated";
import { agressoIdDataToUrl } from "../../resources/utils/url_utils";
import { LinkPanelType } from "../CustomLinkPanel2";
import UserImage from "../UserImage";

const enhetInfoStyles = css({
  height: "100%",
  width: "100%",
  display: "flex",
  alignItems: "center",
  justifyContent: "space-between",
});

type EnhetInfoProperties = {
  title: string;
  type: LinkPanelType;
  leder?: Ressurs;
  lederNavn: string;
  ident?: string;
  icon?: string | undefined;
};

const EnhetInfo = (properties: EnhetInfoProperties) => {
  const { title, lederNavn, ident, type, leder } = properties;

  const getSize = () => {
    return type === LinkPanelType.CURRENT ? "130px" : undefined;
  };

  const getUrlForLeder = () => {
    if (!leder) return "#";
    if (leder.koblinger.length === 0) return "#";

    return `/org/${agressoIdDataToUrl(
      leder.koblinger[0].organisasjonsenhet.agressoId,
      leder.koblinger[0].organisasjonsenhet.orgNiv,
      leder.koblinger[0].organisasjonsenhet.navn
    )}/${leder.navIdent}`;
  };

  return (
    <div className={enhetInfoStyles}>
      <div>
        <Heading size={type === LinkPanelType.CURRENT ? "large" : "medium"}>
          {title}
        </Heading>
        <LinkPanel.Description>
          <div className={css({ display: "flex", alignItem: "baseline" })}>
            <BodyShort>Leder: &nbsp;</BodyShort>

            {type === LinkPanelType.CURRENT ? (
              leder ? (
                <Link
                  className={css({
                    color: "var(--navds-global-color-gray-900)",
                  })}
                  to={getUrlForLeder()}
                >
                  <Label>{lederNavn}</Label>
                </Link>
              ) : (
                <Label>{lederNavn}</Label>
              )
            ) : (
              <Label>{lederNavn}</Label>
            )}
          </div>
        </LinkPanel.Description>
      </div>
      {type !== LinkPanelType.OVERENHET ? (
        <UserImage
          altText={`Fotografi av ${lederNavn}`}
          navIdent={ident}
          size={getSize()}
        />
      ) : (
        <UserImage
          altText={`Fotografi av ${leder?.visningsNavn}`}
          navIdent={leder?.navIdent}
          size={getSize()}
        />
      )}
    </div>
  );
};

export default EnhetInfo;
