import { css } from "@emotion/css";
import { Hamburger } from "@navikt/ds-icons";
import { AddPersonFilled } from "@navikt/ds-icons";
import { Button } from "@navikt/ds-react";
import { Dropdown } from "@navikt/ds-react-internal";
import { useNavigate } from "react-router";

import { hierarchy2 } from "./Images";

export function BurgerMenu() {
  const navigate = useNavigate();
  return (
    <Dropdown>
      <Button
        as={Dropdown.Toggle}
        icon={<Hamburger />}
        size="medium"
        variant="secondary"
      >
        <b>Meny</b>
      </Button>
      <Dropdown.Menu
        className={css`
          --navdsi-dropdown-item-color-text: black;
          padding: 0;

          button {
            padding: var(--navds-spacing-4);
          }
        `}
      >
        <Dropdown.Menu.List>
          <Dropdown.Menu.List.Item onClick={() => navigate("ressurs/create")}>
            <AddPersonFilled fontSize="24px" />
            <span>Opprett ressurs</span>
          </Dropdown.Menu.List.Item>
          <Dropdown.Menu.List.Item onClick={() => navigate("org")}>
            <img alt="hierarki" src={hierarchy2} />
            <span>Organisasjonshierarki</span>
          </Dropdown.Menu.List.Item>
        </Dropdown.Menu.List>
      </Dropdown.Menu>
    </Dropdown>
  );
}
