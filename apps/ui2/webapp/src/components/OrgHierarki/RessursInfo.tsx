import { css } from "@emotion/css";
import { BodyShort, Heading, Label } from "@navikt/ds-react";
import { Link } from "react-router-dom";
import { Ressurs, RessursKobling } from "../../api/generated/graphql_generated";
import CustomLinkPanel2, { LinkPanelType } from "../CustomLinkPanel2";
import TextWithLabel2 from "../TextWithLabel2";
import UserImage from "../UserImage";

const ressursInfoStyles = css({
  width: "100%",
  display: "flex",
  alignItems: "center",
  flexDirection: "column",
});

const metadataStyles = css({
  display: "flex",
  width: "100%",
  marginBottom: "0.5rem",
});

const metadataColumnStyles = css({
  display: "flex",
  flexDirection: "column",
  marginTop: "1rem",
  width: "50%",
});

const LinkWithTextAndLabel = (props: { label: string; text: string, linkTo?: string, alignAtEnd?: boolean }) => (
  <div className={props.alignAtEnd ? css(metadataStyles, {justifyContent: 'end'}) : metadataStyles}>
    <BodyShort>{props.label} &nbsp;</BodyShort>
    {!props.linkTo && <Label>{props.text}</Label>}
    
    {props.linkTo && (
        <Link to={props.linkTo} className={css({color: "#262626"})}> 
            <Label>{props.text}</Label>
        </Link>
    )}
  </div>
);

type RessursInfoProps = {
  ressurs: Ressurs;
};

const RessursInfo = (props: RessursInfoProps) => {
  const { ressurs } = props;
  console.log(ressurs, "RESSURS");

  return (
    <CustomLinkPanel2 type={LinkPanelType.RESSURS}>
      <div className={ressursInfoStyles}>
        <div className={css({ marginTop: "2rem", marginBottom: "0.5rem" })}>
          <UserImage navIdent={ressurs.navIdent} size="130px" />
        </div>

        <Heading size="large">{ressurs.fornavn} {ressurs.etternavn}</Heading>
        <BodyShort size="medium">{`(${ressurs.navIdent})`}</BodyShort>
      </div>

      <div className={css({ display: "flex" })}>
        <div className={css(metadataColumnStyles, { marginRight: "1.5rem" })}>
          <LinkWithTextAndLabel alignAtEnd label="Tlf: " text="Mangler tlf" />

          {ressurs.koblinger &&
            ressurs.koblinger.map((kobling: RessursKobling) => (
                <LinkWithTextAndLabel 
                    alignAtEnd
                    label="Org-enhet: " 
                    text={kobling.organisasjonsenhet.navn || "Mangler enhetsnavn"}
                    linkTo="#"
                />
          ))}
        </div>

        <div className={metadataColumnStyles}>
          <TextWithLabel2 label="E-post: " text={ressurs.epost || "Mangler tlf"} />

          {ressurs.koblinger &&
            ressurs.koblinger.map((kobling: RessursKobling) => (
              <>
                {kobling.organisasjonsenhet.leder && (
                  <LinkWithTextAndLabel
                    label="Leder: "
                    linkTo="#"
                    text={
                      kobling.organisasjonsenhet.leder.length > 0
                        ? (kobling.organisasjonsenhet.leder[0].ressurs.fornavn as string + ' ' + kobling.organisasjonsenhet.leder[0].ressurs.etternavn as string)
                        : "Mangler navn"
                    }
                  />
                )}
              </>
            ))}
        </div>
      </div>
    </CustomLinkPanel2>
  );
};

export default RessursInfo;
