import * as Gq from "../api/generated/graphql_generated";
import { useLocation, useParams } from "react-router";
import { css } from "@emotion/css";
import { BodyShort } from "@navikt/ds-react";
import ContentWrapper from "../components/ContentWrapper";
import RessursInfo from "../components/OrgHierarki/RessursInfo";
import { Back } from "@navikt/ds-icons";
import { Link } from "react-router-dom";

const linkStyles = css({
  marginTop: "20px",
  display: "flex",
  color: "black",
});

const ResourceView = () => {
  const { id } = useParams<any>();
  useLocation() as any;
  const query = Gq.useHentRessursMedNavIdentQuery({
    variables: { navIdent: id },
  });
  return (
    <div>
      <ContentWrapper>
        <>
          {query.loading && <div>...laster graphql data fra nom-api</div>}
          {query.error && <p>{query.error.message}</p>}

          {query.data && (
            <>
              <RessursInfo ressurs={query.data.ressurs as Gq.Ressurs} />
            </>
          )}

          <Link to={"/ressurs/create"}>
            <div className={linkStyles}>
              <Back className="navds-link-panel__chevron" />
              <BodyShort size="medium" className={css({ fontWeight: "600" })}>
                Gå til oppretting av ressurser
              </BodyShort>
            </div>
          </Link>
        </>
      </ContentWrapper>
    </div>
  );
};

export default ResourceView;
