package com.cvandera.showclix.tickets;

import com.cvandera.showclix.tickets.model.Map;

public class TicketReservation {

	public static void main(String[] args) {
		String[] family = { "R1C4", "R1C6", "R2C3", "R2C7", "R3C9", "R3C10" };
		int rows = 3;
		int columns = 11;
		Map map = build(rows, columns, family);
		reserve(map, 2);
		reserve(map, 3);
		reserve(map, 7);
		reserve(map, 11);
		reserve(map, 0);
		reserve(map, 7);
		reserve(map, -3);
		reserve(map, 1);
		reserve(map, 1);
	}

	public static Map build(int rows, int columns, String[] reserved) {
		return new Map(rows, columns, reserved);
	}

	public static Map reserve(Map map, int n) {
		if ( map == null ) return null;
		String result = map.reserve(n);
		System.out.println(result);
		if ( (!result.isEmpty()) && (!result.equals("Not Available")) ) {
			map.print();
		}
		return map;
	}
}
