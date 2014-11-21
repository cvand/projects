package cmu.edu.cloud;

public class Article {

	/*
	 * Rules: 1) language : en 2) articles start with a capital 3) exclude image
	 * files: .jpg .gif .png .JPG .GIF .PNG .txt .ico 4) exclude articles with
	 * exact title: 404_error/ Main_Page Hypertext_Transfer_Protocol Favicon.ico
	 * Search
	 * 
	 * View results by format: <article name> \t <page views>
	 */

	private String	language;
	private String	pageTitle;
	private long	views;
	private long	bytesReturned;

	public Article(String language, String pageTitle, long views, long bytesReturned) {
		super();
		this.language = language;
		this.pageTitle = pageTitle;
		this.views = views;
		this.bytesReturned = bytesReturned;
	}

	public boolean isIncluded(Exclusions exclusions) {
		boolean isEnglish = exclusions.isEnglish(language);
		char firstLetter = pageTitle.charAt(0);
		boolean isEnglishChar = ((firstLetter >= 'a' && firstLetter <= 'z') || (firstLetter >= 'A' && firstLetter <= 'Z'));
		boolean isTitleCorrect = !(Character.isLowerCase(pageTitle.charAt(0)) && isEnglishChar);
		boolean isImage = exclusions.isImage(pageTitle);
		boolean isExcludedTitle = exclusions.containsInTitle(pageTitle);
		boolean isExcludedArticle = exclusions.containsArticle(pageTitle);
		boolean included = isEnglish && isTitleCorrect && !isImage && !isExcludedTitle && !isExcludedArticle;
		return included;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getPageTitle() {
		return pageTitle;
	}

	public void setPageTitle(String pageTitle) {
		this.pageTitle = pageTitle;
	}

	public long getViews() {
		return views;
	}

	public void setViews(long views) {
		this.views = views;
	}

	public long getBytesReturned() {
		return bytesReturned;
	}

	public void setBytesReturned(long bytesReturned) {
		this.bytesReturned = bytesReturned;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(pageTitle);
		builder.append("\t");
		builder.append(views);
		return builder.toString();
	}
}
