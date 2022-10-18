import { css } from "@emotion/css";
import { BodyShort, Heading, Label } from "@navikt/ds-react";
import { FolkeregisterPerson } from "../../api/generated/graphql_generated";

const contentStyles = css({
  backgroundColor: "var(--nom-secondary-hover)",
  border: "1px solid var(--navds-global-color-gray-400)",
  borderRadius: "4px",
  padding: "1rem",
});

const rowStyles = css({
  display: "flex",
  justifyContent: "space-between",
  marginTop: "1rem",
});

const TextWithLabelVertical = (props: { label: string; text: string }) => (
  <div>
    <BodyShort>{props.label}</BodyShort>
    <Label>{props.text}</Label>
  </div>
);

const ResourceMetadata = ({ person }: { person: FolkeregisterPerson }) => {
  return (
    <div className={contentStyles}>
      <Heading size="small">Opplysninger fra Folkeregisteret</Heading>

      <div className={rowStyles}>
        <TextWithLabelVertical
          label="Fornavn"
          text={person.navn.fornavn || ""}
        />
        {person.navn.mellomnavn && (
          <>
            <TextWithLabelVertical
              label="Mellomnavn"
              text={person.navn.mellomnavn}
            />
          </>
        )}
        {/* <TextWithLabelVertical
          label='Mellomnavn'
          text={person.navn.mellomnavn || ''}
        /> */}
        <TextWithLabelVertical
          label="Etternavn"
          text={person.navn.etternavn || ""}
        />
        <TextWithLabelVertical label="Kjønn" text="" />
      </div>
    </div>
  );
};

export default ResourceMetadata;
