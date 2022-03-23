import { NavLink, useNavigate } from "react-router-dom";
import { css } from "@emotion/css";
import { Dropdown, Header } from "@navikt/ds-react-internal";
import { Home, System } from "@navikt/ds-icons";
import { ReactNode } from "react";

const flexStyle = css({
  display: "grid",
  gridAutoFlow: "column",
  columnGap: "1rem",
  alignItems: "center",
  color: "white",
  fill: "white",
});

const whiteStyle = css({
  color: "white",
  fill: "white",
});

const contentStyle = css({
  margin: "2rem 2rem 0 2rem",
});

function DropdownLinkItem(props: { to: string; children: string }) {
  return (
    <Dropdown.Menu.GroupedList.Item>
      <NavLink to={props.to}>{props.children}</NavLink>
    </Dropdown.Menu.GroupedList.Item>
  );
}

function DropdownLinkGroup(props: { label: string; children: ReactNode }) {
  return (
    <Dropdown.Menu.GroupedList>
      <Dropdown.Menu.GroupedList.Heading>
        {props.label}
      </Dropdown.Menu.GroupedList.Heading>
      {props.children}
    </Dropdown.Menu.GroupedList>
  );
}

function StandardDropdown() {
  return (
    <Dropdown>
      <Header.Button as={Dropdown.Toggle}>
        <System />
      </Header.Button>
      <Dropdown.Menu>
        <DropdownLinkGroup label="Hurtiglenker">
          <DropdownLinkItem to="/ressurs">Ressurs</DropdownLinkItem>
          <DropdownLinkItem to="/organisasjon">Organisasjon</DropdownLinkItem>
          <DropdownLinkItem to="/formtestpage">Form test</DropdownLinkItem>
          <DropdownLinkItem to="/nomapigraphqltest">Graphql api test</DropdownLinkItem>
        </DropdownLinkGroup>
      </Dropdown.Menu>
    </Dropdown>
  );
}

export function Pagewrap(props: any) {
  return (
    <>
      {/* <Header system-meny className="w-full"> */}
      <Header system-meny className="w-full">
        <Header.Title as="div" className={flexStyle}>
          <NavLink to="/" className={whiteStyle}>
            <Home />
          </NavLink>
        </Header.Title>
        <StandardDropdown />
        <Header.Title as="h1">NORMA</Header.Title>
        <Header.User
                name="NAV BRUKER"
                description={"A123456"}
                style={{marginLeft: "auto"}}
            />
      </Header>
      <div className={contentStyle}>{props.children}</div>
    </>
  );
}
