import { ApolloClient, HttpLink, InMemoryCache } from "@apollo/client";
import { SetContextLink } from "@apollo/client/link/context";

declare module "@apollo/client" {
  namespace ApolloClient {
    namespace DeclareDefaultOptions {
      interface WatchQuery {
        errorPolicy: "all";
      }
    }
  }
}

const getCookieValue = (name: string): string =>
  document.cookie
    .match(String.raw`(^|;)\s*` + name + String.raw`\s*=\s*([^;]+)`)
    ?.pop() || "";

const httpLink = new HttpLink({
  uri: "/frackend/nom-api/graphql",
});

const csrfLink = new SetContextLink((prevContext, operation) => {
  const csrfToken = getCookieValue("XSRF-TOKEN");
  return {
    headers: {
      ...prevContext.headers,
      "X-XSRF-TOKEN": csrfToken,
      "Content-Type": "application/json;charset=UTF8;",
    },
  };
});

export const apolloClient = new ApolloClient({
  link: csrfLink.concat(httpLink),
  cache: new InMemoryCache(),
  defaultOptions: {
    watchQuery: {
      fetchPolicy: "cache-and-network",
      errorPolicy: "all",
    },
  },
});
