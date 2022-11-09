package no.nav.nom.contract.nom.eventsource;

public record InputMetadata(
        String inputInterfaceName,
        String inputInterfaceMethodName,
        Object inputInterfaceMethodArguments,
        Origination origination
){}