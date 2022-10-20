import { css } from "@emotion/css";

const Breadcrumbs = (properties: {
  url: string;
  previousSite: string;
  currentSite: string;
}) => {
  return (
    <div className={css({ marginBottom: "2em" })}>
      <span>
        {" "}
        <a href={properties.url}>{properties.previousSite}</a> {">"}{" "}
        {properties.currentSite}
      </span>
    </div>
  );
};

export default Breadcrumbs;
