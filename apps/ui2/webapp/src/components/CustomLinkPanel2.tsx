import { css } from "@emotion/css";
import { Next } from "@navikt/ds-icons";

import { building2Icon, peopleFilledIcon } from "./Images";

export enum LinkPanelType {
  CURRENT = "current",
  OVERENHET = "overenhet",
  KOBLING = "kobling",
  RESSURS = "ressurs",
  LARGE = "large",
  SMALL = "small",
}

const commonCss = css({
  minHeight: "100px",
  width: "100%",
  display: "grid",
  gridAutoFlow: "column",
  gridTemplateColumns: "min-content 1fr min-content",
  alignItems: "center",
  border: "1px solid var(--navds-global-color-gray-400)",
  borderRadius: "5px",
  marginBottom: "15px",
});

const innerCommonCss = css({
  padding: "30px 15px",
});

const linkPanelCurrentStyles = css(commonCss, {
  backgroundColor: "var(--nom-secondary-hover)",
  minHeight: "160px",
  gridAutoFlow: "row",
  gridTemplateColumns: "1fr",
});
const linkPanelLargeStyles = css(commonCss, {
  backgroundColor: "var(--navds-global-color-white)",
  minHeight: "130px",
  width: "100%",
});
const linkPanelOverenhetStyles = css(commonCss, {
  backgroundColor: "var(--navds-global-color-white)",
  minHeight: "90px",
  width: "100%",
  marginBottom: "0px",
});
const linkPanelKoblingStyles = css(commonCss, {
  backgroundColor: "var(--navds-global-color-white)",
  minHeight: "70px",
  width: "100%",
});
const linkPanelRessursStyles = css({
  backgroundColor: "var(--nom-secondary-hover)",
  width: "100%",
  borderTop: "5px solid var(--navds-global-color-lightblue-800)",
  borderRadius: "5px",
  borderLeft: "1px solid var(--navds-global-color-gray-400)",
  borderRight: "1px solid var(--navds-global-color-gray-400)",
  borderBottom: "1px solid var(--navds-global-color-gray-400)",
});

const iconStyle = css({
  height: "100%",
  display: "grid",
  justifyItems: "center",
  alignItems: "center",
  backgroundColor: "var(--navds-global-color-lightblue-800)",
  color: "white",
  img: {
    padding: "0.2rem",
  },
});

type CustomLinkPanel2Properties = {
  children: React.ReactNode;
  type: LinkPanelType;
};

function type2Icon(t: LinkPanelType) {
  switch (t) {
    case LinkPanelType.CURRENT: {
      return building2Icon;
    }
    case LinkPanelType.OVERENHET: {
      return building2Icon;
    }
    case LinkPanelType.KOBLING: {
      return peopleFilledIcon;
    }
    case LinkPanelType.RESSURS: {
      return peopleFilledIcon;
    }
    case LinkPanelType.LARGE: {
      return building2Icon;
    }
    case LinkPanelType.SMALL: {
      return building2Icon;
    }
  }
}

const CustomLinkPanel2 = (properties: CustomLinkPanel2Properties) => {
  const { children, type } = properties;

  const setLinkPanelStyles = () => {
    switch (type) {
      case LinkPanelType.CURRENT: {
        return linkPanelCurrentStyles;
      }
      case LinkPanelType.OVERENHET: {
        return linkPanelOverenhetStyles;
      }
      case LinkPanelType.LARGE: {
        return linkPanelLargeStyles;
      }
      case LinkPanelType.KOBLING: {
        return linkPanelKoblingStyles;
      }
      case LinkPanelType.RESSURS: {
        return linkPanelRessursStyles;
      }
      // No default
    }
  };

  const icon = type2Icon(type);

  return (
    <>
      <div className={setLinkPanelStyles()}>
        <div className={iconStyle}>{icon && <img src={icon} />}</div>
        <div className={innerCommonCss}>{children}</div>
        {type !== LinkPanelType.CURRENT && type !== LinkPanelType.RESSURS && (
          <Next className="navds-link-panel__chevron" />
        )}
      </div>
    </>
  );
};

export default CustomLinkPanel2;
