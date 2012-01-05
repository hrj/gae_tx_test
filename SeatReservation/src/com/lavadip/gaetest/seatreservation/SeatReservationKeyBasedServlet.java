package com.lavadip.gaetest.seatreservation;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.*;

import java.util.ConcurrentModificationException;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Transaction;


@SuppressWarnings("serial")
public class SeatReservationKeyBasedServlet extends HttpServlet {
	final private static String SEAT_ENTITY_NAME = "Seat";
	
	public void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
		resp.setContentType("application/json");
		final Query seatQuery = new Query(SEAT_ENTITY_NAME, seatsRootKey);
		final PreparedQuery seats = datastore.prepare(seatQuery);
		final PrintWriter writer = resp.getWriter();
		writer.println("[");
		for (final Entity seat : seats.asIterable()) {
			writer.println(String.format("{ownerName:\"%s\", seatId:\"%s\"}", seat.getProperty("ownerName"), seat.getKey().getName()));
		}
		writer.println("]");
	}
	
	@Override
	protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		final String path = req.getPathInfo();
		resp.setContentType("application/json");
		if ("/reserve".equals(path)) {
			final String ownerName = req.getParameter("ownerName");
			final String seatId = req.getParameter("seatId");
			final int seatNum = Integer.parseInt(seatId.substring(1));
			if (seatId.startsWith("s") && seatNum > 0 && seatNum <= 500) {
				final int successNRetries = reserveSeat(ownerName, seatId);
				if (successNRetries >= 0) {
					resp.getWriter().println(String.format("{result:\"seat_reserved\", ownerName:\"%s\", seatId:\"%s\", retries:%d}", ownerName, seatId, successNRetries));
				} else {
					resp.getWriter().println(String.format("{result:\"seat_taken\", seatId:\"%s\"}", seatId));
				}
			} else {
				resp.getWriter().println("{result:\"illegal_seat_request\"}");
			}
		} else if ("/clearAll".equals(path)) {
			int count = 0;
			final Query query = new Query(SEAT_ENTITY_NAME, seatsRootKey);
			query.setKeysOnly();
			final PreparedQuery prepQuery = datastore.prepare(query);
			for (Entity e : prepQuery.asIterable()) {
				datastore.delete(e.getKey());
				count++;
			}
			resp.getWriter().println(String.format("{result:\"cleared\", count:%d}", count));
		} else {
			resp.getWriter().println("{result:\"invalid api\"}");
		}
		
	}
	
  static private Key seatsRootKey;
  final static DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

  static {
    final Entity rootEntity = new Entity("SeatsRoot", "seats_root_keybased"); // we want only one of these entities to ever exist
    datastore.put(rootEntity);
    seatsRootKey = rootEntity.getKey();
  }

  public int reserveSeat(final String ownerName, final String seatId) {
  	for (int i = 0; i < 40; i++) {
	    try {
	      final boolean success = reserveSeatAttempt(ownerName, seatId);
	      if (success) {
	      	return i;
	      } else {
	      	return -1;
	      }
	    } catch (final ConcurrentModificationException cme) {
	    	try { Thread.sleep(50); } catch (InterruptedException e) {}
	    }
  	}
    return -1;
  }

  private boolean reserveSeatAttempt(final String ownerName, final String seatId) throws ConcurrentModificationException {
    final Transaction txn = datastore.beginTransaction();
    try {
      try {
	      final Entity exists = datastore.get(txn, KeyFactory.createKey(seatsRootKey, SEAT_ENTITY_NAME, seatId));
        return exists == null;		// Should never be null, hence should return false.
      } catch (EntityNotFoundException e) {
        final Entity seatEntity = new Entity(SEAT_ENTITY_NAME, seatId, seatsRootKey);
        seatEntity.setProperty("ownerName", ownerName);
        seatEntity.setProperty("timeStamp", System.currentTimeMillis());
        datastore.put(txn, seatEntity);
        txn.commit(); // throws java.util.ConcurrentModificationException if entity group was modified by other thread
        
        return true;
      }
    }

    finally {
      if (txn.isActive()) {
        txn.rollback();
      }
    }
  }

}
