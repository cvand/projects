package cmu.edu.cloud.mapreduce;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ArticleReducer {
//	private static final String	defaultFilename	= "output.txt";

	public static void main(String[] args) {
//		String filename = defaultFilename;
//		if ( args.length > 0 ) filename = args[0];
//
//		try {
//			System.setIn(new FileInputStream(new File(filename)));
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		}

		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		try {

			System.out.println("Reducer> Starting proccessing...");
			ArticleReducer reducer = new ArticleReducer();
			reducer.readInput(br);
			System.out.println("Reducer> Proccessing Done");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void readInput(BufferedReader br) throws IOException {
		BufferedWriter bwPopular = new BufferedWriter(new FileWriter(new File("most_popular_month.txt"), true));
		BufferedWriter bwPopularDaily = new BufferedWriter(new FileWriter(new File("most_popular_daily.txt"), true));
		int daysGoogle = 0;
		Map<String, GoogleAmazonData> gaData = new HashMap<String, ArticleReducer.GoogleAmazonData>();

		String mostPopular = "";
		long totalArticles = 0;
		long maxViews = 0;

		String input;
		String previousArticle = "";
		boolean sameArticle = false;
		long totalPageViews = 0;
		Map<String, Long> dailyViewsMap = new HashMap<String, Long>();
		
		long apesMaxViews = 0;
		String apesDate = "";

		while ((input = br.readLine()) != null) {
			String[] articleInfo = input.split("\t");
			if ( articleInfo.length != 2 ) {
				System.out.println("The input format should be: <article>\\t<views>_<day>_<date>");
				break;
			}
			String value = articleInfo[1];
			String[] info = value.split("_");
			if ( info.length != 2 ) {
				System.out.println("The input format should be: <article>\\t<views>_<day>_<date>");
				break;
			}

			String article = articleInfo[0];
			long views = Long.parseLong(info[0]);
			String date = info[1];

			if ( article.equals(previousArticle) ) {
				sameArticle = true;
				totalPageViews += views;

				Long dViews = dailyViewsMap.get(date);
				if ( dViews != null ) {
					dViews += views;
				} else {
					dailyViewsMap.put(date, views);
				}
			} else {

				if ( !sameArticle ) {
					totalPageViews = views;
				}

				if ( totalPageViews > 100000 ) {
					StringBuilder sb = new StringBuilder();
					sb.append(totalPageViews);
					sb.append("\t");
					sb.append(article);
					sb.append("\t ");

					Iterator it = dailyViewsMap.entrySet().iterator();
					while (it.hasNext()) {
						String k = (String) it.next();
						sb.append(k);
						sb.append(":");
						sb.append(dailyViewsMap.get(k));
						sb.append("\t ");
					}
					System.out.println(sb.toString());
				}
				
				if (article.equals("Amazon")) {
					GoogleAmazonData data = new GoogleAmazonData();
					data.setDate(date);
					data.setAmazonViews(totalPageViews);
					gaData.put(date, data);
				}
				
				if (article.equals("Google")) {
					GoogleAmazonData data = gaData.get(date);
					if (data != null) {
						data.setGoogleViews(totalPageViews);
					}
				}
				
				if ( (article.equals("Cristiano_Ronaldo")) || article.equals("Neymar") || article.equals("Arjen_Robben")
						|| article.equals("Tim_Howard") || article.equals("Miroslav_Klose") ) {
					System.out.println("------>" + article + ": " + totalPageViews);
					bwPopular.write(article + ": " + totalPageViews);
					bwPopular.newLine();
				}

				if ( (article.equals("The_Fault_in_Our_Stars_(film)")) 
						|| article.equals("Guardians_of_the_Galaxy_(film)")
						|| article.equals("Maleficent_(film)") 
						|| article.equals("Gravity_(film)") 
						|| article.equals("Her_(film)") ) {

					Iterator it = dailyViewsMap.entrySet().iterator();
					while (it.hasNext()) {
						
						System.out.println("------>" + it + " -- " + article + ": " + dailyViewsMap.get(it));
						bwPopularDaily.write(it + " -- " + article + ": " + dailyViewsMap.get(it));
						bwPopularDaily.newLine();
					}
				}
				
				if (article.equals("Dawn_of_the_Planet_of_the_Apes")) {
					Iterator it = dailyViewsMap.keySet().iterator();
					while (it.hasNext()) {
						String d = (String) it.next();
						if (dailyViewsMap.get(d) > apesMaxViews) {
							apesMaxViews = totalPageViews;
							apesDate = d;
						}
					}
					
				}

				if ( totalPageViews > maxViews ) {
					maxViews = totalPageViews;
					mostPopular = article;
				}
				totalArticles++;
				previousArticle = article;
				sameArticle = false;
				dailyViewsMap.clear();
				totalPageViews = 0;
			}
		}

		bwPopular.close();
		bwPopularDaily.close();
		
		Iterator it = gaData.keySet().iterator();
		while (it.hasNext()) {
			String date = (String) it.next();
			GoogleAmazonData data = gaData.get(date);
			if (data.googleHasMostViews()) daysGoogle++;
		}
		System.out.println("Days where Google was more popular than Amazon: " + daysGoogle);
		System.out.println("Dawn of the planet of the apes DATE: " + apesDate);
		
		System.out.println("--------------------");
		System.out.println("Total number of articles: " + totalArticles);
		System.out.println("Most popular article: " + mostPopular);
		System.out.println("Views of most popular article: " + maxViews);

	}
	
	private class GoogleAmazonData {
		private String date;
		private long googleViews;
		private long amazonViews;
		
		public boolean googleHasMostViews() {
			return (googleViews - amazonViews) > 0;
		}

		public void setDate(String date) {
			this.date = date;
		}

		public void setGoogleViews(long googleViews) {
			this.googleViews = googleViews;
		}

		public void setAmazonViews(long amazonViews) {
			this.amazonViews = amazonViews;
		}
		
		
	}
}
