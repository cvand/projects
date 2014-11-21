package com.cmu.edu.database;

import java.util.List;

import com.cmu.edu.cloud.model.Tweet;

public interface Persistor {
	
	public List<Tweet> getTweetsByUserId(long userId) throws Exception;
	
	public List<Tweet> getTweetsByUserAndTime(long userId, String timestamp) throws Exception;
}
