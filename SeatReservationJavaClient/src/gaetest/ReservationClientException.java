package gaetest;

public class ReservationClientException extends Exception {
	private static final long serialVersionUID = 1L;

	public ReservationClientException() {
	}

	public ReservationClientException(String message) {
		super(message);
	}

	public ReservationClientException(Throwable cause) {
		super(cause);
	}

	public ReservationClientException(String message, Throwable cause) {
		super(message, cause);
	}

}
