package gaetest;

public class SeatTakenException extends Exception {
	private static final long serialVersionUID = 1L;

	public SeatTakenException() {
	}

	public SeatTakenException(String message) {
		super(message);
	}

	public SeatTakenException(Throwable cause) {
		super(cause);
	}

	public SeatTakenException(String message, Throwable cause) {
		super(message, cause);
	}

}
