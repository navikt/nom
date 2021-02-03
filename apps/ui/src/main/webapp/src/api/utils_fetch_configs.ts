const GET_SIMPLE: RequestInit = {
  method: "GET",
  mode: "cors",
  cache: "no-cache",
  credentials: "same-origin",
  headers: {
    "Content-Type": "application/json",
  },
};

const POST_GRAPHQL: RequestInit = {
  method: "POST",
  mode: "cors",
  cache: "no-cache",
  credentials: "same-origin",
  headers: {
    "Content-Type": "application/graphql",
    "X-XSRF-TOKEN": (function () {
      const row = document.cookie
        .split(";")
        .find((row) => row.startsWith("XSRF-TOKEN"));
      if (row) {
        return row.split("=")[1];
      } else {
        console.error("Missing XSRF cookie");
        return "";
      }
    })(),
  },
};

function postGraphQl(query: string): RequestInit {
  const out: RequestInit = {
    method: "POST",
    mode: "cors",
    cache: "no-cache",
    credentials: "same-origin",
  };
  out.headers = {
    "Content-Type": "application/graphql",
    "X-XSRF-TOKEN": (function (): string {
      const row = document.cookie
        .split(";")
        .find((row) => row.startsWith("XSRF-TOKEN"));
      if (row) {
        return row.split("=")[1];
      } else {
        console.error("Missing XSRF cookie");
        return "";
      }
    })(),
  };

  out.body = query;

  return out; // as RequestInit;
}

export default {
  GRAPHQL_API_URL: (function () {
    const CREATE_REACT_APP_PORT = "3000";
    const FRACKEND_PORT = "8080";
    const CLOUD_PORT = "80";
    switch (window.location.port) {
      case CREATE_REACT_APP_PORT:
      case FRACKEND_PORT:
      case CLOUD_PORT:
      default:
        return "/api/v1/nom-api/graphql";
    }
  })(),
  GET_SIMPLE,
  POST_GRAPHQL,
};
