package flipboard;

public class MazePosition {
	private long	x;
	private long	y;
	private int timesParsed = 0;

	public MazePosition(long x, long y) {
		super();
		this.x = x;
		this.y = y;
	}

	public long getX() {
		return x;
	}
	
	public long getY() {
		return y;
	}
	
	public int getTimesParsed() {
		return timesParsed;
	}
	
	public void increaseTimesParsed() {
		timesParsed++;
	}
	
	public void setTimesParsed(int timesParsed) {
		this.timesParsed = timesParsed;
	}
	
	@Override
	public String toString() {
		return "position: (" + x + ", " + y + ") times: " + timesParsed;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof MazePosition)) return false;
		MazePosition pos = (MazePosition) obj;
		if ((pos.getX() == this.x) && (pos.getY() == this.y)) return true;
		return false;
	}
}
