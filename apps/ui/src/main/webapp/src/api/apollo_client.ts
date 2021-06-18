import {ApolloClient, createHttpLink, InMemoryCache} from '@apollo/client';
import fetchConfig from './utils_fetch_configs';
import {setContext} from '@apollo/client/link/context';

const getCookieValue = (name): string => (
  document.cookie.match('(^|;)\\s*' + name + '\\s*=\\s*([^;]+)')?.pop() || ''
);

const httpLink = createHttpLink({
  uri: fetchConfig.GRAPHQL_API_URL,
});

const csrfLink = setContext((_, {headers}) => {
  const csrfToken = getCookieValue('XSRF-TOKEN');
  return {
    headers: {
      ...headers,
      'X-XSRF-TOKEN': csrfToken
    }
  };
});

export const apolloClient = new ApolloClient({
  link: csrfLink.concat(httpLink),
  cache: new InMemoryCache(),
  defaultOptions: {
    watchQuery: {
      fetchPolicy: 'cache-and-network'
    }
  }
});
