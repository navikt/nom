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
  useHasAdminReadOnlyAccess,
  useHasRobotAccess,
  useHasUtenlandskKonsulentAccess,
  useHasVikafossenAccess,
  useHasWriteAccess,
} from "~/api/useGetSession";
import { SmartLink } from "~/components/navigation/SmartLink";
import flyttFysiskBrukersted from "~/resources/flyttFysiskBrukersted.svg";
import hierarchyFilled from "~/resources/hierarchyFilled.svg";

export function BurgerMenu() {
  const hasAdminAccess = useHasAdminAccess();
  const hasWriteAccess = useHasWriteAccess();
  const hasAdminReadOnlyAccess = useHasAdminReadOnlyAccess();

  const hasRobotAccess = useHasRobotAccess();
  const hasVikafossenAccess = useHasVikafossenAccess();
  const hasUtenlandskKonsulentAccess = useHasUtenlandskKonsulentAccess();

  const session = useGetSession();

  const navigate = useNavigate();

  const adminLenke: string = globalThis.location.href.includes("nom.nav.no")
    ? "https://nom-admin.ansatt.nav.no/sinks/ressurs"
    : "https://nom-admin.intern.dev.nav.no/sinks/ressurs";

  const ledetAvMeg = useGetSession().data?.navident
    ? (useQuery(
        getOrgEnheterLedetAvOptions(useGetSession().data?.navident as string),
      ).data ?? [])
    : [];

  const delegertTilMeg =
    useQuery(getTilgangerDelegertTilMegOptions()).data ?? [];

  const lederEllerDelegert =
    ledetAvMeg?.length > 0 || delegertTilMeg?.length > 0;

  const harRedigeringstilgang =
    hasAdminAccess || (hasWriteAccess && lederEllerDelegert);

  const showOpprettLink = harRedigeringstilgang;

  const showOpprettSpecialLink =
    hasRobotAccess ||
    hasVikafossenAccess ||
    hasUtenlandskKonsulentAccess ||
    hasAdminAccess;

  const showOrgMastringLink = harRedigeringstilgang || hasAdminReadOnlyAccess;

  const showTilgangerLink = harRedigeringstilgang;

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
          width: fit-content;

          button {
            padding: var(--a-spacing-4);
          }

          a {
            text-decoration: none;
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

          {showOpprettLink && (
            <>
              <Dropdown.Menu.Divider />

              <SmartLink href="/ressurs/create/1">
                <Dropdown.Menu.List.Item data-cy="opprettRessurs-button">
                  <PersonPlusFillIcon color="black" fontSize="24px" />
                  <span>Opprett medarbeider</span>
                </Dropdown.Menu.List.Item>
              </SmartLink>
            </>
          )}

          {showOpprettSpecialLink && (
            <>
              {!showOpprettLink && <Dropdown.Menu.Divider />}

              <Dropdown.Menu.List.Item
                onClick={() =>
                  navigate({
                    to: "/ressurs/createspecial",
                  })
                }
              >
                <PersonPlusFillIcon color="black" fontSize="24px" />
                <span>Opprett annen medarbeider</span>
              </Dropdown.Menu.List.Item>
            </>
          )}

          {hasAdminAccess && (
            <>
              {!showOpprettLink && !showOpprettSpecialLink && (
                <Dropdown.Menu.Divider />
              )}

              <Dropdown.Menu.List.Item
                onClick={() =>
                  navigate({ to: "/masseflytting-fysisk-brukersted" })
                }
              >
                <img
                  alt=""
                  css={css`
                    height: 24px;
                    width: 24px;
                    align-self: center;
                  `}
                  src={flyttFysiskBrukersted}
                />
                <span>Masseflytting fysisk brukersted</span>
              </Dropdown.Menu.List.Item>
            </>
          )}

          {showOrgMastringLink && (
            <>
              <Dropdown.Menu.Divider />

              <Dropdown.Menu.List.Item
                onClick={() => navigate({ to: "/organisasjonsmastring" })}
              >
                <img alt="hierarki" src={hierarchyFilled} />
                <span>Mine organisasjonsenheter</span>
              </Dropdown.Menu.List.Item>
            </>
          )}

          {showTilgangerLink && (
            <>
              <Dropdown.Menu.Divider />

              <Dropdown.Menu.List.Item
                data-cy="tilganger-button"
                onClick={() => navigate({ to: "/tilganger" })}
              >
                <KeyVerticalFillIcon color="black" fontSize="24px" />
                <span>Tilganger</span>
              </Dropdown.Menu.List.Item>
            </>
          )}

          {hasAdminAccess && (
            <>
              {!showTilgangerLink && <Dropdown.Menu.Divider />}

              <Dropdown.Menu.List.Item
                onClick={() => globalThis.open(adminLenke, "_blank")}
              >
                <TokenFillIcon color="black" fontSize="24px" />
                <span>Admin</span>
              </Dropdown.Menu.List.Item>
            </>
          )}
        </Dropdown.Menu.List>
      </Dropdown.Menu>
    </Dropdown>
  );
}
