package cmu.edu.cloud;

public class Test {

	public static void main(String[] args) {
		for (int i = 1; i < 10; i++) {
			for (int j = 0; j < 10; j++) {
				System.out.println("https://wikipediatraf.s3.amazonaws.com/201407-gz/pagecounts-2014070" + i + "-0" + j + "0000");
			}
			for (int j = 10; j < 24; j++) {
				System.out.println("https://wikipediatraf.s3.amazonaws.com/201407-gz/pagecounts-2014070" + i + "-" + j + "0000");
			}
			
		}
		for (int i = 10; i < 13; i++) {
			for (int j = 0; j < 10; j++) {
				System.out.println("https://wikipediatraf.s3.amazonaws.com/201407-gz/pagecounts-201407" + i + "-0" + j + "0000");
			}
			for (int j = 10; j < 24; j++) {
				System.out.println("https://wikipediatraf.s3.amazonaws.com/201407-gz/pagecounts-201407" + i + "-" + j + "0000");
			}
			
		}
	}
}
