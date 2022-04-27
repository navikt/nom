package no.nav.nom.apps.nom.agresso.agresso;

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
