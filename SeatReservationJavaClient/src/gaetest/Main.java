package gaetest;


import gaetest.MobsterThread.ReservationCallback;
import gaetest.ReservationClient.RestAPI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class Main {
	
	final static RestAPI API = ReservationClient.BADKEY_BASED_API;
	final static int N = 1000;
	final static int M = 200;
	
	static public void main(String[] args) throws Exception {
		doMobTest();
	}
	
	static private void doMobTest() throws Exception {

		final HashMap<String, String> seatingChart = new HashMap<String, String>();
		ReservationCallback callback = new ReservationCallback() {
			@Override
			public void onFailure(String ownerName, String seatId, String errorMessage) {
				System.out.println(" !  "+errorMessage+" (failed while trying to get seat "+seatId+" for "+ownerName+")");
			}
			@Override
			public void onSeatDenied(String ownerName, String seatId) {
//				System.out.println(" -  seat "+seatId+" denied for "+ownerName);
			}
			@Override
			public void onSeatGranted(String ownerName, String seatId) {
				synchronized(seatingChart) {
					if ( seatingChart.containsKey(seatId) ) {
						System.err.println("duplicate seat assignment for seat " + seatId);
					} else {
						seatingChart.put(seatId, ownerName);						
						System.out.println(" +  seat "+seatId+" granted to "+ownerName);
					}
				}
			}
		};
		
		ReservationClient client = new ReservationClient(API);
		ArrayList<Thread> threads = new ArrayList<Thread>();
		for (int i=0;i<N;i++) {
			int j = 1 + (i % M);
			String owner = "owner" + i;
			String seat = "s" + j;
			threads.add(new Thread(new MobsterThread(client, callback, owner, seat)));
		}

		client.clearAll();

		for (int i=0;i<N;i++) {
			threads.get(i).start();
		}
		for (int i=0;i<N;i++) {
			threads.get(i).join();
		}
		
		// compare the server's seating chart against ours.
		boolean isMismatch = false;
		System.out.println("Comparing returned records against server's final record.");
		Map<String, String> serverChart = client.getAllSeatAssignments();
		if ( serverChart.size() == seatingChart.size() ) {
			for ( String seatId : serverChart.keySet() ) {
				if ( ! serverChart.get(seatId).equals(seatingChart.get(seatId)) ) {
					isMismatch = true;
					System.err.println("mismatch of owners for seat " +seatId);
				}
			}
		} else {
			throw new Exception("seating chart sizes do not match.");
		}
		if ( isMismatch ) {
			System.out.println("Mismatches were found.");
		} else {
			System.out.println("No mismatches found.");
		}
		System.out.println("Done.");
		
	}

}
