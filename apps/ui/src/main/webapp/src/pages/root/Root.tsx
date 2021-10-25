import React, { Fragment } from "react";
import {
  LinkCard,
} from "../../components/contentcard/Contentcard";
import ContentcardArea from "../../components/contentcard/ContentcardArea";
import Nompagewrap from "../../components/nompagewrap/Nompagewrap";
import Searchbox from "../../components/searchbox/Searchbox";

const links = [
  { label: "Organisasjonsenheter", uri: "organisasjonsenheter" },
  { label: "Medarbeidere", uri: "medarbeidere" },
  { label: "Bestillinger", uri: "bestillinger" },
  { label: "Bestillings-godkjenning", uri: "bestillingsgodkjenning" },
];

export default function Root(props: any): JSX.Element {
  return (
    <Nompagewrap pagetitle="Hovedside">
      <div className="Root">
        <Searchbox />
        <ContentcardArea>
          {links.map((it) => (
            <Fragment key={it.uri}>
              <LinkCard title={it.label} uri={it.uri}></LinkCard>
            </Fragment>
          ))}
        </ContentcardArea>
      </div>
    </Nompagewrap>
  );
}
