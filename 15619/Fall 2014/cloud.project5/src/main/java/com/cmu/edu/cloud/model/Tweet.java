package com.cmu.edu.cloud.model;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

public class Tweet {

	private long id;
	private String text;
	private String formattedText;
	private int score;
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public int getScore() {
		return score;
	}
	public void setScore(int score) {
		this.score = score;
	}
	public String getFormattedText() {
		return formattedText;
	}
	public void setFormattedText(String formattedText) {
		this.formattedText = formattedText;
	}
	
	public void calculateSentiment() {
		score = 0;
		String[] text = this.text.split("\\W+");
		
		AfinnDataset afinn = new AfinnDataset();
		try {
			Map<String, Integer> ds = afinn.getDataset();
			for (String w : text) {
				Integer sentiment = ds.get(w);
				if (sentiment != null) {
					score += sentiment;
				}
			}
		} catch (MalformedURLException | URISyntaxException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void formatText() {
		formattedText = text;
		String[] text = this.text.split("\\W+");
		Rot13ed rot13 = new Rot13ed();
		try {
			List<String> bannedList = rot13.getBannedWordsList();
			for (String t : text) {
				if (bannedList.contains(t)) {
					
					int length = t.length();
					StringBuilder sb = new StringBuilder(length);
				    for (int i = 0; i < length; i++) {
				    	
				    	if ((i == 0) || (i == (length - 1))) {
				    		sb.append(t.charAt(i));
				    	} else {
				    		sb.append('*');
				    	}
				    	
				    }
				    formattedText.replace(t, sb.toString());
				}
			}
		} catch (URISyntaxException | IOException e) {
			e.printStackTrace();
		}
	}
}
