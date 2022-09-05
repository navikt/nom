import { css } from "@emotion/css";
import { PersonResult } from "../../api/generated/graphql_generated";
import Card from "../Card";
import TextWithLabel from "../TextWithLabel";

const rowStyle = css({
  marginTop: "20px",
  width: "65%",
  display: "flex",
  justifyContent: "space-between",
});

type PersonDetailCardProps = {
  cardTitle: string;
  person: PersonResult;
};

const PersonDetailCard = (props: PersonDetailCardProps) => {
  const { cardTitle, person } = props;
  return (
    <Card title={cardTitle}>
      {person.person && (
        <div className={rowStyle}>
          <TextWithLabel
            label="Etternavn"
            text={person.person.navn.etternavn}
          />
          <TextWithLabel
            label="Mellomnavn"
            text={person.person.navn.mellomnavn || ""}
          />
          <TextWithLabel label="Fornavn" text={person.person.navn.fornavn} />
          <TextWithLabel label="Kjønn" text="Mangler kjønn.." />
        </div>
      )}
    </Card>
  );
};

export default PersonDetailCard;
