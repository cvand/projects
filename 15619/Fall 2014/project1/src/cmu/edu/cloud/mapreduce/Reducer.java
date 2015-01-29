package cmu.edu.cloud.mapreduce;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class Reducer {

	public static void main(String[] args) {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		try {

			System.out.println("Reducer> Starting proccessing...");
			Reducer reducer = new Reducer();
			reducer.readInput(br);
			System.out.println("Reducer> Proccessing Done");
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

		File fout = new File("output");
		FileOutputStream fos = new FileOutputStream(fout);
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

		while ((input = br.readLine()) != null) {
			System.out.println(input);
			String[] articleInfo = input.split("\t");
			if ( articleInfo.length != 2 ) {
				System.out.println("The input format should be: <article>\\t<date>:<views>");
				break;
			}

			String article = articleInfo[0];
			String value = articleInfo[1];

			String[] info = value.split(":");
			if ( info.length != 2 ) {
				System.out.println("The input format should be: <article>\\t<date>:<views>");
				break;
			}

			String date = info[0];
			long views = Long.parseLong(info[1]);

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
					Map<String, Long> sortedDailyViews = new TreeMap<String, Long>(dailyViewsMap);

					StringBuilder sb = new StringBuilder();
					sb.append(totalPageViews);
					sb.append("\t");
					sb.append(article);
					sb.append("\t ");

					for (Map.Entry<String, Long> entry : sortedDailyViews.entrySet()) {
						String k = (String) entry.getKey();
						sb.append(k);
						sb.append(":");
						sb.append(entry.getValue());
						sb.append("\t ");
					}

					System.out.println(sb.toString());
					bw.write(sb.toString());
					bw.newLine();
				}

				previousArticle = article;
				sameArticle = false;
				dailyViewsMap.clear();
				totalPageViews = 0;
			}

		}

		bw.close();

	}
}
