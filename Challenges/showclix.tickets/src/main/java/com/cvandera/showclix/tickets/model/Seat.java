package com.cvandera.showclix.tickets.model;

public class Seat implements Comparable<Seat> {

	private Position	position;
	private boolean		reserved;

	public Seat(int row, int column) {
		position = new Position(row, column);
		this.reserved = false;
	}

	@Override
	public String toString() {
		return "R" + position.getRow() + "C" + position.getColumn();
	}

	public Seat(Position position) {
		this.position = position;
		this.reserved = false;
	}

	public Position getPosition() {
		return position;
	}

	public void setPosition(Position position) {
		this.position = position;
	}

	public boolean isReserved() {
		return reserved;
	}

	public void setReserved(boolean reserved) {
		this.reserved = reserved;
	}

	public double getDistanceFromSeat(Seat seat) {
		return position.getDistanceFromPosition(seat.getPosition());
	}

	public int compareTo(Seat o) {
		return this.toString().compareTo(o.toString());
	}
}
