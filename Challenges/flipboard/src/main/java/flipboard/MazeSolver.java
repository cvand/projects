package flipboard;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class MazeSolver {

	private String		mazeId;
	private String		answer			= "";
	private boolean		end				= false;
	private URL			startUrl;
	private URL			stepUrl;
	private URL			checkUrl;
	List<MazePosition>	parsedPosition	= new ArrayList<MazePosition>();

	public MazeSolver(String startUrl, String stepUrl, String checkUrl) {
		try {
			this.startUrl = new URL(startUrl);
			this.stepUrl = new URL(stepUrl);
			this.checkUrl = new URL(checkUrl);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	public MazeStep startMaze() {
		return requestStep(startUrl, true, null, null);
	}

	public MazeStep step(MazePosition position) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("s", mazeId);
		params.put("x", String.valueOf(position.getX()));
		params.put("y", String.valueOf(position.getY()));
		return requestStep(stepUrl, false, position, params);
	}

	private MazeStep requestStep(URL url, boolean start, MazePosition position, Map<String, String> params) {
		try {
			HttpURLConnection connection = doGet(url, params);
			InputStream res = connection.getInputStream();
			if ( start ) {
				URL u = connection.getURL();
				setMazeId(u);
			}
			String response = getResponse(res);

			JSONParser parser = new JSONParser();
			JSONObject obj = (JSONObject) parser.parse(response);
			end = (Boolean) obj.get("end");
			answer += (String) obj.get("letter");
			MazePosition pos;
			if ( !start ) {
				pos = position;
			} else {
				pos = setInitialPosition(connection.getURL());
			}
			MazeStep step = MazeStep.fromJSONObject(obj, pos);
			return step;

		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}

	private MazePosition setInitialPosition(URL url) {
		long x = Long.parseLong((String) getAttributeFromResponse(url, "x"));
		long y = Long.parseLong((String) getAttributeFromResponse(url, "y"));
		MazePosition position = new MazePosition(x, y);
		return position;
	}

	private HttpURLConnection doGet(URL url, Map<String, String> parameters) throws IOException {
		if ( parameters != null ) {
			StringBuilder sb = new StringBuilder();
			sb.append("?");
			Set<String> keys = parameters.keySet();
			Iterator<String> it = keys.iterator();
			while (it.hasNext()) {
				String paramKey = it.next();
				sb.append(paramKey);
				sb.append("=");
				sb.append(parameters.get(paramKey));
				sb.append("&");
			}
			sb.deleteCharAt(sb.length() - 1);
			url = new URL(url.toString() + sb.toString());
		}

		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");
		return connection;
	}

	private void setMazeId(URL u) {
		String s = (String) getAttributeFromResponse(u, "s");
		if ( s != null && (!s.equals("")) ) {
			mazeId = s;
		}
	}

	private Object getAttributeFromResponse(URL u, String key) {
		String query = u.getQuery();
		String[] params = query.split("&");
		for (String p : params) {
			if ( p.contains(key + "=") ) { return p.split("=")[1]; }
		}
		return null;
	}

	public void check() {
		Map<String, String> params = new HashMap<String, String>();
		params.put("s", mazeId);
		params.put("guess", answer);
		try {
			HttpURLConnection connection = doGet(checkUrl, params);
			InputStream res = connection.getInputStream();
			String response = getResponse(res);
			if (response.contains("success")) {
				System.out.println("Answer: " + answer);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String getResponse(InputStream res) throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(res));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		return response.toString();
	}

	public static void main(String[] args) {
		String startUrl = "https://challenge.flipboard.com/start";
		String stepUrl = "https://challenge.flipboard.com/step";
		String checkUrl = "https://challenge.flipboard.com/check";
		MazeSolver solver = new MazeSolver(startUrl, stepUrl, checkUrl);
		MazeStep step = solver.startMaze();
		System.out.println("Looking for the solution of the maze...");
		while (!solver.end) {
			if (!solver.parsedPosition.contains(step.getPosition())) {
				solver.parsedPosition.add(step.getPosition());
			}
			solver.increaseTimesParsed(step.getPosition());
			MazePosition nextPos = step.getNextStepPosition(solver.parsedPosition);
			step = solver.step(nextPos);
		}
		solver.check();
	}

	private void increaseTimesParsed(MazePosition position) {
		for (MazePosition pos : parsedPosition) {
			if (pos.equals(position)) {
				pos.increaseTimesParsed();
			}
		}
	}

}
