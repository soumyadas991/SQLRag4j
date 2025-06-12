package dev.genai.sqlrag4j.exceptions;

public class ModelNotDefinedException extends RuntimeException {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ModelNotDefinedException() {
        super("Model is not defined.");
    }

    public ModelNotDefinedException(String message) {
        super(message);
    }

    public ModelNotDefinedException(String message, Throwable cause) {
        super(message, cause);
    }

    public ModelNotDefinedException(Throwable cause) {
        super(cause);
    }
}
