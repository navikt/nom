import { css } from "@emotion/css";
import { BodyShort, Heading, Label, LinkPanel } from "@navikt/ds-react";
import { Link } from "react-router-dom";
import { Ressurs } from "../../api/generated/graphql_generated";
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

type EnhetInfoProps = {
  title: string;
  type: LinkPanelType;
  leder: Ressurs
  lederNavn: string;
  ident?: string;
  icon?: string | undefined;
  overenhet?: boolean;
};

const EnhetInfo = (props: EnhetInfoProps) => {
  const { title, lederNavn, ident, overenhet, type, leder } = props
  

  const getSize = () => {
    if (type === LinkPanelType.CURRENT) return "130px";
    else if (type === LinkPanelType.LARGE) return "90px";
    else return undefined;
  }

  const getUrlForLeder = () => {
    if (!leder) return '#'
    if (leder.koblinger.length === 0) return '#'

    return `/org/${agressoIdDataToUrl(leder.koblinger[0].organisasjonsenhet.agressoId, leder.koblinger[0].organisasjonsenhet.orgNiv, leder.koblinger[0].organisasjonsenhet.navn)}/${leder.navIdent}`
  }

  return (
    <div className={enhetInfoStyles}>
      <div>
        <Heading size={type === LinkPanelType.CURRENT ? 'large' : 'medium'}>{title}</Heading>
        <LinkPanel.Description>
          <div className={css({ display: "flex", alignItem: "baseline" })}>
            <BodyShort>Leder: &nbsp;</BodyShort>

            {type === LinkPanelType.CURRENT ? (
              <Link 
                to={getUrlForLeder()}
                className={css({ color: "#262626" })}>
                    <Label>{lederNavn}</Label>
              </Link>
            ) : (
              <Label>{lederNavn}</Label>
            )}
          </div>
        </LinkPanel.Description>
      </div>
      {!overenhet && <UserImage navIdent={ident} size={getSize()} altText={`Fotografi av ${lederNavn}`}/>}
    </div>
  );
};

export default EnhetInfo;
