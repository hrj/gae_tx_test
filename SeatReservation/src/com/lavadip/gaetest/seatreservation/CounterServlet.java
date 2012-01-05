package com.lavadip.gaetest.seatreservation;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.*;

import java.util.ConcurrentModificationException;
import java.util.LinkedList;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Transaction;


@SuppressWarnings("serial")
public class CounterServlet extends HttpServlet {
	private ShardedCounter counter = new ShardedCounter("testCounter");
	
	public void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
		resp.setContentType("application/json");
		final PrintWriter writer = resp.getWriter();
		writer.println(String.format("[counterValue=%d, shardCount=%d, missedExceptionCount=%d]", counter.getCount(), counter.getShardCount(), counter.missedCount));
	}
	
	@Override
	protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		final String path = req.getPathInfo();
		resp.setContentType("application/json");
		if ("/increment".equals(path)) {
			counter.increment();
			resp.getWriter().println(String.format("{result:\"done\"}"));
		} else {
			resp.getWriter().println("{result:\"invalid api\"}");
		}
	}
}
