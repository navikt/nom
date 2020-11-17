package no.nav.nom.skjermede.personer.pip;

import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Aspect
@Component
public class CacheCallbackAspect {
    private List<Callback> callbacks = new ArrayList<>();

    public void addCallback(Callback callback) {
        callbacks.add(callback);
    }


    @After("execution(* no.nav.nom.skjermede.personer.pip.Cache.store(..))")
    public void callbackAfterCacheMethodInvocation() {
        for (Callback callback: callbacks) {
            callback.notifyMe();
        }
    }
}
