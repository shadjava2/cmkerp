package cd.shad.erp.cmk.cmkerp.sharedkernel.exception;

/**
 * Base exception class for all CMK ERP domain and technical exceptions. All business exceptions in
 * the shared-kernel inherit from this.
 */
public abstract class CmkBaseException extends RuntimeException {

  private static final long serialVersionUID = 1L;
  private final String errorCode;
  private final String technicalMessage;

  /**
   * Construct with a simple error message.
   *
   * @param message user-friendly error message
   */
  public CmkBaseException(String message) {
    super(message);
    this.errorCode = null;
    this.technicalMessage = null;
  }

  /**
   * Construct with error message and cause.
   *
   * @param message user-friendly error message
   * @param cause the underlying exception
   */
  public CmkBaseException(String message, Throwable cause) {
    super(message, cause);
    this.errorCode = null;
    this.technicalMessage = null;
  }

  /**
   * Construct with error code and message.
   *
   * @param errorCode machine-readable error code (e.g., "ERR_NOT_FOUND")
   * @param message user-friendly error message
   */
  public CmkBaseException(String errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
    this.technicalMessage = null;
  }

  /**
   * Construct with error code, message, cause and technical details.
   *
   * @param errorCode machine-readable error code (e.g., "ERR_NOT_FOUND")
   * @param message user-friendly error message
   * @param cause the underlying exception
   */
  public CmkBaseException(String errorCode, String message, Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
    this.technicalMessage = cause != null ? cause.getMessage() : null;
  }

  /**
   * Get the machine-readable error code.
   *
   * @return error code or null if not set
   */
  public String getErrorCode() {
    return errorCode;
  }

  /**
   * Get the technical/low-level error message.
   *
   * @return technical message or null if not set
   */
  public String getTechnicalMessage() {
    return technicalMessage;
  }
}
