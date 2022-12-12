package no.nav.nom.apps.api.common;

public class Utils {
    public static void validateNavident(String navident) throws IllegalArgumentException {
        if (navident == null){
            throw new IllegalArgumentException("Navident argument missing");
        } else if (navident.length() != 7){
            throw new IllegalArgumentException(String.format("Length of navident is %d, should be 7", navident.length()));
        } else if (!Character.isLetter(navident.charAt(0)) || !Character.isUpperCase(navident.charAt(0))) {
            throw new IllegalArgumentException(String.format("First letter is invalid '%c' should be uppercased letter", navident.charAt(0)));
        } else {
            for (int i = 1; i<navident.length(); i++){
                if(!Character.isDigit(navident.charAt(i))){
                    throw new IllegalArgumentException(String.format("Character %c at position %d is invalid, should be a digit", navident.charAt(i), i));
                }
            }
        }
    }
}
