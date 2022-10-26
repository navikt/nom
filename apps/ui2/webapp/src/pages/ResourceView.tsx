import { css } from "@emotion/css";
import { Back } from "@navikt/ds-icons";
import { Alert, BodyShort } from "@navikt/ds-react";
import { useParams } from "react-router";
import { Link, useLocation } from "react-router-dom";

import * as Gq from "../api/generated/graphql_generated";
import ContentWrapper from "../components/ContentWrapper";
import RessursInfo from "../components/OrgHierarki/RessursInfo";

const linkStyles = css({
  marginTop: "20px",
  display: "flex",
  color: "black",
});

const ResourceView = () => {
  const { id } = useParams();
  const { state } = useLocation();
  const query = Gq.useHentRessursMedNavIdentQuery({
    variables: { navident: id },
  });
  return (
    <div>
      <ContentWrapper>
        <>
          {query.loading && <div>...laster graphql data fra nom-api</div>}
          {query.error && <p>{query.error.message}</p>}

          {query.data && (
            <>
              {state?.alreadyExists ? (
                <Alert variant="info">
                  Ressurs finnes allerede i NAV. Endringer kan gjøres i Remedy
                  og Unit4
                </Alert>
              ) : undefined}
              <RessursInfo ressurs={query.data.ressurs as Gq.Ressurs} />
            </>
          )}

          <Link to={"/ressurs/create"}>
            <div className={linkStyles}>
              <Back className="navds-link-panel__chevron" />
              <BodyShort className={css({ fontWeight: "600" })} size="medium">
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
