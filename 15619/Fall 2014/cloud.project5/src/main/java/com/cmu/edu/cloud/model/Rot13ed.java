package com.cmu.edu.cloud.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Rot13ed {

	private final String	path	= "https://s3.amazonaws.com/F14CloudTwitterData/banned.txt";
	
	public List<String> getBannedWordsList() throws URISyntaxException, IOException {
		List<String> list = new ArrayList<>();
		URL afinnURL = new URL(path);
		File afinn = new File(afinnURL.toURI());
		FileInputStream fis = new FileInputStream(afinn);
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));
		String line = "";
		while ((line = br.readLine()) != null) {
			list.add(line);
		}

		br.close();
		br = null;
		fis = null;
		return list;
	}
}
