package no.nav.nom.contract.nom.eventsource;

import java.util.Map;

public record InputMetadata(
        String inputInterfaceName,
        String inputInterfaceMethodName,
        Object inputInterfaceMethodArguments,
        Map<String, ?> additionalData,
        Origination origination
){}