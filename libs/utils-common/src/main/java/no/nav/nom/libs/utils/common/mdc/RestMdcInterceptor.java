package no.nav.nom.libs.utils.common.mdc;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

import static no.nav.nom.libs.utils.common.mdc.MdcConfig.HEADER_CALL_ID_KEY;
import static no.nav.nom.libs.utils.common.mdc.MdcConfig.HEADER_CONSUMER_ID_KEY;
import static no.nav.nom.libs.utils.common.mdc.MdcConfig.HEADER_X_REQUEST_ID_KEY;
import static no.nav.nom.libs.utils.common.mdc.MdcConfig.MDC_CALL_ID_KEY;
import static no.nav.nom.libs.utils.common.mdc.MdcConfig.MDC_CONSUMER_ID_KEY;
import static no.nav.nom.libs.utils.common.mdc.MdcConfig.MDC_X_REQUEST_ID_KEY;

@Component
public class RestMdcInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String callId = extractAndValidateHeaderValue(request, HEADER_CALL_ID_KEY);
        String consumerId = extractAndValidateHeaderValue(request, HEADER_CONSUMER_ID_KEY);
        var xRequestId = Optional.ofNullable(request.getHeader(HEADER_X_REQUEST_ID_KEY));

        MDC.put(MDC_CALL_ID_KEY, callId);
        MDC.put(MDC_CONSUMER_ID_KEY, consumerId);
        if (xRequestId.isPresent()) {
            MDC.put(MDC_X_REQUEST_ID_KEY, xRequestId.get());
        } else {
            MDC.put(MDC_X_REQUEST_ID_KEY,callId);
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        MDC.remove(MDC_CONSUMER_ID_KEY);
        MDC.remove(MDC_CALL_ID_KEY);
        MDC.remove(MDC_X_REQUEST_ID_KEY);
    }

    private String extractAndValidateHeaderValue(HttpServletRequest request, String headerName) {
        return Optional.ofNullable(request.getHeader(headerName))
                .filter(headerValue -> !headerValue.isEmpty())
                .orElseThrow(() -> new MissingHttpHeaderException(headerName));
    }
}
