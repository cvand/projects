package cmu.edu.cloud;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LogFilter {

	private final int	VIEWS_LIMIT				= 10000;

	private long		maxViews				= 0;
	private long		lines					= 0;
	private long		filteredLines			= 0;
	private long		requests				= 0;
	private String		mostPopularArticle		= "";
	private int			mostPopularMovieViews	= 0;
	private int			mostViewsArticles		= 0;

	private File		file;

	/*
	 * File row format: <project name> <page title> <number of accesses> <total
	 * data returned in bytes>
	 * 
	 * <project name> = <language identifier>.<subproject suffix>
	 * 
	 * example: fr.b Special:Recherche/All_Mixed_Up 1 730
	 */

	public LogFilter(File file) {
		if ( file != null ) this.file = file;
	}

	public static void main(String[] args) {
		if ( args.length != 2 ) {
			System.out.println("The program needs 2 arguments!");
			return;
		}
		String filename = args[0];
		String function = args[1];

		File file = new File(filename);
		LogFilter parser = new LogFilter(file);

		if ( function.equals("popular_movie") ) {

			try {
				parser.findPopularMovieCount(filename);
				System.out.println(parser.mostPopularMovieViews);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}
		
		parser.execute();

		switch (function) {
		case "total_lines":
			System.out.println(parser.lines);
			break;
		case "total_requests":
			System.out.println(parser.requests);
			break;
		case "final_lines":
			System.out.println(parser.filteredLines);
			break;
		case "popular":
			System.out.println(parser.mostPopularArticle);
			break;
		case "popular_views":
			System.out.println(parser.maxViews);
			break;
		case "articles":
			System.out.println(parser.mostViewsArticles);
			break;
		}

	}

	public File execute() {
		try {
			File output = filterInputFile(file);
			return output;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void findPopularMovieCount(String filename) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(filename));
		String line;
		this.mostPopularMovieViews = 0;
		
		while ((line = br.readLine()) != null) {
			if ( line.contains("(film)") ) {
				String views = line.split("\t")[1];
				int v = Integer.parseInt(views);
				
				if (v > this.mostPopularMovieViews) {
					this.mostPopularMovieViews = v;
				}
			}
		}
		br.close();
	}

	private void filterFile(File file, BufferedWriter bw) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(file));
//		requests = 0;

		String line;
		while ((line = br.readLine()) != null) {
			lines++;
			Article article = parseLine(line);
			long views = article.getViews();
			requests += views;

			if ( article.isIncluded(new Exclusions()) ) {
				filteredLines++;
				bw.write(article.toString());
				bw.newLine();
				if ( views > maxViews ) {
					maxViews = views;
					mostPopularArticle = article.getPageTitle();
				}
				if ( views > VIEWS_LIMIT ) {
					mostViewsArticles++;
				}
			}
		}
		br.close();
	}

	private File filterInputFile(File file) throws IOException {
		List<String> files = splitFile(file);
		File output = new File("output");
		BufferedWriter bw = new BufferedWriter(new FileWriter(output));

		for (String fname : files) {
			File f = new File(fname);
			filterFile(f, bw);
			f.delete();
		}

		bw.close();
		return output;
	}

	private Article parseLine(String line) {
		String[] lineInfo = line.split(" ");
		String info = lineInfo[0];
		Article article = new Article(info, lineInfo[1], Long.parseLong(lineInfo[2]), Long.parseLong(lineInfo[3]));
		return article;
	}

	private List<String> splitFile(File file) throws IOException {
		List<String> filenames = new ArrayList<String>();
		String filename = file.getName();
		BufferedReader br = new BufferedReader(new FileReader(filename));
		int files = 1;
		String f = filename + "_" + files;
		filenames.add(f);
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(f)));
		int lineCount = 0;
		String line;
		while ((line = br.readLine()) != null) {
			lineCount++;
			bw.write(line);
			bw.newLine();
			if ( lineCount == 100000 ) {
				bw.close();
				files++;
				f = filename + "_" + files;
				filenames.add(f);
				bw = new BufferedWriter(new FileWriter(new File(f)));
				lineCount = 0;
			}
		}
		br.close();
		bw.close();
		return filenames;
	}

	public int getMostPopularMovie() {
		return mostPopularMovieViews;
	}

	public void setMostPopularMovie(int mostPopularMovie) {
		this.mostPopularMovieViews = mostPopularMovie;
	}

	public int getMostViewsArticles() {
		return mostViewsArticles;
	}

	public void setMostViewsArticles(int mostViewsArticles) {
		this.mostViewsArticles = mostViewsArticles;
	}

}
