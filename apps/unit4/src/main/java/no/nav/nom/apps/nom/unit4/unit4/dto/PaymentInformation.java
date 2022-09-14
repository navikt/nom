package no.nav.nom.apps.nom.unit4.unit4.dto;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@Builder
@RequiredArgsConstructor
public class PaymentInformation{
    String bankAccount = "00000000019";
    String payMethod = "AM";
    String supplierGroupId = "01";
    String supplierId;
}
