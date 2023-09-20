import { css } from "@emotion/react";
import {
  MenuHamburgerIcon,
  PersonFillIcon,
  PersonPlusFillIcon,
  TokenFillIcon,
} from "@navikt/aksel-icons";
import { Button, Dropdown } from "@navikt/ds-react";
import { useNavigate } from "@tanstack/react-router";

import {
  useGetSession,
  useHasAdminAccess,
  useHasWriteAccess,
} from "~/api/useGetSession";
import {
  adminRoute,
  createRessursRoute,
  indexRoute,
  ressursRoute,
} from "~/reactRouterRoutes";
import hierarchyFilled from "~/resources/hierarchyFilled.svg";

export function BurgerMenu() {
  const navigate = useNavigate();
  const hasAdminAccess = useHasAdminAccess();
  const hasWriteAccess = useHasWriteAccess();
  const session = useGetSession();

  return (
    <Dropdown>
      <Button
        as={Dropdown.Toggle}
        icon={<MenuHamburgerIcon />}
        size="medium"
        variant="secondary"
      >
        Meny
      </Button>
      <Dropdown.Menu
        css={css`
          padding: 0;
          color: var(--a-text-default);

          button {
            padding: var(--a-spacing-4);
          }
        `}
      >
        <Dropdown.Menu.List>
          <Dropdown.Menu.List.Item
            onClick={() => navigate({ to: indexRoute.to })}
          >
            <img alt="hierarki" src={hierarchyFilled} />
            <span>Organisasjonshierarki</span>
          </Dropdown.Menu.List.Item>
          <Dropdown.Menu.List.Item
            onClick={() =>
              navigate({
                to: ressursRoute.to,
                params: { id: session.data?.navident ?? "" },
              })
            }
          >
            <PersonFillIcon color="black" fontSize="24px" />
            <span>Min profil</span>
          </Dropdown.Menu.List.Item>
          {hasWriteAccess && (
            <Dropdown.Menu.List.Item
              onClick={() =>
                navigate({ to: createRessursRoute.to, params: { step: "1" } })
              }
            >
              <PersonPlusFillIcon color="black" fontSize="24px" />
              <span>Opprett medarbeider</span>
            </Dropdown.Menu.List.Item>
          )}
          {hasAdminAccess && (
            <Dropdown.Menu.List.Item
              onClick={() => navigate({ to: adminRoute.to })}
            >
              <TokenFillIcon color="black" fontSize="24px" />
              <span>Admin</span>
            </Dropdown.Menu.List.Item>
          )}
        </Dropdown.Menu.List>
      </Dropdown.Menu>
    </Dropdown>
  );
}
