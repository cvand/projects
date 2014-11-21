import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


public class WarmUp {

	public void run(String lgDNS, String elbDNS, int runs) {
		String url = "http://" + lgDNS + "/warmup?dns=" + elbDNS + "&testId=warm";
		int count = runs;
		while (count > 0) {
			try {
				System.out.print(((runs + 1) - count) + ".  ");
				sendRequest(url);
				Thread.sleep(304500);
				count--;
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void main(String[] args) {
		WarmUp test = new WarmUp();
		test.run("ec2-54-164-205-86.compute-1.amazonaws.com", "MyELB-1048083970.us-east-1.elb.amazonaws.com", 5);
	}

	private static List<String> sendRequest(String url) throws MalformedURLException, IOException {

		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		con.setRequestMethod("GET");

		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		List<String> tmp = new ArrayList<String>();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
			tmp.add(inputLine);
		}
		in.close();

		// print result
		System.out.println(response.toString());
		return tmp;
	}
}
