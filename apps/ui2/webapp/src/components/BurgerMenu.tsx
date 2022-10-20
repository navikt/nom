import { css } from "@emotion/css";
import { Hamburger } from "@navikt/ds-icons";
import { Button, Label } from "@navikt/ds-react";
import React from "react";
import { Link, useLocation } from "react-router-dom";

import { addPerson, hierarchy2 } from "./Images";

const BurgerButton = css({
  display: "flex",
  width: "6.3rem",
  height: "2.5rem",
  padding: "0.5rem",
  color: "var(--nom-interaction-primary)",
  borderRadius: "0.25rem",
  boxShadow: `inset 0 0 0 2px var(--nom-interaction-primary)`,
  ":hover": {
    backgroundColor: "var(--nom-interaction-primary-hover)",
    color: "var(--nom-interaction-primary)",
  },
  ":focus": {
    boxShadow: `inset 0 0 0 4px var(--nom-interaction-primary)`,
  },
});

const buttonIcon = css({
  marginRight: "0.4rem",
});

const linkStyle = css({
  display: "flex",
  textDecoration: "none",
  color: "var(--navds-global-color-gray-900)",
  padding: "1rem",
  borderBottom: "solid 1px var(--navds-global-color-gray-200)",
});

const dropdownContent = css({
  position: "absolute",
  backgroundColor: "var(--navds-global-color-white)",
  minWidth: "160px",
  boxShadow: "0px 8px 16px 0px rgba(0,0,0,0.2)",
  zIndex: "1",
  marginTop: "0.3em",
});

const dropDown = css({
  position: "relative",
  display: "inline-block",
});

const linkImage = css({
  marginRight: "1rem",
});

const ListItem = (properties: { label: string; url: string; icon: string }) => (
  <Link className={linkStyle} to={properties.url}>
    <img
      alt={properties.label + "- icon"}
      className={linkImage}
      src={properties.icon}
    />
    <Label
      className={css`
        cursor: pointer;
      `}
    >
      {properties.label}
    </Label>
  </Link>
);

const BurgerMenu = () => {
  const [showMenu, setShowMenu] = React.useState<boolean>(false);
  const menuContent = React.useRef<HTMLDivElement>(null);
  const mouseLeaveListenerReference = React.useRef(() => {
    setShowMenu(false);
  });
  const loc = useLocation();
  const path = loc.pathname;

  React.useEffect(() => {
    if (menuContent.current) {
      const element = menuContent.current;
      element.addEventListener(
        "mouseleave",
        mouseLeaveListenerReference.current
      );
      return () =>
        element.removeEventListener(
          "mouseleave",
          mouseLeaveListenerReference.current
        );
    }
  }, [showMenu, menuContent]);

  React.useEffect(
    function hideMenuOnPathChange() {
      setShowMenu(false);
    },
    [path]
  );

  return (
    <div className={dropDown}>
      <Button
        className={BurgerButton}
        onClick={() => (showMenu ? setShowMenu(false) : setShowMenu(true))}
        size="medium"
        variant="secondary"
      >
        <Hamburger className={buttonIcon} />
        <b>Meny</b>
      </Button>

      {showMenu && (
        <div className={dropdownContent} ref={menuContent}>
          <ListItem
            icon={hierarchy2}
            label="Organisasjonshierarki"
            url="/org"
          />
          <ListItem
            icon={addPerson}
            label="Opprett ressurs"
            url="/ressurs/create"
          />
        </div>
      )}
    </div>
  );
};

export default BurgerMenu;
