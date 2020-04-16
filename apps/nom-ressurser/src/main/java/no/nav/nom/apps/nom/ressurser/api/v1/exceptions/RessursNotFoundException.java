package no.nav.nom.apps.nom.ressurser.api.v1.exceptions;

public class RessursNotFoundException extends RuntimeException {

    public RessursNotFoundException(String message){
        super(message);
    }
}
