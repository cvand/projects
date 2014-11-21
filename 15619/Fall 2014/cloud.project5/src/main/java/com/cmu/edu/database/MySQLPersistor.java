package com.cmu.edu.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import com.cmu.edu.cloud.model.Tweet;

public class MySQLPersistor implements Persistor {

	private final String	dbClass		= "com.mysql.jdbc.Driver";
	private final String	host		= "jdbc:mysql://localhost";
	private final String	port		= "3306";
	private final String	db			= "twitter_db";
	private final String	username	= "root";
	private final String	password	= "";

	private Connection		connection	= null;

	String					query		= "Select distinct(table_name) from INFORMATION_SCHEMA.TABLES";

	@Override
	public List<Tweet> getTweetsByUserId(long userId) throws SQLException {
		String query = "SELECT * from Tweets WHERE (userId == " + userId + ")";
		List<Tweet> tweets = new ArrayList<Tweet>();
		Statement statement = connect();
		ResultSet resultSet = statement.executeQuery(query);
		while (resultSet.next()) {
			Tweet t = new Tweet();
			t.setId(resultSet.getLong("id"));
			t.setText(resultSet.getString("text"));
			tweets.add(t);
		}
		disconnect();
		return tweets;
	}

	@Override
	public List<Tweet> getTweetsByUserAndTime(long userId, String timestamp) throws SQLException, ParseException {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		df.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date date = df.parse(timestamp);
		SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		String d = sdf.format(date);

		String query = "SELECT * from tweets WHERE (userId == " + userId + " AND created_at == '" + d + "')";
		List<Tweet> tweets = new ArrayList<Tweet>();
		Statement statement = connect();
		ResultSet resultSet = statement.executeQuery(query);
		while (resultSet.next()) {
			Tweet t = new Tweet();
			t.setId(resultSet.getLong("id"));
			t.setText(resultSet.getString("text"));
			tweets.add(t);
		}
		disconnect();
		return tweets;
	}

	private Statement connect() {
		try {
			Class.forName(dbClass);
			connection = DriverManager.getConnection(buildUrl(), username, password);
			Statement statement = connection.createStatement();
			return statement;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	private void disconnect() {
		if ( connection != null ) {
			try {
				connection.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	private String buildUrl() {
		return host + ":" + port + "/" + db;
	}

}
