import { css } from "@emotion/react";
import { MenuHamburgerIcon, PersonPlusFillIcon } from "@navikt/aksel-icons";
import { Button, Dropdown } from "@navikt/ds-react";
import { useNavigate } from "react-router";

import { useHasAdminAccess } from "~/api/useGetSession";
import hierarchyFilled from "~/resources/hierarchyFilled.svg";

export function BurgerMenu() {
  const navigate = useNavigate();
  const hasAdminAccess = useHasAdminAccess();
  return (
    <Dropdown>
      <Button
        as={Dropdown.Toggle}
        icon={<MenuHamburgerIcon />}
        size="medium"
        variant="secondary"
      >
        <b>Meny</b>
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
          <Dropdown.Menu.List.Item onClick={() => navigate("ressurs/create")}>
            <PersonPlusFillIcon color="black" fontSize="24px" />
            <span>Opprett ressurs</span>
          </Dropdown.Menu.List.Item>
          <Dropdown.Menu.List.Item onClick={() => navigate("org")}>
            <img alt="hierarki" src={hierarchyFilled} />
            <span>Organisasjonshierarki</span>
          </Dropdown.Menu.List.Item>
          {hasAdminAccess && (
            <Dropdown.Menu.List.Item onClick={() => navigate("admin")}>
              <span>Admin</span>
            </Dropdown.Menu.List.Item>
          )}
        </Dropdown.Menu.List>
      </Dropdown.Menu>
    </Dropdown>
  );
}
