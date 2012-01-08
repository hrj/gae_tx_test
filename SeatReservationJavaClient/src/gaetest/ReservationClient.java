package gaetest;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * 

POST xyz.com/seatreservation/reserve
Parameters:
ownerName : String
seatId : String
Output JSON
Eg: {result: "seat_successfully_reserved", ownerName:"moby", seatId:"s123"}
or for failure {result: "seat_taken"}
or {result: "illegal_seat_request"}

GET xyz.com/seatreservation
Output JSON array containing all reservations. Eg:
[ {ownerName:"moby", seatId:"s123"}, {...}, .... ]

POST xyz.com/seatreservation/clearAll
Output JSON
Eg: {result: "success"}


Also, the URL /seatreservationkeybased will use a datastore.get() rather than a query to test for existence.
 *
 */

public class ReservationClient {
	
	enum Method { GET, POST, PUT, DELETE };
	
	public interface RestAPI {
		public String URL_RESERVE_SEAT();
		public String URL_CLEAR_ALL();
		public String URL_GET_ALL();
	}
	final static public RestAPI QUERY_BASED_API = new RestAPI() {
		public String URL_RESERVE_SEAT()	{ return "http://gaetxtest.appspot.com/seatreservation/reserve"; }
		public String URL_CLEAR_ALL()		{ return "http://gaetxtest.appspot.com/seatreservation/clearAll"; }
		public String URL_GET_ALL()		{ return "http://gaetxtest.appspot.com/seatreservation"; }
	};
	final static public RestAPI KEY_BASED_API = new RestAPI() {
		public String URL_RESERVE_SEAT()	{ return "http://gaetxtest.appspot.com/seatreservationkeybased/reserve"; }
		public String URL_CLEAR_ALL()		{ return "http://gaetxtest.appspot.com/seatreservationkeybased/clearAll"; }
		public String URL_GET_ALL()		{ return "http://gaetxtest.appspot.com/seatreservationkeybased"; }
	};
	final static public RestAPI BADKEY_BASED_API = new RestAPI() {
		public String URL_RESERVE_SEAT()	{ return "http://gaetxtest.appspot.com/seatreservation_badkey/reserve"; }
		public String URL_CLEAR_ALL()		{ return "http://gaetxtest.appspot.com/seatreservation_badkey/clearAll"; }
		public String URL_GET_ALL()		{ return "http://gaetxtest.appspot.com/seatreservation_badkey"; }
	};

	final static private String UTF8 = "UTF-8";


	private RestAPI api;
	
	public ReservationClient(RestAPI api) {
		this.api = api;
	}
	
	
	/**
	 * Request a seat reservation via REST call
	 * 
	 * @param ownerName the unique name of the person making the reservation
	 * @param seatId the desired seadId, in the range "s1" through "s500".
	 * @throws IOException if something bad happens in the HTTP communication
	 * @throws IllegalArgumentException if the requested seat is an illegal id.
	 * @throws SeatTakenException if the requested seat is no longer available.
	 */
	public ServerResponse reserveSeat (String ownerName, String seatId) throws ReservationClientException, IllegalArgumentException, SeatTakenException {
		Map<String, String> params = new HashMap<String, String>();
		params.put("ownerName", ownerName);
		params.put("seatId", seatId);
		
		JSONObject json = toJSONObject(POST(api.URL_RESERVE_SEAT(), params));
		ServerResponse sr = parseOneJsonResponse(json);
		if ( Result.SEAT_GRANTED.equals(sr.getResult()) ) {
			if ( ! ownerName.equals(sr.getOwnerName()) ) {
				throw new ReservationClientException("passed in ownerName="+ownerName+", but got back reservation for "+sr.getOwnerName());
			}
			if ( ! seatId.equals(sr.getSeatId()) ) {
				throw new ReservationClientException("passed in seatId="+seatId+", but got back reservation for "+sr.getSeatId());
			}
			return sr; // things went fine.
		}
		else if ( Result.SEAT_DENIED.equals(sr.getResult()) ) {
			throw new SeatTakenException();
		}
		else if ( Result.SEAT_ILLEGAL.equals(sr.getResult()) ) {
			throw new IllegalArgumentException();
		}
		else {
			throw new ReservationClientException("unknown result from server");
		}
	}
	
	public void clearAll () throws ReservationClientException {
		JSONObject json = toJSONObject(POST(api.URL_CLEAR_ALL(), null));
		ServerResponse sr = parseOneJsonResponse(json);
		if ( ! Result.CLEARED.equals(sr.getResult()) ) {
			throw new ReservationClientException("clearAll failed.");
		}
	}

	/**
	 * 
	 * @return Map<String, String> where the key is the seatId, and the value is the ownerName
	 * @throws ReservationClientException
	 */
	public Map<String, String> getAllSeatAssignments() throws ReservationClientException {
		JSONArray json = toJSONArray(GET(api.URL_GET_ALL(), null));
		HashMap<String, String> map = new HashMap<String, String>();
		try {
			int len = json.length();
			for (int i=0;i<len;i++) {
				JSONObject tmp = json.getJSONObject(i);
				String ownerName = tmp.getString("ownerName");
				String seatId = tmp.getString("seatId");
				if ( map.containsKey(seatId) ) {
					throw new ReservationClientException("duplicate seatId '"+seatId+"' in seat assignments.");
				}
				map.put(seatId, ownerName);
			}
			return map;
		}
		catch (JSONException e) {
			throw new ReservationClientException("Unexpected JSON parse error, " + e.getMessage(), e);
		}
	}
	
	private String POST(String url, Map<String, String> params) throws ReservationClientException {
		return doRequest(url, Method.POST, params);
	}

	private String GET(String url, Map<String, String> params) throws ReservationClientException {
		return doRequest(url, Method.GET, params);
	}

    private String doRequest(String urlBase, Method method, Map<String, String> params) throws ReservationClientException {
		StringBuilder encoded = new StringBuilder();
		if (params != null) {
			for (String key : params.keySet()) {
				if (encoded.length() > 0) {
					encoded.append("&");
				}
				try {
					encoded.append(key).append("=").append(URLEncoder.encode(params.get(key), UTF8));
				}
				catch (UnsupportedEncodingException e) {
					// wow -- don't have UTF-8 ???
				}
			}
		}

		// build up our URL
		StringBuilder url = new StringBuilder();
		url.append(urlBase);

		// if method is a GET, and there are params, then append them to the URL
		// as query args.
		if (method.equals(Method.GET) && (encoded.length() > 0)) {
			url.append("?").append(encoded.toString());
		}
            
		try {
			URL resturl = new URL(url.toString());
			HttpURLConnection con = (HttpURLConnection) resturl.openConnection();
			con.setDoOutput(true); // FYI, this implicitly sets req method to POST
//			con.setRequestMethod("POST");
			con.setRequestProperty("Accept-Charset", UTF8);
			con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=" + UTF8);

			// initialize a new curl object
			if (method.equals(Method.GET)) {
				con.setRequestMethod("GET");
			}
			else if (method.equals(Method.POST)) {
				con.setRequestMethod("POST");
				OutputStreamWriter out = new OutputStreamWriter(con.getOutputStream());
				out.write(encoded.toString());
				out.close();
			}
			else if (method.equals(Method.PUT) ) {
				con.setRequestMethod("PUT");
				OutputStreamWriter out = new OutputStreamWriter(con.getOutputStream());
				out.write(encoded.toString());
				out.close();
			}
			else if (method.equals(Method.DELETE) ) {
				con.setRequestMethod("DELETE");
			}
			else {
				throw new ReservationClientException("Unknown method " + method.toString());
			}

			// prepare to read the response from the server.
			Reader in = null;
			try {
				if ( con.getInputStream() != null ) {
					in = new InputStreamReader(con.getInputStream(), UTF8);
				}				
			}
			catch (IOException e) {
				if ( con.getErrorStream() != null ) {
					in = new InputStreamReader(con.getErrorStream(), UTF8);
				}
			}
			if ( in == null ) {
				throw new ReservationClientException("Unable to read response from server");
			}

			// accumulate the server response
			final char[] buffer = new char[1024];
			StringBuilder jsonBuf = new StringBuilder();
			int read;
			do {
				read = in.read(buffer, 0, buffer.length);
				if (read > 0) {
					jsonBuf.append(buffer, 0, read);
				}
			} while (read >= 0);
			in.close();

			// get result code
			int responseCode = con.getResponseCode();
			if ( responseCode != 200 ) {
				throw new ReservationClientException("the server responded with an error code: " + responseCode);
			}

			// return the raw json string that we got from the server.
			return jsonBuf.toString();
		}
		catch (MalformedURLException e) {
			throw new ReservationClientException(e.getMessage());
		}
		catch (IOException e) {
			throw new ReservationClientException(e.getMessage());
		}
	}
    
    private ServerResponse parseOneJsonResponse(JSONObject json) throws ReservationClientException {
//    	System.out.println("json="+json);
		try {
			ServerResponse sr = new ServerResponse();
			String result = json.getString("result");
			if ( "seat_reserved".equals(result) ) {
				sr.setResult(Result.SEAT_GRANTED);
				sr.setOwnerName(json.getString("ownerName"));
				sr.setSeatId(json.getString("seatId"));
			}
			else if ( "seat_taken".equals(result) ) {
				sr.setResult(Result.SEAT_DENIED);
			}
			else if ( "illegal_seat_request".equals(result) ) {
				sr.setResult(Result.SEAT_ILLEGAL);
			}
			else if ( "cleared".equals(result) ) { // happens when calling cleanAll
				sr.setResult(Result.CLEARED);
			}
			else {				
				throw new ReservationClientException("Unexpected JSON result: " + result);
			}
			return sr;
		}
		catch (JSONException e) {
			throw new ReservationClientException("Unexpected JSON parse error, " + e.getMessage(), e);
		}
    }
	
	private JSONObject toJSONObject(String jsonString) throws ReservationClientException {
		try {
			return new JSONObject(jsonString);
		}
		catch (JSONException e) {
			throw new ReservationClientException("Unexpected JSON parse error, " + e.getMessage(), e);
		}
	}

	private JSONArray toJSONArray(String jsonString) throws ReservationClientException {
		try {
			return new JSONArray(jsonString);
		}
		catch (JSONException e) {
			throw new ReservationClientException("Unexpected JSON parse error, " + e.getMessage(), e);
		}
	}

}
