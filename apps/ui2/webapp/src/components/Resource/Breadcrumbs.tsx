import { css } from "@emotion/css";

const Breadcrumbs = (props: {
  url: string;
  previousSite: string;
  currentSite: string;
}) => {
  return (
    <div className={css({ marginBottom: "2em" })}>
      <span>
        {" "}
        <a href={props.url}>{props.previousSite}</a> {">"} {props.currentSite}
      </span>
    </div>
  );
};

export default Breadcrumbs;
