package vn.campushub.registration;

public class RegistrationConflictException extends RuntimeException {

    private final String code;

    public RegistrationConflictException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
