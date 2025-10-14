import { createGraphiQLFetcher } from '@graphiql/toolkit';
import { GraphiQL } from 'graphiql';
import { createRoot } from 'react-dom/client';
import 'graphiql/style.css';

const fetcher = createGraphiQLFetcher({ url: '/frackend/nom-api/graphql' });


createRoot(document.getElementById('root')!).render(<GraphiQL fetcher={fetcher} />)
