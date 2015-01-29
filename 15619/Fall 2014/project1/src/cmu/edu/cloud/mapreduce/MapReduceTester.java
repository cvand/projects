package cmu.edu.cloud.mapreduce;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class MapReduceTester {

	public static void main(String[] args) {
		String[] filenames = new String[args.length];
		for (int i = 0; i < args.length; i++) {
			filenames[i] = args[i];
		}
		try {

			for (String fname : filenames) {
				File file = new File(fname);
				Mapper mapper = new Mapper(new FileInputStream(file));
				mapper.map();
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
}
