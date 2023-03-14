import { css } from "@emotion/react";
import { Back } from "@navikt/ds-icons";
import { Alert, BodyShort, Link } from "@navikt/ds-react";
import { useParams } from "react-router";
import { Link as ReactRouterLink, useLocation } from "react-router-dom";

import type { Ressurs } from "~/api/generated/graphql_generated";
import { useHasWriteAccess } from "~/api/useGetSession";

import * as Gq from "../api/generated/graphql_generated";
import ContentWrapper from "../components/ContentWrapper";
import { FocusedRessursView } from "./OrganisasjonsHierarkiPage/components/FocusedRessursView";

const linkStyles = css`
  margin-top: 20px;
  display: flex;
`;

const ResourceView = () => {
  const { id } = useParams();
  const { state } = useLocation();
  const query = Gq.useHentRessursMedNavidentQuery({
    variables: { navident: id },
  });

  const hasWrite = useHasWriteAccess();

  return (
    <ContentWrapper>
      <div
        css={css`
          display: flex;
          flex-direction: column;
          gap: var(--a-spacing-4);
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
          <Link as={ReactRouterLink} to={"/ressurs/create"}>
            <div css={linkStyles}>
              <Back className="navds-link-panel__chevron" />
              <BodyShort css={css({ fontWeight: "600" })} size="medium">
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
