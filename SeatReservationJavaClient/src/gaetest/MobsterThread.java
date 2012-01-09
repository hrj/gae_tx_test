package gaetest;

public class MobsterThread implements Runnable {
	
	public interface ReservationCallback {
		public void onSeatGranted(String ownerName, String seatId);
		public void onSeatDenied(String ownerName, String seatId);
		public void onFailure(String ownerName, String seatId, String errorMessage);
	}
	
	private ReservationClient client;
	private ReservationCallback callback;
	private String ownerName;
	private String seatId;
	
	public MobsterThread(ReservationClient client, ReservationCallback callback, String ownerName, String seatId) {
		this.client = client;
		this.callback = callback;
		this.ownerName = ownerName;
		this.seatId = seatId;
	}

	@Override
	public void run() {
		try {
			ServerResponse sr = client.reserveSeat(ownerName, seatId);
			callback.onSeatGranted(sr.getOwnerName(), sr.getSeatId());
		}
		catch (IllegalArgumentException e) {
			callback.onFailure(ownerName, seatId, e.getMessage());
		}
		catch (ReservationClientException e) {
			callback.onFailure(ownerName, seatId, e.getMessage());
		}
		catch (SeatTakenException e) {
			callback.onSeatDenied(ownerName, seatId);
		}
	}

}
