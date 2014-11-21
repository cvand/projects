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

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.elasticloadbalancing.model.CreateLoadBalancerResult;
import com.amazonaws.services.elasticloadbalancing.model.InstanceState;

public class MSBMain {
	private static EC2Manager ec2Manager;
	private static ElasticLoadBalancerManager elbManager;
	private static AutoScalingManager asManager;

	public static void main(String[] args) throws Exception {
		ec2Manager = new EC2Manager();
		elbManager = new ElasticLoadBalancerManager();
		asManager = new AutoScalingManager();
		
		System.out.println("Creating Elastic Load Balancer...");
		CreateLoadBalancerResult elb = elbManager.createELB("MyELB", "us-east-1c", "us-east-1b", "us-east-1a", "sg-294d794c", "Project", "2.2");
		elbManager.registerIfInstances(ec2Manager);
		String elbDnsName = elb.getDNSName();
//		String elbDnsName = "MyELB-999854684.us-east-1.elb.amazonaws.com";
		System.out.println("--Done!");
		
		System.out.println("Creating Auto Scaling Group...");
		asManager.createASG("ASG", "MyELB", 1, 5, 2, "us-east-1c", "us-east-1b", "us-east-1a");
		System.out.println("--Done!");
		
		System.out.println("Wait until all instances are InService at the ELB...");
		Thread.sleep(60000);
		List<InstanceState> states = elbManager.getInstanceStates();
		boolean ready = false;
		if (states.size() == 0) ready = true;
		do {
			for (InstanceState state : states) {
				boolean inService = state.getState().equals("InService");
				ready = inService;
				if (!ready) {
					Thread.sleep(3000);
					states = elbManager.getInstanceStates();
					break;
				}
			}
		} while (!ready);
		System.out.println("--Done!");
		
		System.out.println("Creating Load Generator....");
//		Instance lg = getLoadGenerator("i-401470ae");
//		if (lg == null) {
//			lg = createLoadGenerator();
//		}
		Instance lg = createLoadGenerator();
		Thread.sleep(240000);
		System.out.println("--Done!");
		
		System.out.println("Warming up....");
		WarmUp warmUp = new WarmUp();
		warmUp.run(lg.getPublicDnsName(), elbDnsName);
		System.out.println("--Done!");
		
		String url = "http://" + lg.getPublicDnsName() + "/begin-phase-2?dns=" + elbDnsName + "&testId=phase2";
		System.out.println("-------------------");
		System.out.println(url);
		sendRequest(url);
		
		Thread.sleep(2405000);
		url = "http://" + lg.getPublicDnsName() + "/view-logs?name=result_cvandera_phase2.txt";
		sendRequest(url);
		System.out.println("------------- Finished Test --------------");
		
		asManager.deleteASG();
	}
	
	private static Instance createLoadGenerator() throws Exception {
		Instance loadGenerator = ec2Manager.createInstance("ami-562d853e", "m3.medium", "15619demo", "launch-wizard-1", "Project", "2.2");
		loadGenerator = getDnsName(loadGenerator);
		String instanceDNS = loadGenerator.getPublicDnsName();
		
		String url = " http://" + instanceDNS + "/username?username=cvandera";
		sendRequest(url);
		return loadGenerator;
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
	
	private static Instance getLoadGenerator(String instanceId) {
		Instance lg = ec2Manager.getInstance(instanceId);
		return lg;
	}
}
