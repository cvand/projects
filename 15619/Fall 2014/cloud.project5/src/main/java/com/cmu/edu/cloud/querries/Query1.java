package com.cmu.edu.cloud.querries;

import java.math.BigInteger;
import com.cmu.edu.Config;
import java.sql.Timestamp;
import java.util.Date;

public class Query1 {
	
	public String execute(String key) {
		StringBuilder sb = new StringBuilder();
		BigInteger keyInt = new BigInteger(key);
		BigInteger Y = keyInt.divide(Config.PUBLICKEY);
		
		sb.append(Y.toString());
		sb.append("\n");
		sb.append(Config.TEAM);
		sb.append(",");
		sb.append(Config.ACCOUNT1);
		sb.append(",");
		sb.append(Config.ACCOUNT2);
		sb.append(",");
		sb.append(Config.ACCOUNT3);
		sb.append("\n");
		
		Date timestamp = new Date();
		sb.append(new Timestamp(timestamp.getTime()));
		sb.append("\n");
		
		return sb.toString();
	}

	
}
