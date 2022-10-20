import { SearchForPerson } from "./components/SearchForPerson";
import { Outlet, Route, Routes, useMatch } from "react-router";
import CreateResourceForm from "./components/CreateResourceForm";
import { ResourceCreatedReceiptPage } from "./subPages/ResourceCreatedReceiptPage";
import ContentWrapper from "../../components/ContentWrapper";
import { css } from "@emotion/css";
import { SuccessStroke } from "@navikt/ds-icons";
import { PageBanner } from "../../components/PageBanner";

export function CreateResourcePageRoutes() {
  return (
    <Routes>
      <Route path="/" element={<Wrapper />}>
        <Route path="/" element={<SearchForPerson />}>
          <Route path=":personIdentInBase64" element={<CreateResourceForm />} />
        </Route>
        <Route
          path=":navIdent/receipt"
          element={<ResourceCreatedReceiptPage />}
        />
      </Route>
    </Routes>
  );
}

function Wrapper() {
  const isAtReceiptPage = !!useMatch(
    "ressurs/create/:navIdentInBase64/receipt"
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
                  <b
                    className={css`
                      color: var(--navds-global-color-white);
                    `}
                  >
                    1
                  </b>
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
                <b
                  className={css`
                    color: ${isAtReceiptPage
                      ? "var(--navds-global-color-white"
                      : "var(--navds-global-color-lightblue-800)"};
                  `}
                >
                  2
                </b>
              </FilledCircle>
            </div>
            <div
              className={css`
                display: flex;
                gap: 100px;
                color: var(--navds-global-color-lightblue-800);
              `}
            >
              <b>Personalopplysninger</b>
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
  children: React.ReactNode;
}) {
  return (
    <div
      className={css`
        display: flex;
        align-items: center;
        justify-content: center;
        border: 2px solid var(--navds-global-color-lightblue-800);
        border-radius: 50%;
        height: 42px;
        width: 42px;
        background: ${filled
          ? "var(--navds-global-color-lightblue-800)"
          : "var(--navds-global-color-white"};
      `}
    >
      {children}
    </div>
  );
}
