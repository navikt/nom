import React from "react";
import {LinkCard} from "../../components/contentcard/Contentcard";
import ContentcardArea from "../../components/contentcard/ContentcardArea";
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
        <LinkCard
          title="Se pågående bestillinger"
          uri={"/bestillinger/list"}
        />
      </ContentcardArea>
    </Nompagewrap>
  );
}
