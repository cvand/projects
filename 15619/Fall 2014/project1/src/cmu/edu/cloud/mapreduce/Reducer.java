package cmu.edu.cloud.mapreduce;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class Reducer {

	public static void main(String[] args) {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		try {

			Reducer reducer = new Reducer();
			reducer.readInput(br);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void readInput(BufferedReader br) throws IOException {
		String input;
		String previousArticle = "";
		boolean sameArticle = false;
		long totalPageViews = 0;
		Map<String, Long> dailyViewsMap = new HashMap<String, Long>();

		while ((input = br.readLine()) != null) {
			String[] articleInfo = input.split("\t");
			if ( articleInfo.length != 2 ) {
				System.err.println("The input format should be: <article>\\t<date>:<views>");
				continue;
			}

			String article = articleInfo[0];
			String value = articleInfo[1];

			String[] info = value.split(":");
			if ( info.length != 2 ) {
				System.err.println("The input format should be: <date>:<views>");
				continue;
			}

			String date = info[0];
			long views = Long.parseLong(info[1]);

			if ( article.equals(previousArticle) ) {
				sameArticle = true;
				totalPageViews += views;

				Long dViews = dailyViewsMap.get(date);
				System.err.println("dailyViews for that date gave back " + dViews);
				if ( dViews != null ) {
					dViews += views;
				} else {
					dailyViewsMap.put(date, views);
				}
			} else {

				if ( !sameArticle ) {
					dailyViewsMap.put(date, views);
					totalPageViews = views;
				}

				if ( totalPageViews > 100000 ) {

					StringBuilder sb = new StringBuilder();
					sb.append(totalPageViews);
					sb.append("\t");
					sb.append(article);
					sb.append("\t ");

					int day = 20141100;
					for (int i=0; i < 31; i++) {
						day++;
						String k = String.valueOf(day);
						sb.append(k);
						sb.append(":");
						
						Long dViews = dailyViewsMap.get(String.valueOf(day));
						if ( dViews != null ) {
							sb.append(dViews);
						} else {
							sb.append('0');
						}
						sb.append("\t ");
					}
					System.out.println(sb.toString());
				}

				previousArticle = article;
				sameArticle = false;
				dailyViewsMap.clear();
				totalPageViews = 0;
			}

		}

	}
}
