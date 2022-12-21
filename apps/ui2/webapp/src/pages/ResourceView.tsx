import { css } from "@emotion/css";
import { Back } from "@navikt/ds-icons";
import { Alert, BodyShort } from "@navikt/ds-react";
import { useParams } from "react-router";
import { Link, useLocation } from "react-router-dom";

import type { Ressurs } from "../api/generated/graphql_generated";
import * as Gq from "../api/generated/graphql_generated";
import ContentWrapper from "../components/ContentWrapper";
import { UserRole, useSessionContext } from "../main";
import { FocusedRessursView } from "./OrganisasjonsHierarkiPage/components/FocusedRessursView";

const linkStyles = css({
  marginTop: "20px",
  display: "flex",
  color: "black",
});

const ResourceView = () => {
  const { id } = useParams();
  const { state } = useLocation();
  const ses = useSessionContext();
  const query = Gq.useHentRessursMedNavidentQuery({
    variables: { navident: id },
  });

  const hasWrite = ses.roles.includes(UserRole.ROLE_WRITE);

  return (
    <ContentWrapper>
      <div
        className={css`
          display: flex;
          flex-direction: column;
          gap: var(--navds-spacing-4);
        `}
      >
        {query.loading && <div>...laster graphql data fra nom-api</div>}
        {query.error && <p>{query.error.message}</p>}
        {query.data?.ressurs && (
          <>
            {state?.alreadyExists ? (
              <Alert variant="info">
                Ressurs finnes allerede i NAV. Endringer kan gjøres i Remedy og
                Unit4
              </Alert>
            ) : undefined}
            <FocusedRessursView
              focusedRessurs={query.data.ressurs as Ressurs}
            />
          </>
        )}
        {hasWrite && (
          <Link to={"/ressurs/create"}>
            <div className={linkStyles}>
              <Back className="navds-link-panel__chevron" />
              <BodyShort className={css({ fontWeight: "600" })} size="medium">
                Gå til oppretting av ressurser
              </BodyShort>
            </div>
          </Link>
        )}
      </div>
    </ContentWrapper>
  );
};

export default ResourceView;
