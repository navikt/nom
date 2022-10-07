import { css } from "@emotion/css";
import { BodyShort, Heading, Label } from "@navikt/ds-react";
import { FolkeregisterPerson as Person } from "../../api/generated/graphql_generated";

const contentStyles = css({
  backgroundColor: "var(--nom-secondary-hover)",
  border: "1px solid var(--navds-global-color-gray-400)",
  borderRadius: "4px",
  marginTop: "2rem",
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

type ResourceMetadataProps = {
  resource: Person;
};

const ResourceMetadata = (props: ResourceMetadataProps) => {
  const { resource } = props;

  return (
    <div className={contentStyles}>
      <Heading size="small">Opplysninger fra folkeregisteret</Heading>

      <div className={rowStyles}>
        <TextWithLabelVertical
          label="Fornavn"
          text={resource.navn.fornavn || ""}
        />
        {resource.navn.mellomnavn && (
          <>
            <TextWithLabelVertical
              label="Mellomnavn"
              text={resource.navn.mellomnavn}
            />
          </>
        )}
        {/* <TextWithLabelVertical
          label='Mellomnavn'
          text={resource.navn.mellomnavn || ''}
        /> */}
        <TextWithLabelVertical
          label="Etternavn"
          text={resource.navn.etternavn || ""}
        />
        <TextWithLabelVertical label="Kjønn" text="" />
      </div>
    </div>
  );
};

export default ResourceMetadata;
