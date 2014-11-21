package flipboard;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class MazeStep {

	private String				letter;
	private MazePosition		position;
	private List<MazePosition>	adjacent;

	public MazeStep(MazePosition position) {
		this.position = position;
		this.adjacent = new ArrayList<MazePosition>();
	}

	public MazeStep(int x, int y) {
		position = new MazePosition(x, y);
	}

	public List<MazePosition> getAdjacent() {
		return adjacent;
	}

	public void setAdjacent(List<MazePosition> adjacent) {
		this.adjacent = adjacent;
	}

	public void addAdjacent(MazePosition position) {
		adjacent.add(position);
	}

	public MazePosition getPosition() {
		return position;
	}

	public void setLetter(String letter) {
		this.letter = letter;
	}

	public String getLetter() {
		return letter;
	}

	public static MazeStep fromJSONObject(JSONObject obj, MazePosition pos) {
		MazeStep step = new MazeStep(pos);
		step.setLetter((String) obj.get("letter"));

		JSONArray adjacentArray = (JSONArray) obj.get("adjacent");
		Iterator it = adjacentArray.iterator();
		while (it.hasNext()) {
			JSONObject adj = (JSONObject) it.next();
			MazePosition position = new MazePosition((Long) adj.get("x"), (Long) adj.get("y"));
			step.addAdjacent(position);
		}
		return step;
	}

	public MazePosition getNextStepPosition(List<MazePosition> parsedPositions) {
		MazePosition chosen = adjacent.get(0);
		boolean parsed = false;
		for (MazePosition position : adjacent) {
			parsed = false;
			for (MazePosition pos : parsedPositions) {
				if ( position.equals(pos) ) {
					parsed = true;
					position.setTimesParsed(pos.getTimesParsed());
					break;
				}
			}
			if (parsed) {
				continue;
			} else {
				chosen = position;
				break;
			}
		}
		if (parsed) {
			for (MazePosition position : adjacent) {
				if (position.getTimesParsed() < chosen.getTimesParsed()) {
					chosen = position;
				}
			}
		}
		return chosen;
	}

	@Override
	public String toString() {
		return "Step " + position + ", letter: " + letter + ", adjecent: " + adjacent.size();
	}
}
