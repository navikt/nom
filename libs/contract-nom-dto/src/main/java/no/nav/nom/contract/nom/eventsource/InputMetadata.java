package no.nav.nom.contract.nom.eventsource;

import lombok.Builder;

@Builder(toBuilder = true)
public record InputMetadata(
        String inputInterfaceName,
        String inputInterfaceMethodName,
        Object inputInterfaceMethodArguments,
        Origination origination
){}