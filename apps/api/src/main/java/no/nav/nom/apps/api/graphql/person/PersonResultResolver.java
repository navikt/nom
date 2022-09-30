//package no.nav.nom.apps.api.graphql.person;
//
//import graphql.execution.DataFetcherResult;
//import graphql.schema.DataFetchingEnvironment;
//import no.nav.nom.apps.api.graphql.common.LoaderUtils;
//import no.nav.nom.apps.api.graphql.common.loader.PersonBatchLoader;
//import no.nav.nom.contract.nom.api.graphql.FolkeregisterPerson;
//import no.nav.nom.contract.nom.api.graphql.PersonResult;
//import org.springframework.stereotype.Component;
//
//import java.util.concurrent.CompletableFuture;
//
//
//@Component
//public class PersonResultResolver implements GraphQLResolver<PersonResult> {
//
//    public CompletableFuture<DataFetcherResult<FolkeregisterPerson>> folkeregisterPerson(PersonResult personResult, DataFetchingEnvironment env) {
//        return LoaderUtils.get(env, PersonBatchLoader.PERSON_DATALOADER, personResult.getPersonIdent());
//    }
//
//    public CompletableFuture<DataFetcherResult<FolkeregisterPerson>> person(PersonResult personResult, DataFetchingEnvironment env) {
//        return folkeregisterPerson(personResult, env);
//    }
//}
