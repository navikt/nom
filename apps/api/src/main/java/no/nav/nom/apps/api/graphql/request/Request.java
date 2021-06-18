package no.nav.nom.apps.api.graphql.request;

import lombok.Value;

import java.util.UUID;

@Value
public class Request {

    UUID requestId;
    Object data;
}
