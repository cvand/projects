import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.model.ApplySecurityGroupsToLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.ConfigureHealthCheckRequest;
import com.amazonaws.services.elasticloadbalancing.model.ConnectionDraining;
import com.amazonaws.services.elasticloadbalancing.model.CreateLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.CreateLoadBalancerResult;
import com.amazonaws.services.elasticloadbalancing.model.DeleteLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.DescribeInstanceHealthRequest;
import com.amazonaws.services.elasticloadbalancing.model.DescribeInstanceHealthResult;
import com.amazonaws.services.elasticloadbalancing.model.HealthCheck;
import com.amazonaws.services.elasticloadbalancing.model.InstanceState;
import com.amazonaws.services.elasticloadbalancing.model.Listener;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerAttributes;
import com.amazonaws.services.elasticloadbalancing.model.ModifyLoadBalancerAttributesRequest;
import com.amazonaws.services.elasticloadbalancing.model.RegisterInstancesWithLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.RegisterInstancesWithLoadBalancerResult;
import com.amazonaws.services.elasticloadbalancing.model.Tag;

public class ElasticLoadBalancerManager {
	private AmazonElasticLoadBalancingClient	elb;
	private String								elbName;

	public ElasticLoadBalancerManager() {
		AWSCredentials credentials = null;
		try {
			credentials = new ProfileCredentialsProvider().getCredentials();
		} catch (Exception e) {
			throw new AmazonClientException("Cannot load the credentials from the credential profiles file. "
					+ "Please make sure that your credentials file is at the correct "
					+ "location (~/.aws/credentials), and is in valid format.", e);
		}
		elb = new AmazonElasticLoadBalancingClient(credentials);
	}

	public CreateLoadBalancerResult createELB(String elbName, String availabilityZone1, String availabilityZone2, String availabilityZone3, String securityGroup, String tagKey, String tagValue) {
		this.elbName = elbName;
		CreateLoadBalancerRequest lbRequest = new CreateLoadBalancerRequest();
		lbRequest.setLoadBalancerName(this.elbName);

		List<Listener> listeners = new ArrayList<Listener>(1);
		listeners.add(new Listener("HTTP", 80, 80));

		lbRequest.withAvailabilityZones(availabilityZone1, availabilityZone2, availabilityZone3);
		lbRequest.setListeners(listeners);

		Tag tag = new Tag();
		tag.setKey(tagKey);
		tag.setValue(tagValue);
		List<Tag> tags = new ArrayList<Tag>();
		tags.add(tag);
		lbRequest.setTags(tags);

		ConfigureHealthCheckRequest health = new ConfigureHealthCheckRequest();
		health.setLoadBalancerName(this.elbName);
		HealthCheck healthCheck = new HealthCheck("HTTP:80/heartbeat?username=cvandera", 30, 5, 2, 10);
		health.setHealthCheck(healthCheck);

		ApplySecurityGroupsToLoadBalancerRequest secGroups = new ApplySecurityGroupsToLoadBalancerRequest();
		secGroups.withSecurityGroups(securityGroup);
		secGroups.setLoadBalancerName(this.elbName);

		ModifyLoadBalancerAttributesRequest attr = new ModifyLoadBalancerAttributesRequest();
		attr.setLoadBalancerName(this.elbName);
		LoadBalancerAttributes attrs = new LoadBalancerAttributes();
		ConnectionDraining cd = new ConnectionDraining();
		cd.setEnabled(false);
		attrs.setConnectionDraining(cd);
		attr.setLoadBalancerAttributes(attrs);

		try {
			DeleteLoadBalancerRequest deleteIfExists = new DeleteLoadBalancerRequest();
			deleteIfExists.setLoadBalancerName(this.elbName);
			elb.deleteLoadBalancer(deleteIfExists);
		} catch (Exception e) {
			e.printStackTrace();
		}
		CreateLoadBalancerResult lbResult = elb.createLoadBalancer(lbRequest);
		elb.applySecurityGroupsToLoadBalancer(secGroups);
		elb.modifyLoadBalancerAttributes(attr);
		elb.configureHealthCheck(health);
		return lbResult;
	}

	public RegisterInstancesWithLoadBalancerResult registerIfInstances(EC2Manager ec2Manager) {
		List<com.amazonaws.services.elasticloadbalancing.model.Instance> instanceId = getDataCenterInstances(ec2Manager);
		RegisterInstancesWithLoadBalancerRequest register = new RegisterInstancesWithLoadBalancerRequest();
		register.setLoadBalancerName(elbName);
		register.setInstances(instanceId);
		if ( instanceId.size() > 0 ) { return elb.registerInstancesWithLoadBalancer(register); }
		return null;
	}

	private List<com.amazonaws.services.elasticloadbalancing.model.Instance> getDataCenterInstances(EC2Manager ec2Manager) {
		List<Instance> instances = getInstances(ec2Manager);

		// get instance id's
		String id;
		List<com.amazonaws.services.elasticloadbalancing.model.Instance> instanceId = new ArrayList<com.amazonaws.services.elasticloadbalancing.model.Instance>();
		List<String> instanceIdString = new ArrayList<String>();
		Iterator<Instance> iterator = instances.iterator();
		while (iterator.hasNext()) {
			Instance inst = iterator.next();
			id = inst.getInstanceId();
			if ( inst.getImageId().equals("cmu_cc_p23_datacenter_vm (ami-3c8f3a54)") ) {
				instanceId.add(new com.amazonaws.services.elasticloadbalancing.model.Instance(id));
				instanceIdString.add(id);
			}
		}
		return instanceId;
	}

	private List<Instance> getInstances(EC2Manager ec2Manager) {
		DescribeInstancesResult describeInstancesRequest = ec2Manager.getEc2().describeInstances();
		List<Reservation> reservations = describeInstancesRequest.getReservations();
		List<Instance> instances = new ArrayList<Instance>();

		for (Reservation reservation : reservations) {
			instances.addAll(reservation.getInstances());
		}
		return instances;
	}

	public List<InstanceState> getInstanceStates() {
		DescribeInstanceHealthRequest request = new DescribeInstanceHealthRequest();
		request.setLoadBalancerName(elbName);
		DescribeInstanceHealthResult result = elb.describeInstanceHealth(request);
		return result.getInstanceStates();
	}
}
