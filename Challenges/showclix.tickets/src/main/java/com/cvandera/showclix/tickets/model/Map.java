package com.cvandera.showclix.tickets.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Map {

	private java.util.Map<String, Seat>	map;
	private int							rows;
	private int							columns;
	private final int					MAX_RESERVE	= 10;

	public Map(int rows, int columns, String[] reserved) {
		map = new HashMap<String, Seat>();

		// Creating the Map
		for (int i = 1; i < rows + 1; i++) {
			for (int j = 1; j < columns + 1; j++) {
				String seat = formKey(i, j);
				map.put(seat, new Seat(i, j));
			}
		}

		// Reserving the initial seats
		if ( reserved != null ) {
			for (String seat : reserved) {
				map.get(seat).setReserved(true);
			}
		}
		this.rows = rows;
		this.columns = columns;
	}

	/**
	 * Forms the key for the map, in the format RrCc where r the row number and
	 * c the column number of the seat
	 * 
	 * @param r
	 *            Row number
	 * @param c
	 *            Column number
	 * @return The string key
	 */
	private String formKey(int r, int c) {
		String seat = "R" + r + "C" + c;
		return seat;
	}

	/**
	 * Finds the best seats available and reserves them.
	 * 
	 * @param seats
	 *            The number of seats to be reserved
	 * @return The result of the reservation
	 */
	public String reserve(int seats) {
		if ( (seats > MAX_RESERVE) || (seats < 1) ) {
			// If trying to reserve more seats than it is allowed
			return "Not Available";
		}
		int center = (columns / 2) + 1; // Find the center seat

		java.util.Map<Double, List<Seat>> result = new HashMap<Double, List<Seat>>();

		for (int i = 1; i < rows + 1; i++) { // Look for best seats in each row
			if ( hasFreeSeats(i, seats) ) { // If there are enough available
											// seats in that row
				java.util.Map<Double, List<Seat>> availableSeats = findSeats(i, seats, center);

				if ( !availableSeats.isEmpty() ) {
					if ( result.isEmpty() ) result = new HashMap<Double, List<Seat>>(availableSeats);

					if ( availableSeats.keySet().iterator().next() <= result.keySet().iterator().next() ) {
						// The <= condition gives as the last best result we
						// find. We can change it to < to give us the first best
						// result
						result = new HashMap<Double, List<Seat>>(availableSeats);
					}

				}
			}
		}

		if ( result.isEmpty() ) { return "Not Available"; }
		// Iterator<List<Seat>> it = result.values().iterator();
		List<Seat> resultSeats = result.values().iterator().next();

		for (Seat seat : resultSeats) {
			map.get(seat.toString()).setReserved(true);
		}
		if ( resultSeats.size() == 1 ) return resultSeats.get(0).toString();
		return (resultSeats.get(0) + "-" + resultSeats.get(resultSeats.size() - 1));
	}

	/**
	 * Finds the best seats available in a specific row and return them
	 * 
	 * @param i
	 *            Row number
	 * @param seats
	 *            Number of seats to be reserved
	 * @param center
	 *            Center seat
	 * @return The list of seats selected to be reseved and the distance that
	 *         was calculated
	 */
	private java.util.Map<Double, List<Seat>> findSeats(int i, int seats, int center) {
		List<Seat> rowSeats = getRow(i);
		Seat centerSeat = map.get(formKey(1, center));

		double minDistance = 0;
		List<Seat> reserveSeats = new ArrayList<Seat>();

		double currentDistance = 0;
		List<Seat> currentSeats = new ArrayList<Seat>();
		int nSeats = 0;

		for (Seat seat : rowSeats) {

			// If the number of sequential seats is the one we are looking for
			if ( nSeats == seats ) {

				// The first nSeats to be found will be assumed to have the
				// minDistance
				// If the total distance calculated is better for this
				// combination of seats
				if ( (currentDistance <= minDistance) || (minDistance == 0) ) {
					// The <= condition gives as the last best result we find.
					// We can change it to < to give us the first best result
					minDistance = currentDistance; // change the result
					reserveSeats = new ArrayList<Seat>(currentSeats);
				}
				// remove the first seat, so that there is room for the next
				// one, if it is not reserved

				Seat removed = currentSeats.remove(0);
				nSeats--;
				currentDistance -= removed.getDistanceFromSeat(centerSeat);
			}
			if ( seat.isReserved() ) { // If the seat is reserved the sequence
										// is blown -> need to reset
				nSeats = 0;
				currentDistance = 0;
				currentSeats.clear();
				continue;
			}
			currentSeats.add(seat); // If the seat is free add it to the
									// sequence and calculate the new total
									// distance
			currentDistance += seat.getDistanceFromSeat(centerSeat);
			nSeats++;
		}

		java.util.Map<Double, List<Seat>> result = new HashMap<Double, List<Seat>>();
		result.put(minDistance, reserveSeats);

		return result;
	}

	/**
	 * Calculates if the specific row has enough free seats
	 * 
	 * @param r
	 *            Row number
	 * @param seats
	 *            Number of seats to be reserved
	 * @return True/False if there are enough seats or not
	 */
	private boolean hasFreeSeats(int r, int seats) {
		List<Seat> row = getRow(r);
		int nSeats = 0;
		for (Seat seat : row) {
			if ( seat.isReserved() ) {
				nSeats = 0;
				continue;
			}
			nSeats++;
			if ( nSeats == seats ) return true;
		}
		return false;
	}

	/**
	 * Returns the seats of a whole row
	 * 
	 * @param r
	 *            Row number
	 * @return List of seats of the row
	 */
	private List<Seat> getRow(int r) {
		List<Seat> row = new ArrayList<Seat>();
		for (int i = 1; i < columns + 1; i++) {
			row.add(map.get(formKey(r, i)));
		}
		return row;
	}

	/**
	 * Prints the seat map showing the reserved and free seats
	 * 
	 * R for reserved F for free
	 * 
	 * The counting of rows starts top bottom (the first row is the closest to
	 * the stage)
	 */
	public void print() {
		System.out.println("      -------------------");
		System.out.println("      |      STAGE      |");
		System.out.println("      -------------------");
		for (int i = 1; i < rows + 1; i++) {
			for (int j = 1; j < columns + 1; j++) {
				String seat = formKey(i, j);
				String reserved = map.get(seat).isReserved() ? "R" : "F";
				System.out.print(reserved + "  ");
			}
			System.out.println();
		}
		System.out.println("*******************************");
	}

	public int getRows() {
		return rows;
	}

	public void setRows(int rows) {
		this.rows = rows;
	}

	public int getColumns() {
		return columns;
	}

	public void setColumns(int columns) {
		this.columns = columns;
	}

}
