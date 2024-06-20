import { ExternalLinkIcon, MenuGridIcon } from "@navikt/aksel-icons";
import { Dropdown, InternalHeader, Spacer } from "@navikt/ds-react";
import type { QueryClient } from "@tanstack/react-query";
import {
  createRootRouteWithContext,
  Link,
  Outlet,
} from "@tanstack/react-router";
import React from "react";

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
  queryClient: QueryClient;
}>()({
  component: () => {
    return (
      <>
        <React.Suspense fallback="">
          <TanStackRouterDevtools position="bottom-right" />
        </React.Suspense>
        <div>
          <InternalHeader>
            <InternalHeader.Title as={Link} to="/">
              NOM ADMIN
            </InternalHeader.Title>
            <Spacer />
            <Dropdown>
              <InternalHeader.Button as={Dropdown.Toggle}>
                <MenuGridIcon
                  style={{ fontSize: "1.5rem" }}
                  title="Systemer og oppslagsverk"
                />
              </InternalHeader.Button>

              <Dropdown.Menu>
                <Dropdown.Menu.GroupedList>
                  <Dropdown.Menu.GroupedList.Heading>
                    Systemer og oppslagsverk
                  </Dropdown.Menu.GroupedList.Heading>
                  <Dropdown.Menu.GroupedList.Item
                    as="a"
                    href="https://nom.ekstern.dev.no"
                    target="_blank"
                  >
                    NOM <ExternalLinkIcon aria-hidden />
                  </Dropdown.Menu.GroupedList.Item>
                </Dropdown.Menu.GroupedList>
              </Dropdown.Menu>
            </Dropdown>
            <InternalHeader.User name="TODO" />
          </InternalHeader>
          <main className="m-4">
            <Outlet />
          </main>
        </div>
      </>
    );
  },
});
