package cmu.edu.cloud.mapreduce;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.StringTokenizer;

import cmu.edu.cloud.LogParser;

public class ArticleMapper {

	private String	filename;
	private File	file;

	public ArticleMapper(String filename) {
		this.filename = filename;
		file = new File(filename);
		// TODO: delete this constructor
	}

	public ArticleMapper(InputStream is) {
		if ( filename == null ) filename = "input_temp";
		try {
			file = createTempFile(is);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		// comment those begin
//		String filename = args[0];
		// comment those end

//		ArticleMapper mapper = new ArticleMapper(filename);
//		mapper.map();
		 ArticleMapper mapper = new ArticleMapper(System.in);
		 mapper.map();

	}

	public void map() {
		filename = System.getenv("map_input_file");
		String filename2 = System.getenv("map.input.file");
		String filename3 = System.getenv("mapreduce_map_input_file");
		if ( (filename == null) && (filename2 != null) ) {
			filename = filename2;
		} else if ( (filename == null) && (filename3 != null) ) {
			filename = filename3;
		}

		try {
			LogParser parser = new LogParser(file);
			File output = parser.parse();
			file.delete();
			BufferedReader bfr = new BufferedReader(new FileReader(output));
			mapInput(bfr, filename);
			bfr.close();
			output.delete();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private File createTempFile(InputStream is) throws IOException {
		File originalData = new File("_" + filename);
		// File originalData = new File(filename);
//		BufferedWriter bw = new BufferedWriter(new FileWriter(originalData));
		OutputStream os = new FileOutputStream(originalData);
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		byte[] buffer = new byte[1024];
		int bytesRead;
		// read from is to buffer
		while ((bytesRead = is.read(buffer)) != -1) {
			os.write(buffer, 0, bytesRead);
		}
		os.close();
		br.close();
		return originalData;
	}

	private void mapInput(BufferedReader br, String filename) throws IOException {

		BufferedWriter bw = new BufferedWriter(new FileWriter(new File("mapper_output.txt"), true));

		String input;
		while ((input = br.readLine()) != null) {
			StringTokenizer tk = new StringTokenizer(input);
			if ( tk.countTokens() > 2 ) {
				System.out.println("Input is not by the format: <article>\\t<views>");
				break;
			} else {
				String article = (String) tk.nextElement();
				String views = (String) tk.nextElement();
				String date = extractDate(filename);
				System.out.println(article + "\t" + views + "_" + date);
				bw.write(article + "\t" + views + "_" + date);
				bw.newLine();
			}
		}
		bw.close();
	}

	private String extractDate(String file) {
		String date = file.substring(11, 19);
		String year = date.substring(0, 4);
		String month = date.substring(4, 6);
		String day = date.substring(6, 8);
		return month + "/" + day + "/" + year;
	}
}
