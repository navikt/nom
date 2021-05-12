import React, { ReactNode } from "react";
import { Link } from "react-router-dom";
import { LinkCard } from "../../components/contentcard/Contentcard";
import ContentcardArea from "../../components/contentcard/ContentcardArea";
import NomUiHeader from "../../components/header/NomUiHeader";
import Nompagewrap from "../../components/nompagewrap/Nompagewrap";

export default function Bestillinger(props: any): JSX.Element {
  return (
    <Nompagewrap pagetitle="Bestillinger">
      {/* <NomUiHeader /> */}
      <ContentcardArea>
        <LinkCard title="Ny bruker" uri={"/bestillinger/nybruker"} />
        <LinkCard title="Ny enhet" uri={"/bestillinger/nyenhet"} />
        <LinkCard title="Flytte enhet" uri={"/bestillinger/flytteenhet"} />
        <LinkCard title="Splitte enhet" uri={"/bestillinger/splitteenhet"} />
        <LinkCard
          title="Kategoriser enhet"
          uri={"/bestillinger/kategoriserenhet"}
        />
        <LinkCard
          title="Sammenslå enheter"
          uri={"/bestillinger/sammenslaaenheter"}
        />
      </ContentcardArea>
    </Nompagewrap>
  );
}
