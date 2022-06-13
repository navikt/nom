import { css } from "@emotion/css";
import { BodyShort, Heading, Label, LinkPanel } from "@navikt/ds-react";
import { Link } from "react-router-dom";
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
  lederNavn: string;
  ident?: string;
  icon?: string | undefined;
  overenhet?: boolean;
  type: LinkPanelType;
};

const EnhetInfo = (props: EnhetInfoProps) => {
  const { title, lederNavn, ident, overenhet, type } = props;

  const getSize = () => {
    if (type === LinkPanelType.CURRENT) return "130px";
    else if (type === LinkPanelType.LARGE) return "90px";
    else return undefined;
  };

  return (
    <div className={enhetInfoStyles}>
      <div>
        <Heading size={type === LinkPanelType.CURRENT ? 'large' : 'medium'}>{title}</Heading>
        <LinkPanel.Description>
          <div className={css({ display: "flex", alignItem: "baseline" })}>
            <BodyShort>Leder: &nbsp;</BodyShort>

            {type === LinkPanelType.CURRENT ? (
              <Link to={"#"} className={css({ color: "#262626" })}><Label>{lederNavn}</Label></Link>
            ) : (
              <Label>{lederNavn}</Label>
            )}
          </div>
        </LinkPanel.Description>
      </div>
      {!overenhet && <UserImage navIdent={ident} size={getSize()} />}
    </div>
  );
};

export default EnhetInfo;
