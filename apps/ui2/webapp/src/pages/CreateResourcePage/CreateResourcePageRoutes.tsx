import { css } from "@emotion/css";
import { SuccessStroke } from "@navikt/ds-icons";
import type { ReactNode } from "react";
import { Outlet, Route, Routes, useMatch } from "react-router";

import ContentWrapper from "../../components/ContentWrapper";
import { PageBanner } from "../../components/PageBanner";
import { UserRole, useSessionContext } from "../../main";
import { NotAuthorizedPage } from "../NotAuthorizedPage";
import CreateResourceForm from "./components/CreateResourceForm";
import { SearchForPerson } from "./components/SearchForPerson";
import { ResourceCreatedReceiptPage } from "./subPages/ResourceCreatedReceiptPage";

export function CreateResourcePageRoutes() {
  const ses = useSessionContext();
  const hasWrite = ses.roles.includes(UserRole.ROLE_WRITE);

  return hasWrite ? (
    <Routes>
      <Route element={<Wrapper />} path="/">
        <Route element={<SearchForPerson />} path="/">
          <Route element={<CreateResourceForm />} path=":personidentInBase64" />
        </Route>
        <Route
          element={<ResourceCreatedReceiptPage />}
          path=":navident/receipt"
        />
      </Route>
    </Routes>
  ) : (
    <Routes>
      <Route element={<NotAuthorizedPage />} path="*" />
    </Routes>
  );
}

function Wrapper() {
  const isAtReceiptPage = !!useMatch(
    "ressurs/create/:navidentInBase64/receipt"
  );
  return (
    <>
      <PageBanner text="Opprett ressurs" />
      <ContentWrapper>
        <>
          <div
            className={css`
              display: flex;
              flex-direction: column;
              align-items: center;
              gap: var(--navds-spacing-3);
              margin-bottom: var(--navds-spacing-12);
            `}
          >
            <div
              className={css`
                display: flex;
                align-items: center;
              `}
            >
              <FilledCircle filled={!isAtReceiptPage}>
                {isAtReceiptPage ? (
                  <SuccessStroke
                    className={css`
                      path {
                        stroke: var(--navds-global-color-lightblue-800);
                        stroke-width: 2px;
                      }
                    `}
                  />
                ) : (
                  <b>1</b>
                )}
              </FilledCircle>
              <div
                className={css`
                  width: 205px;
                  height: 2px;
                  background: var(--navds-global-color-lightblue-800);
                `}
              />
              <FilledCircle filled={isAtReceiptPage}>
                <b>2</b>
              </FilledCircle>
            </div>
            <div
              className={css`
                display: flex;
                gap: 100px;
                color: var(--navds-global-color-lightblue-800);
              `}
            >
              <b>Personopplysninger</b>
              <b>Lønnsopplysninger i Unit4</b>
            </div>
          </div>
          <Outlet />
        </>
      </ContentWrapper>
    </>
  );
}

function FilledCircle({
  children,
  filled,
}: {
  filled: boolean;
  children: ReactNode;
}) {
  return (
    <div
      className={css(
        css`
          display: flex;
          align-items: center;
          justify-content: center;
          border: 2px solid var(--navds-global-color-lightblue-800);
          border-radius: 50%;
          height: 42px;
          width: 42px;
        `,
        filled
          ? css`
              background: var(--navds-global-color-lightblue-800);
              color: var(--navds-global-color-white);
            `
          : css`
              background: var(--navds-global-color-white);
              color: var(--navds-global-color-lightblue-800);
            `
      )}
    >
      {children}
    </div>
  );
}
