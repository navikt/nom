import { css } from "@emotion/react";
import { QueryClient } from "@tanstack/react-query";
import {
  createRootRouteWithContext,
  Outlet,
  useRouterState,
} from "@tanstack/react-router";
import React from "react";

import { useHasAdminAccess } from "~/api/useGetSession";
import { ChristmasBanner } from "~/components/ChristmasBanner";
import { Footer, footerHeight } from "~/components/Footer";
import { headerHeight, NavHeader } from "~/components/NavHeader";
import { PilotBanner, pilotBannerHeight } from "~/components/PilotBanner";
import { PrefetchUsefulAdminQueries } from "~/components/PrefetchUsefulQueries";
import { TestBanner } from "~/components/TestBanner";

export const queryClient = new QueryClient();

const TanStackRouterDevtools =
  process.env.NODE_ENV === "production"
    ? () => null // Render nothing in production
    : React.lazy(() =>
        // Lazy load in development
        import("@tanstack/router-devtools").then((response) => ({
          default: response.TanStackRouterDevtools,
        })),
      );

export const Route = createRootRouteWithContext<{
  queryClient: typeof queryClient;
}>()({
  component: () => {
    const hasAdminAccess = useHasAdminAccess();

    const routerState = useRouterState();
    const currentRoute = routerState.location.pathname;

    const routesWithPageBanner = [
      "/ressurs/",
      "/ressurs/create",
      "/ressurs/createspecial",
      "/org/",
      "/opprett-organisasjonsenhet",
      "/organisasjonsmastring",
      "/kontaktpersoner/",
      "/masseflytting/",
      "/masseflytting-fysisk-brukersted",
      "/org/table/",
      "/ressurs-terminate/",
      "/tilganger",
      "/kontaktpersoner-tabell",
      "/korriger-organisasjonsenhet/",
      "/avslutt-organisasjonsenhet",
    ];

    const showChristmasBanner = !routesWithPageBanner.some((bannerRoute) =>
      currentRoute.includes(bannerRoute),
    );

    return (
      <>
        {hasAdminAccess && <PrefetchUsefulAdminQueries />}
        <React.Suspense fallback="">
          <TanStackRouterDevtools position="bottom-right" />
        </React.Suspense>
        <TestBanner />
        <NavHeader />
        <PilotBanner />
        {showChristmasBanner && <ChristmasBanner />}
        <div
          css={css`
            background-color: var(--nom-bg);
            min-height: calc(
              100vh - ${headerHeight} - ${pilotBannerHeight} - ${footerHeight}
            );
            width: 100%;
            display: flex;
          `}
        >
          <div css={css({ width: "100%" })}>
            <Outlet />
          </div>
        </div>
        <Footer />
      </>
    );
  },
});
