package com.cvand.utils.files;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipUtils {

	public static void unzip(String filename) {
		unzip(filename, "extracted");
	}

	public static void unzip(String filename, String outputPath) {
		unzip(new File(filename), new File(outputPath));
	}

	public static void unzip(File file, File output) {
		byte[] buffer = new byte[1024];

		try {
			if ( !output.exists() ) {
				output.mkdir();
			}

			ZipInputStream zis = new ZipInputStream(new FileInputStream(file));
			// get the zipped file list entry
			ZipEntry ze = zis.getNextEntry();

			while (ze != null) {

				String fileName = ze.getName();
				File newFile = new File(output + File.separator + fileName);

				System.out.println("file unzip : " + newFile.getAbsoluteFile());

				// create all non exists folders
				// else you will hit FileNotFoundException for compressed folder
				new File(newFile.getParent()).mkdirs();

				FileOutputStream fos = new FileOutputStream(newFile);

				int len;
				while ((len = zis.read(buffer)) > 0) {
					fos.write(buffer, 0, len);
				}

				fos.close();
				ze = zis.getNextEntry();
			}

			zis.closeEntry();
			zis.close();

			System.out.println("Done");

		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	public static void unzip(InputStream is, File output) {
		byte[] buffer = new byte[1024];
		
		try {
			if ( !output.exists() ) {
				output.mkdir();
			}
			
			ZipInputStream zis = new ZipInputStream(is);
			// get the zipped file list entry
			ZipEntry ze = zis.getNextEntry();
			
			while (ze != null) {
				
				String fileName = ze.getName();
				File newFile = new File(output + File.separator + fileName);
				
				System.out.println("file unzip : " + newFile.getAbsoluteFile());
				
				// create all non exists folders
				// else you will hit FileNotFoundException for compressed folder
				new File(newFile.getParent()).mkdirs();
				
				FileOutputStream fos = new FileOutputStream(newFile);
				
				int len;
				while ((len = zis.read(buffer)) > 0) {
					fos.write(buffer, 0, len);
				}
				
				fos.close();
				ze = zis.getNextEntry();
			}
			
			zis.closeEntry();
			zis.close();
			
			System.out.println("Done");
			
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	

}
