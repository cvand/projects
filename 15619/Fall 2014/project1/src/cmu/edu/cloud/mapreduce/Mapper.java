package cmu.edu.cloud.mapreduce;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.StringTokenizer;

import cmu.edu.cloud.LogFilter;

public class Mapper {

	private String	filename;
	private File	file;

	public Mapper(InputStream is) {
		if ( filename == null ) filename = "input_temp";
		try {
			file = createTempFile(is);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		 Mapper mapper = new Mapper(System.in);
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
		
		//FIXME: Delete the hardcoded filename
//		filename = "s3://...-pagecounts-20141126-150000";

		try {
			LogFilter filter = new LogFilter(file);
			File output = filter.execute();
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
		OutputStream os = new FileOutputStream(originalData);
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		byte[] buffer = new byte[1024];
		int bytesRead;
		while ((bytesRead = is.read(buffer)) != -1) {
			os.write(buffer, 0, bytesRead);
		}
		os.close();
		br.close();
		return originalData;
	}

	private void mapInput(BufferedReader br, String filename) throws IOException {
		String input;
		while ((input = br.readLine()) != null) {
			StringTokenizer tk = new StringTokenizer(input);
			if ( tk.countTokens() > 2 ) {
				System.err.println("Input is not by the format: <article>\\t<views>");
				break;
			} else {
				String article = (String) tk.nextElement();
				String views = (String) tk.nextElement();
				String date = extractDate(filename);
				
				System.out.println(article + "\t" + date + ":" + views);
				
			}
		}
	}

	private String extractDate(String file) {
		String[] parts = file.split("-");
		if (parts.length == 4) {
			return parts[2];
		} else return parts[1];
	}
}
