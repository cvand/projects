package cmu.edu.cloud;

import java.util.ArrayList;
import java.util.List;


public class Exclusions {

	private List<String> excludedTitles;
	private List<String> imageExtensions;
	private List<String> excludedArticles;
	
	public Exclusions() {
		excludedTitles = new ArrayList<String>();
		excludedTitles.add("Media");
		excludedTitles.add("Special");
		excludedTitles.add("Talk");
		excludedTitles.add("User");
		excludedTitles.add("User_talk");
		excludedTitles.add("Project");
		excludedTitles.add("Project_talk");
		excludedTitles.add("File");
		excludedTitles.add("File_talk");
		excludedTitles.add("MediaWiki");
		excludedTitles.add("MediaWiki_talk");
		excludedTitles.add("Template");
		excludedTitles.add("Template_talk");
		excludedTitles.add("Help");
		excludedTitles.add("Help_talk");
		excludedTitles.add("Category");
		excludedTitles.add("Category_talk");
		excludedTitles.add("Portal");
		excludedTitles.add("Wikipedia");
		excludedTitles.add("Wikipedia_talk");
		
		imageExtensions = new ArrayList<String>();
		imageExtensions.add(".jpg");
		imageExtensions.add(".gif");
		imageExtensions.add(".png");
		imageExtensions.add(".JPG");
		imageExtensions.add(".GIF");
		imageExtensions.add(".PNG");
		imageExtensions.add(".txt");
		imageExtensions.add(".ico");
		
		excludedArticles = new ArrayList<String>();
		excludedArticles.add("404_error/");
		excludedArticles.add("Main_Page");
		excludedArticles.add("Hypertext_Transfer_Protocol");
		excludedArticles.add("Search");
	}
	
	public boolean isEnglish(String language) {
		return language.equals("en");
//		return (language.equals("En") || language.equals("en") || (language.equals("EN")));
	}
	
	public boolean containsInTitle(String page) {
		boolean contains = false;
		for (String exclPage : excludedTitles) {
			if (page.startsWith(exclPage + ":")) {
				contains = true;
				break;
			}
		}
		return contains;
	}
	
	public boolean containsArticle(String article) {
		boolean contains = false;
		for (String exclArticle : excludedArticles) {
			if (article.equals(exclArticle)) {
				contains = true;
				break;
			}
		}
		return contains;
	}

	public boolean isImage(String pageTitle) {
		boolean contains = false;
		for (String imageExtension : imageExtensions) {
			if (pageTitle.endsWith(imageExtension)) {
				contains = true;
				break;
			}
		}
		return contains;
	}
}
