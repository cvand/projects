package com.cmu.edu.cloud.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class AfinnDataset {

	private final String	afinnPath	= "https://s3.amazonaws.com/F14CloudTwitterData/AFINN.txt";

	public Map<String, Integer> getDataset() throws URISyntaxException, IOException {
		Map<String, Integer> ds = new HashMap<>();
		URL afinnURL = new URL(afinnPath);
		File afinn = new File(afinnURL.toURI());
		FileInputStream fis = new FileInputStream(afinn);
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));
		String line = "";
		while ((line = br.readLine()) != null) {
			String[] word = line.split("\t");
			if ( word.length > 1 ) {
				ds.put(word[0], Integer.parseInt(word[1]));
			} else if ( word.length == 1 ) {
				ds.put(word[0], 0);
			}
		}

		br.close();
		br = null;
		fis = null;

		return ds;
	}
}
