import { css } from "@emotion/react";
import {
  KeyVerticalFillIcon,
  MenuHamburgerIcon,
  PersonFillIcon,
  PersonPlusFillIcon,
  TokenFillIcon,
} from "@navikt/aksel-icons";
import { Button, Dropdown } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import { useNavigate } from "@tanstack/react-router";

import {
  getOrgEnheterLedetAvOptions,
  getTilgangerDelegertTilMegOptions,
} from "~/api/react-query-endpoints";
import {
  useGetSession,
  useHasAdminAccess,
  useHasWriteAccess,
} from "~/api/useGetSession";
import hierarchyFilled from "~/resources/hierarchyFilled.svg";

export function BurgerMenu() {
  const hasAdminAccess = useHasAdminAccess();
  const hasWriteAccess = useHasWriteAccess();

  const session = useGetSession();

  const navigate = useNavigate();

  const delegertTilMeg =
    useQuery(getTilgangerDelegertTilMegOptions()).data ?? [];

  const ledetAvMeg = useGetSession().data?.navident
    ? useQuery(
        getOrgEnheterLedetAvOptions(useGetSession().data?.navident as string),
      ).data ?? []
    : [];

  const lederEllerDelegert =
    ledetAvMeg?.length > 0 || delegertTilMeg?.length > 0;

  return (
    <Dropdown>
      <Button
        as={Dropdown.Toggle}
        data-cy="menu-button"
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
          <Dropdown.Menu.List.Item onClick={() => navigate({ to: "/" })}>
            <img alt="hierarki" src={hierarchyFilled} />
            <span>Organisasjonshierarki</span>
          </Dropdown.Menu.List.Item>
          <Dropdown.Menu.List.Item
            onClick={() =>
              navigate({
                to: "/ressurs/$id",
                params: { id: session.data?.navident ?? "" },
              })
            }
          >
            <PersonFillIcon color="black" fontSize="24px" />
            <span>Min profil</span>
          </Dropdown.Menu.List.Item>
          {(hasAdminAccess || (hasWriteAccess && lederEllerDelegert)) && (
            <Dropdown.Menu.List.Item
              data-cy="opprettRessurs-button"
              onClick={() =>
                navigate({ to: "/ressurs/create/$step", params: { step: "1" } })
              }
            >
              <PersonPlusFillIcon color="black" fontSize="24px" />
              <span>Opprett medarbeider</span>
            </Dropdown.Menu.List.Item>
          )}
          {(hasAdminAccess || (hasWriteAccess && lederEllerDelegert)) && (
            <Dropdown.Menu.List.Item
              data-cy="tilganger-button"
              onClick={() => navigate({ to: "/tilganger" })}
            >
              <KeyVerticalFillIcon color="black" fontSize="24px" />
              <span>Tilganger</span>
            </Dropdown.Menu.List.Item>
          )}
          {hasAdminAccess && (
            <Dropdown.Menu.List.Item
              onClick={() => navigate({ to: "/admin/orgenheter" })}
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
