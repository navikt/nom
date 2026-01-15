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

  const innloggetNavident = session.data?.navident;

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
          <SmartLink href="/">
            <Dropdown.Menu.List.Item>
              <img alt="hierarki" src={hierarchyFilled} />
              <span>Organisasjonshierarki</span>
            </Dropdown.Menu.List.Item>
          </SmartLink>

          <SmartLink href={`/ressurs/${innloggetNavident}`}>
            <Dropdown.Menu.List.Item>
              <PersonFillIcon color="black" fontSize="24px" />
              <span>Min profil</span>
            </Dropdown.Menu.List.Item>
          </SmartLink>

          {showOpprettLink && (
            <>
              <Dropdown.Menu.Divider />

              <SmartLink href="/ressurs/create/1">
                <Dropdown.Menu.List.Item>
                  <PersonPlusFillIcon color="black" fontSize="24px" />
                  <span>Opprett medarbeider</span>
                </Dropdown.Menu.List.Item>
              </SmartLink>
            </>
          )}

          {showOpprettSpecialLink && (
            <>
              {!showOpprettLink && <Dropdown.Menu.Divider />}

              <SmartLink href="/ressurs/createspecial">
                <Dropdown.Menu.List.Item>
                  <PersonPlusFillIcon color="black" fontSize="24px" />
                  <span>Opprett annen medarbeider</span>
                </Dropdown.Menu.List.Item>
              </SmartLink>
            </>
          )}

          {hasAdminAccess && (
            <>
              {!showOpprettLink && !showOpprettSpecialLink && (
                <Dropdown.Menu.Divider />
              )}

              <SmartLink href="/masseflytting-fysisk-brukersted">
                <Dropdown.Menu.List.Item>
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
              </SmartLink>
            </>
          )}

          {showOrgMastringLink && (
            <>
              <Dropdown.Menu.Divider />

              <SmartLink href="/organisasjonsmastring">
                <Dropdown.Menu.List.Item
                  onClick={() => navigate({ to: "/organisasjonsmastring" })}
                >
                  <img alt="hierarki" src={hierarchyFilled} />
                  <span>Mine organisasjonsenheter</span>
                </Dropdown.Menu.List.Item>
              </SmartLink>
            </>
          )}

          {showTilgangerLink && (
            <>
              <Dropdown.Menu.Divider />

              <SmartLink href="/tilganger">
                <Dropdown.Menu.List.Item>
                  <KeyVerticalFillIcon color="black" fontSize="24px" />
                  <span>Tilganger</span>
                </Dropdown.Menu.List.Item>
              </SmartLink>
            </>
          )}

          {hasAdminAccess && (
            <>
              {!showTilgangerLink && <Dropdown.Menu.Divider />}

              <SmartLink
                css={css`
                  width: 100%;

                  :hover {
                    width: 100%;
                  }

                  .navds-dropdown__list-item {
                    width: 100%;
                  }
                `}
                href={adminLenke}
              >
                <Dropdown.Menu.List.Item>
                  <TokenFillIcon color="black" fontSize="24px" />
                  <span>Admin</span>
                </Dropdown.Menu.List.Item>
              </SmartLink>
            </>
          )}
        </Dropdown.Menu.List>
      </Dropdown.Menu>
    </Dropdown>
  );
}
