package cmu.edu.cloud;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LogParser {

	private static long		maxViews			= 0;
	private static long		lines				= 0;
	private static long		filteredLines		= 0;
	private static long		requests			= 0;
	private static String	mostPopularArticle	= "";

	private File			file;

	/*
	 * File row format: <project name> <page title> <number of accesses> <total
	 * data returned in bytes>
	 * 
	 * <project name> = <language identifier>.<subproject suffix>
	 * 
	 * example: fr.b Special:Recherche/All_Mixed_Up 1 730
	 */

	public LogParser(File file) {
		if ( file != null ) this.file = file;
	}

	public static void main(String[] args) {
		String filename = args[0];
		File file = new File(filename);
		LogParser parser = new LogParser(file);
		parser.parse();
	}

	public File parse() {
		try {
			System.out.println("Began parsing...");
			long startTime = System.nanoTime();
			File output = parseInputFile(file);
			long stopTime = System.nanoTime();
			System.out.println("Done!");
			printResults();
			long elapsedTime = stopTime - startTime;
			double seconds = elapsedTime / 1000000000.0;
			System.out.println("Calculation time: " + seconds + "secs");
			return output;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	// private File parseFile(File file) throws IOException {
	// BufferedReader br = new BufferedReader(new FileReader(file));
	//
	// File output = new File("Output_" + file.getName() + ".txt");
	// BufferedWriter bw = new BufferedWriter(new FileWriter(output));
	// requests = 0;
	//
	// String line;
	// while ((line = br.readLine()) != null) {
	// lines++;
	// Article article = parseLine(line);
	// long views = article.getViews();
	// requests += views;
	//
	// if ( article.isIncluded(new Exclusions()) ) {
	// bw.write(article.toString());
	// bw.newLine();
	// filteredLines++;
	// if ( views > maxViews ) {
	// maxViews = views;
	// mostPopularArticle = article.getPageTitle();
	// }
	// }
	// }
	// br.close();
	// bw.close();
	// return output;
	// }

	private void parseFile(File file, BufferedWriter bw) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(file));
		requests = 0;

		String line;
		while ((line = br.readLine()) != null) {
			lines++;
			Article article = parseLine(line);
			long views = article.getViews();
			requests += views;

			if ( article.isIncluded(new Exclusions()) ) {
				bw.write(article.toString());
				bw.newLine();
				filteredLines++;
				if ( views > maxViews ) {
					maxViews = views;
					mostPopularArticle = article.getPageTitle();
				}
			}
		}
		br.close();
	}

	private File parseInputFile(File file) throws IOException {
		List<String> files = splitFile(file);
		File output = new File("output.txt");
		BufferedWriter bw = new BufferedWriter(new FileWriter(output));
		for (String f : files) {
			parseFile(f, bw);
		}
		bw.close();
		return output;
	}

	private void parseFile(String fname, BufferedWriter bw) throws IOException {
		File f = new File(fname);
		parseFile(f, bw);
		f.delete();
	}

	private Article parseLine(String line) {
		String[] lineInfo = line.split(" ");
		String info = lineInfo[0];
		Article article = new Article(info, lineInfo[1], Long.parseLong(lineInfo[2]), Long.parseLong(lineInfo[3]));
		return article;
	}

	private void printResults() {
		System.out.println("------------------------------------------------------------------------");
		System.out.println("Total number of articles: " + lines);
		System.out.println("Number of included articles: " + filteredLines + "   ?~ 1856476");
		System.out.println("Total number of requests: " + requests);
		System.out.println("Most popular article: " + mostPopularArticle);
		System.out.println("Most popular article views: " + maxViews);
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

}
