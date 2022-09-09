import { css } from "@emotion/css";
import { BodyShort, Heading, Label } from "@navikt/ds-react";
import { Link } from "react-router-dom";
import {
  OrgEnhet as Organisasjonsenhet,
  Ressurs,
  RessursOrgTilknytning as RessursKobling,
  RessursLeder,
} from "../../api/generated/graphql_generated";
import { agressoIdDataToUrl } from "../../resources/utils/url_utils";
import CustomLinkPanel2, { LinkPanelType } from "../CustomLinkPanel2";
import UserImage from "../UserImage";

const ressursInfoStyles = css({
  width: "100%",
  display: "flex",
  alignItems: "center",
  flexDirection: "column",
});

const metadataStyles = css({
  display: "flex",
  marginBottom: "0.5rem",
});

const metadataColumnStyles = css({
  display: "flex",
  flexDirection: "column",
  marginTop: "1rem",
});

const LabelAndText = (props: {
  label: string;
  text: string;
  linkTo?: string;
  alignAtEnd?: boolean;
}) => (
  <div
    className={
      props.alignAtEnd
        ? css(metadataStyles, { justifyContent: "end" })
        : metadataStyles
    }
  >
    <BodyShort>{props.label} &nbsp;</BodyShort>
    {!props.linkTo && <Label>{props.text}</Label>}

    {props.linkTo && (
      <Link to={props.linkTo} className={css({ color: "#262626" })}>
        <Label>{props.text}</Label>
      </Link>
    )}
  </div>
);

const getLink = (orgenhet: Organisasjonsenhet, lederIdent?: string) => {
  if (!lederIdent)
    return (
      "/org/" +
      agressoIdDataToUrl(orgenhet.agressoId, orgenhet.orgNiv, orgenhet.navn)
    );
  else
    return (
      "/org/" +
      agressoIdDataToUrl(orgenhet.agressoId, orgenhet.orgNiv, orgenhet.navn) +
      `/${lederIdent}`
    );
};

const getLink2 = (
  agressoId: string,
  orgNiv: string,
  orgNavn: string,
  lederIdent: string
) => {
  return (
    "/org/" + agressoIdDataToUrl(agressoId, orgNiv, orgNavn) + `/${lederIdent}`
  );
};

type RessursInfoProps = {
  ressurs: Ressurs;
};

const RessursInfo = (props: RessursInfoProps) => {
  const { ressurs } = props;
  console.log(ressurs, "RESSURS");

  return (
    <div className={css({ marginTop: "1rem" })}>
      <CustomLinkPanel2 type={LinkPanelType.RESSURS}>
        <div className={ressursInfoStyles}>
          <div className={css({ marginTop: "2rem", marginBottom: "0.5rem" })}>
            <UserImage navIdent={ressurs.navIdent} size="130px" />
          </div>

          <Heading size="large">
            {ressurs.fornavn} {ressurs.etternavn}
          </Heading>
          <BodyShort
            className={css({ fontWeight: "600" })}
            size="medium"
          >{`(${ressurs.navIdent})`}</BodyShort>

          <div className={css({ display: "flex", marginBottom: "1.5rem" })}>
            <div
              className={css(metadataColumnStyles, { marginRight: "1.8rem" })}
            >
              <LabelAndText alignAtEnd label="Tlf: " text="Mangler tlf" />

              {ressurs.koblinger &&
                ressurs.koblinger.map((kobling: RessursKobling) => (
                  <LabelAndText
                    key={JSON.stringify(kobling)}
                    alignAtEnd
                    label="Org.enhet: "
                    text={
                      kobling.organisasjonsenhet.navn || "Mangler enhetsnavn"
                    }
                    linkTo={getLink(kobling.organisasjonsenhet)}
                  />
                ))}
            </div>

            <div className={metadataColumnStyles}>
              <LabelAndText
                label="E-post: "
                text={ressurs.epost || "Mangler tlf"}
              />
              {ressurs.ledere &&
                ressurs.ledere.map((leder: RessursLeder) => {
                  const l = leder.ressurs;
                  let lederNavn = "Uten navn";
                  if (l.fornavn && l.etternavn)
                    lederNavn = l.fornavn + " " + l.etternavn;
                  const oe = l.koblinger[0].organisasjonsenhet;
                  return (
                    <LabelAndText
                      key={JSON.stringify(leder)}
                      label="Leder: "
                      linkTo={getLink2(
                        oe.agressoId,
                        oe.orgNiv,
                        oe.navn,
                        l.navIdent
                      )}
                      text={lederNavn}
                    />
                  );
                })}
            </div>
          </div>
        </div>
      </CustomLinkPanel2>
    </div>
  );
};

export default RessursInfo;
