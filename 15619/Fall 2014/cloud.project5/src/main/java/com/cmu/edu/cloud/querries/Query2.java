package com.cmu.edu.cloud.querries;

import java.util.ArrayList;
import java.util.List;

import com.cmu.edu.Config;
import com.cmu.edu.cloud.model.Tweet;
import com.cmu.edu.database.MySQLPersistor;
import com.cmu.edu.database.Persistor;

public class Query2 {

	public String execute(String key, String userId, String tweetTime) {
		List<Tweet> tweets = new ArrayList<Tweet>();
		Persistor persistor = new MySQLPersistor();
		// Persistor nosql = new NoSQLPersistor();
		try {
			tweets = persistor.getTweetsByUserAndTime(Long.parseLong(userId), tweetTime);
		} catch (Exception e) {
			e.printStackTrace();
		}

		StringBuilder sb = new StringBuilder();
		sb.append(Config.TEAM);
		sb.append(",");
		sb.append(Config.ACCOUNT1);
		sb.append(",");
		sb.append(Config.ACCOUNT2);
		sb.append(",");
		sb.append(Config.ACCOUNT3);
		sb.append("\n");

		for (Tweet t : tweets) {
			t.calculateSentiment();
			t.formatText();
			sb.append(t.getId());
			sb.append(":");
			sb.append(t.getScore());
			sb.append(":");
			sb.append(t.getFormattedText());
			sb.append("\n");
		}

		return sb.toString();
	}
}
