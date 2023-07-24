import { css } from "@emotion/react";
import { CheckmarkIcon } from "@navikt/aksel-icons";
import type { ReactNode } from "react";
import { Outlet, Route, Routes, useMatch } from "react-router";

import { useHasWriteAccess } from "~/api/useGetSession";
import { PageBanner } from "~/components/PageBanner";

import ContentWrapper from "../../components/ContentWrapper";
import { NotAuthorizedPage } from "../NotAuthorizedPage";
import CreateResourceForm from "./components/CreateResourceForm";
import { SearchForPerson } from "./components/SearchForPerson";
import { ResourceCreatedReceiptPage } from "./subPages/ResourceCreatedReceiptPage";

export function CreateResourcePageRoutes() {
  const hasWrite = useHasWriteAccess();

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
    "ressurs/create/:navidentInBase64/receipt",
  );
  return (
    <>
      <PageBanner text="Opprett ressurs" />
      <ContentWrapper>
        <>
          <div
            css={css`
              display: flex;
              flex-direction: column;
              align-items: center;
              gap: var(--a-spacing-3);
              margin-bottom: var(--a-spacing-12);
            `}
          >
            <div
              css={css`
                display: flex;
                align-items: center;
              `}
            >
              <FilledCircle filled={!isAtReceiptPage}>
                {isAtReceiptPage ? (
                  <CheckmarkIcon
                    css={css`
                      path {
                        stroke: var(--a-lightblue-800);
                        stroke-width: 2px;
                      }
                    `}
                  />
                ) : (
                  <b>1</b>
                )}
              </FilledCircle>
              <div
                css={css`
                  width: 205px;
                  height: 2px;
                  background: var(--a-lightblue-800);
                `}
              />
              <FilledCircle filled={isAtReceiptPage}>
                <b>2</b>
              </FilledCircle>
            </div>
            <div
              css={css`
                display: flex;
                gap: 100px;
                color: var(--a-lightblue-800);
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
      css={css(
        css`
          display: flex;
          align-items: center;
          justify-content: center;
          border: 2px solid var(--a-lightblue-800);
          border-radius: 50%;
          height: 42px;
          width: 42px;
        `,
        filled
          ? css`
              background: var(--a-lightblue-800);
              color: var(--a-white);
            `
          : css`
              background: var(--a-white);
              color: var(--a-lightblue-800);
            `,
      )}
    >
      {children}
    </div>
  );
}
