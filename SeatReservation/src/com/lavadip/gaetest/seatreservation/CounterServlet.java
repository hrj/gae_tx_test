package com.lavadip.gaetest.seatreservation;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.*;

import java.util.ConcurrentModificationException;
import java.util.LinkedList;
import java.util.List;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Query;


@SuppressWarnings("serial")
public class CounterServlet extends HttpServlet {
	private ShardedCounter counter = new ShardedCounter("testCounter");
  private static final DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
  private static final String MISSED_COUNTER_ENTITY_KIND = "missedCounter";

	
	public void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
		resp.setContentType("application/json");
		final PrintWriter writer = resp.getWriter();
		final Query q = new Query(MISSED_COUNTER_ENTITY_KIND);
		q.setKeysOnly();
		final int missedCount = ds.prepare(q).countEntities(FetchOptions.Builder.withLimit(1<<20));
		writer.println(String.format("[counterValue=%d, shardCount=%d, missedExceptionCount=%d]", counter.getCount(), counter.getShardCount(), missedCount));
	}
	
	@Override
	protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		final String path = req.getPathInfo();
		resp.setContentType("application/json");
		if ("/increment".equals(path)) {
			try {
				counter.increment();
				resp.getWriter().println(String.format("{result:\"done\"}"));
	  	} catch (final ConcurrentModificationException e) {
	  		System.out.println("Sharded Counter: Unbelievablely, we are missing a crucial exception!");
	  		final Entity entity = new Entity(MISSED_COUNTER_ENTITY_KIND);
	  		ds.put(entity);
				resp.getWriter().println(String.format("{result:\"collision\"}"));
	  	}
		} else if ("/clearAll".equals(path)) {
			final Query q = new Query(MISSED_COUNTER_ENTITY_KIND);
			q.setKeysOnly();
			final Iterable<Entity> pq = ds.prepare(q).asIterable();
			final List<Key> pqkeys = new LinkedList<Key>();
			for (Entity e : pq) {
				pqkeys.add(e.getKey());
			}
			ds.delete(pqkeys);
			
			counter.clearAllData();
			
		} else {
			resp.getWriter().println("{result:\"invalid api\"}");
		}
	}
}
