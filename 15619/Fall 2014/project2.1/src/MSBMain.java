/*
 * Copyright 2010-2014 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.model.Instance;

public class MSBMain {
	private static final long	SLEEP_CYCLE	= 62000;	// 62 secs
	private static EC2Manager ec2Manager;

	public static void main(String[] args) throws Exception {
		List<Instance> instances = new ArrayList<>();
		try {
			ec2Manager = new EC2Manager();
			Instance loadGenerator = ec2Manager.createInstance("ami-1810b270", "m3.medium", "15619demo", "launch-wizard-4", "Project", "2.1");
			loadGenerator = getDnsName(loadGenerator);
			String instanceDNS = loadGenerator.getPublicDnsName();
			
			String url = " http://" + instanceDNS + "/username?username=cvandera";
			sendRequest(url);

			double sum = 0;
			do {
				if (sum < 3600) {
					Instance dataCenter = createDataCenter(ec2Manager, instanceDNS, "ami-324ae85a", "m3.medium", "15619demo", "launch-wizard-4", "Project", "2.1");
					instances.add(dataCenter);
				}
				Thread.sleep(SLEEP_CYCLE);
				sum = 0;
				url = "http://" + instanceDNS + "/view-logs?name=result_cvandera_Donut.txt";
				List<String> response = sendRequest(url);
				for (int i = 0; i < instances.size(); i++) {
					Instance inst = instances.get(i);
					for (int j = response.size() - 1; j >= 0; j--) {
						String line = response.get(j);
						if ( line.contains(inst.getPublicDnsName()) ) {
							String[] parts = line.split(":");
							String info = parts[2];
							String[] infos = info.split(" ");
							double rps = 0;
							for (int k = 0; k < infos.length; k++) {
								if (!infos[k].equals("")) {
									rps = Double.parseDouble(infos[k]);
									break;
								}
							}
							sum += rps;
							break;
						}
					}
				}
				System.out.println("Instances: " + (instances.size() - 1) + "  ---  Total RPS: " + sum);
			} while (sum < 3600);

		} catch (AmazonServiceException ase) {
			System.out.println("Caught Exception: " + ase.getMessage());
			System.out.println("Reponse Status Code: " + ase.getStatusCode());
			System.out.println("Error Code: " + ase.getErrorCode());
			System.out.println("Request ID: " + ase.getRequestId());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static Instance getDnsName(Instance instance) throws Exception {
		while (!ec2Manager.getInstanceStatus(instance).equals("ok")) {
			Thread.sleep(1000);
		}
		String instanceDNS = instance.getPublicDnsName();
		int tries = 5;
		while (((instanceDNS == null) || (instanceDNS.equals(""))) && (tries != 0)) {
			Thread.sleep(1000);
			instance = ec2Manager.getInstance(instance.getInstanceId());
			instanceDNS = instance.getPublicDnsName();
			tries--;
		}
		if ((instanceDNS == null) || instanceDNS.equals("")) {
			throw new Exception("Could not load public DNS for instance " + instance.getInstanceId());
		}
		return instance;
	}

	private static Instance createDataCenter(EC2Manager ec2Manager, String loadGeneratorDNS, String ami, String type, String keyName, String securityGroup, String tagKey, String tagValue) throws Exception {
		Instance dataCenter = ec2Manager.createInstance(ami, type, keyName, securityGroup, tagKey, tagValue);
		dataCenter = getDnsName(dataCenter);
		if (dataCenter.getPublicDnsName().equals("")) {
			throw new Exception("Could not get public DNS name for data center instance.");
		}
		String url = " http://" + dataCenter.getPublicDnsName() + "/username?username=cvandera";
		sendRequest(url);

		url = " http://" + loadGeneratorDNS + "/part/one/i/want/more?dns=" + dataCenter.getPublicDnsName() + "&testId=Donut";
		sendRequest(url);
		return dataCenter;
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
