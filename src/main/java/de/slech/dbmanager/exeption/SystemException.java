package de.slech.dbmanager.exeption;

/**
 * Runtime Exception thrown when a checked exception is caught but cannot be handled.
 */
public class SystemException extends RuntimeException {

    private static final long serialVersionUID = -2465444399517199534L;

    /**
     * @param cause exception that cannot be handled otherwise
     */
    public SystemException(Exception cause) {
        super(cause);
    }
}
