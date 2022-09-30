//package no.nav.nom.apps.api.graphql.common;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import graphql.execution.DataFetcherResult;
//import graphql.schema.DataFetchingEnvironment;
//import lombok.val;
//import no.nav.nom.apps.api.hendelse.contract.HendelseMetadata;
//import no.nav.nom.apps.api.hendelse.contract.HendelseMetadataSerialize;
//import org.springframework.core.env.Environment;
//import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
//import org.springframework.stereotype.Component;
//
//import java.time.LocalDateTime;
//import java.util.Arrays;
//import java.util.List;
//import java.util.Optional;
//
//@Component
//public class SecurityUtil {
//
//    private final boolean securityEnabled;
//
//    public SecurityUtil(Environment environment){
//        securityEnabled = !Arrays.stream(environment.getActiveProfiles()).toList().contains("local");
//    }
//
//    public <T> Optional<DataFetcherResult<T>> checkAuth(DataFetchingEnvironment env, List<String> preAuth) {
//        if(securityEnabled) {
//            CustomGraphQLContext context = env.getContext();
//
//            if (!context.isAuthenticated() || (preAuth != null && !preAuth.contains(((JwtAuthenticationToken) context.getAuthentication()).getTokenAttributes().get("azp")))) {
//                return Optional.of(DataFetcherResult.<T>newResult()
//                        .error(ErrorCode.UNAUTHENTICATED.construct(env, "Missing or invalid token"))
//                        .build());
//            }
//        }
//
//        return Optional.empty();
//    }
//
//    public Optional<String> getAzpName(DataFetchingEnvironment env){
//        if(securityEnabled) {
//            CustomGraphQLContext context = env.getContext();
//            val azp_name = ((JwtAuthenticationToken) context.getAuthentication()).getTokenAttributes().get("azp_name");
//
//            return Optional.ofNullable(azp_name != null ? azp_name.toString() : null);
//        }
//        return Optional.empty();
//    }
//
//    public Optional<String> getAzp(DataFetchingEnvironment env){
//        if(securityEnabled) {
//            CustomGraphQLContext context = env.getContext();
//            val azp_name = ((JwtAuthenticationToken) context.getAuthentication()).getTokenAttributes().get("azp");
//
//            return Optional.ofNullable(azp_name != null ? azp_name.toString() : null);
//        }
//        return Optional.empty();
//    }
//
//    public Optional<String> getToken(DataFetchingEnvironment env) {
//        if (securityEnabled) {
//            CustomGraphQLContext context = env.getLocalContext();
//            val token = ((JwtAuthenticationToken)context.getAuthentication()).getToken();
//
//            return Optional.of(token.getTokenValue());
//        }
//
//        return Optional.empty();
//    }
//
//
//
//    public HendelseMetadataSerialize metadataFromEnv(DataFetchingEnvironment env, JsonNode rawData){
//        CustomGraphQLContext context = env.getContext();
//        val azp_name = ((JwtAuthenticationToken) context.getAuthentication()).getTokenAttributes().get("azp_name").toString();
//        val navident = ((JwtAuthenticationToken) context.getAuthentication()).getTokenAttributes().get("NAVident").toString();
//
//        return HendelseMetadataSerialize.builder()
//                .opprettetAv(new HendelseMetadata.OpprettetAv(azp_name,navident))
//                .opprettetTidspunkt(LocalDateTime.now())
//                .rawInputData(rawData)
//                .build();
//    }
//}
