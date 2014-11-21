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

import java.util.List;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceStatus;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.Tag;

public class EC2Manager {
    private AmazonEC2         ec2;

    /**
     * Public constructor.
     * @throws Exception
     */
    public EC2Manager () throws Exception {
        init();
    }

    /**
     * The only information needed to create a client are security credentials
     * consisting of the AWS Access Key ID and Secret Access Key. All other
     * configuration, such as the service endpoints, are performed
     * automatically. Client parameters, such as proxies, can be specified in an
     * optional ClientConfiguration object when constructing a client.
     *
     * @see com.amazonaws.auth.BasicAWSCredentials
     * @see com.amazonaws.auth.PropertiesCredentials
     * @see com.amazonaws.ClientConfiguration
     */
    private void init() throws Exception {
        AWSCredentials credentials = null;
        try {
            credentials = new ProfileCredentialsProvider().getCredentials();
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Cannot load the credentials from the credential profiles file. " +
                    "Please make sure that your credentials file is at the correct " +
                    "location (~/.aws/credentials), and is in valid format.",
                    e);
        }

        ec2 = new AmazonEC2Client(credentials);
        Region usEast = Region.getRegion(Regions.US_EAST_1);
        ec2.setRegion(usEast);
    }
    
    public Instance createInstance(String ami, String type, String keyName, String securityGroup, String tagKey, String tagValue) {
    	//Create Instance Request
        RunInstancesRequest runInstancesRequest  = new RunInstancesRequest();

        //Configure Instance Request
        runInstancesRequest.withImageId(ami)
        .withInstanceType(type)
        .withMinCount(1)
        .withMaxCount(1)
        .withKeyName(keyName)
        .withSecurityGroups(securityGroup);
         
        //Launch Instance
        RunInstancesResult runInstancesResult = ec2.runInstances(runInstancesRequest);  
         
        //Return the Object Reference of the Instance just Launched
        Instance instance = runInstancesResult.getReservation().getInstances().get(0);
        Tag tag = new Tag();
        tag.setKey(tagKey);
        tag.setValue(tagValue);
        instance.getTags().add(tag);
        return instance;
	}
    
    public Instance getInstance(String instanceId) {
    	List<Reservation> reservations = ec2.describeInstances().getReservations();
    	
    	int reservationCount = reservations.size();
    	
    	for(int i = 0; i < reservationCount; i++) {
    		List<Instance> instances = reservations.get(i).getInstances();
    		
    		int instanceCount = instances.size();
    		
    		for(int j = 0; j < instanceCount; j++) {
    			Instance instance = instances.get(j);
    			if (instance.getInstanceId().equals(instanceId)) {
    				return instance;
    			}
    		}
    	}
    	return null;
    }

	public String getInstanceStatus(Instance instance) {
		DescribeInstanceStatusRequest descr = new DescribeInstanceStatusRequest();
		descr.withInstanceIds(instance.getInstanceId());
		
		DescribeInstanceStatusResult describeInstanceResult = ec2.describeInstanceStatus(descr);
		List<InstanceStatus> state = describeInstanceResult.getInstanceStatuses();
		if (state.size() > 0) {
			String status = state.get(0).getSystemStatus().getStatus();
			return status;
		}
		return "";
	}

}

