package com.cvandera.showclix.tickets.model;

public class Position {

	private int	row;
	private int	column;

	public Position(int row, int column) {
		this.row = row;
		this.column = column;
	}

	public int getRow() {
		return row;
	}

	public void setRow(int row) {
		this.row = row;
	}

	public int getColumn() {
		return column;
	}

	public void setColumn(int column) {
		this.column = column;
	}

	/**
	 * Calculates the Manhattan Distance between this position and another
	 * 
	 * @param position	Ending point
	 * @return			The distance between the two positions
	 */
	public double getDistanceFromPosition(Position position) {
		return (Math.abs(this.row - position.getRow()) + Math.abs(this.column - position.getColumn()));
	}
}
