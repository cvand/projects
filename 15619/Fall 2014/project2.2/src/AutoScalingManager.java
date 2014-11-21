import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.CreateLaunchConfigurationRequest;
import com.amazonaws.services.autoscaling.model.DeleteAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.DeleteLaunchConfigurationRequest;
import com.amazonaws.services.autoscaling.model.InstanceMonitoring;
import com.amazonaws.services.autoscaling.model.PutScalingPolicyRequest;
import com.amazonaws.services.autoscaling.model.PutScalingPolicyResult;
import com.amazonaws.services.autoscaling.model.Tag;
import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupRequest;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.PutMetricAlarmRequest;


public class AutoScalingManager {
	private AmazonAutoScalingClient asg;
	private String asgName;
	private AWSCredentials credentials;
	
	public AutoScalingManager() {
		try {
			credentials = new ProfileCredentialsProvider().getCredentials();
		} catch (Exception e) {
			throw new AmazonClientException("Cannot load the credentials from the credential profiles file. "
					+ "Please make sure that your credentials file is at the correct "
					+ "location (~/.aws/credentials), and is in valid format.", e);
		}
		asg = new AmazonAutoScalingClient(credentials);
	}
	
	public CreateAutoScalingGroupRequest createASG(String asgName, String elbName, int minSize, int maxSize, int desiredCapacity, String availabilityZone1, String availabilityZone2, String availabilityZone3) {
		this.asgName = asgName;
		DeleteAutoScalingGroupRequest deleteIfExists = new DeleteAutoScalingGroupRequest();
		deleteIfExists.setAutoScalingGroupName(this.asgName);
		try {
			asg.deleteAutoScalingGroup(deleteIfExists);
			Thread.sleep(10000);
		} catch (Exception e) {
			System.out.println("Failed to delete, because the ASG doesn't exist.");
		}
		
		String launchConfigName = "LaunchConfig";
		asg.createLaunchConfiguration(createLaunchConfiguration(launchConfigName, "ami-ec14ba84", "m3.medium", "sg-294d794c"));
		CreateAutoScalingGroupRequest req = new CreateAutoScalingGroupRequest();
		req.setAutoScalingGroupName(this.asgName);
		linkToELB(req, elbName);
		
		req.setDesiredCapacity(desiredCapacity);
		req.setMinSize(minSize);
		req.setMaxSize(maxSize);
		req.withAvailabilityZones(availabilityZone1, availabilityZone2, availabilityZone3);
		
		req.setVPCZoneIdentifier("subnet-1267773a,subnet-8744b5de,subnet-c7bb6db0");
		
		Tag tag = new Tag();
		tag.setKey("Project");
		tag.setValue("2.2");
		req.withTags(tag);
		req.setLaunchConfigurationName(launchConfigName);
		asg.createAutoScalingGroup(req);
		
		PutScalingPolicyRequest policy = createScalingPolicy(asgName, "Increase_Group_Size", "ChangeInCapacity", 1, 10);
		PutScalingPolicyResult result = null;
		try {
			result = asg.putScalingPolicy(policy);
		} catch (AmazonClientException e) {
			e.printStackTrace();
		}
		createActionFromAlarm(result, "ScaleUp");
		
		policy = createScalingPolicy(asgName, "Decrease_Group_Size", "ChangeInCapacity", -1, 15);
		result = asg.putScalingPolicy(policy);
		createActionFromAlarm(result, "ScaleDown");
		System.out.println("created auto scaling group");
		return req;
	}
	
	private CreateLaunchConfigurationRequest createLaunchConfiguration(String name, String ami, String type, String securityGroup) {
		DeleteLaunchConfigurationRequest deleteIfExists = new DeleteLaunchConfigurationRequest();
		deleteIfExists.setLaunchConfigurationName(name);
		try {
			asg.deleteLaunchConfiguration(deleteIfExists);
		} catch (Exception e) {
			e.printStackTrace();
		}
		CreateLaunchConfigurationRequest request = new CreateLaunchConfigurationRequest();
		request.setLaunchConfigurationName(name);
		request.setImageId(ami);
		request.setInstanceType(type);
		request.getInstanceMonitoring();
		InstanceMonitoring monitoring = new InstanceMonitoring();
		monitoring.setEnabled(true);
		request.setInstanceMonitoring(monitoring);
		
		request.withSecurityGroups(securityGroup);
		request.setKeyName("15619demo");
		return request;
	}
	
	private void linkToELB(CreateAutoScalingGroupRequest req, String elbName) {
		req.withLoadBalancerNames(elbName);
		req.setHealthCheckType("EC2");
		req.setHealthCheckGracePeriod(300);
	}
	
	private void createActionFromAlarm(PutScalingPolicyResult policy, String scaleType) {
		AmazonCloudWatchClient cw = new AmazonCloudWatchClient(credentials);
		if (scaleType.equals("ScaleUp")) {
			cw.putMetricAlarm(createCloudWatchAlarm("CPU-Utilization-80", "CPUUtilization", "Average", "GreaterThanOrEqualToThreshold", 80.0, 300, policy.getPolicyARN()));
		} else {
			cw.putMetricAlarm(createCloudWatchAlarm("CPU-Utilization-20", "CPUUtilization", "Average", "LessThanThreshold", 20.0, 300, policy.getPolicyARN()));
		}
	}
	
	private PutMetricAlarmRequest createCloudWatchAlarm(String alarmName, String metric, String statistic, String comparisonOp, double threshold, int period, String policyARN) {
		PutMetricAlarmRequest alarm = new PutMetricAlarmRequest();
		alarm.setAlarmName(alarmName);
		alarm.setComparisonOperator(comparisonOp);
		alarm.setThreshold(threshold);
		alarm.setPeriod(period);
		alarm.setMetricName(metric);
		alarm.withStatistic(statistic);
		Dimension dimension = new Dimension();
		dimension.setName("AutoScalingGroupName");
		dimension.setValue(asgName);
		alarm.withDimensions(dimension);
		alarm.withAlarmActions(policyARN);
		alarm.setEvaluationPeriods(1);
		alarm.setNamespace("AWS/EC2");
		return alarm;
	}
	
	private PutScalingPolicyRequest createScalingPolicy(String groupName, String policyName, String adjustmentType, int adjustment, int cooldown) {
		PutScalingPolicyRequest policy = new PutScalingPolicyRequest();
		policy.setAutoScalingGroupName(groupName);
		policy.setPolicyName(policyName);
		policy.setCooldown(cooldown);
		policy.setAdjustmentType(adjustmentType);
		policy.setScalingAdjustment(adjustment);
		return policy;
	}
	
	public void deleteASG() {
		UpdateAutoScalingGroupRequest req = new UpdateAutoScalingGroupRequest();
		req.setDesiredCapacity(0);
		req.setMinSize(0);
		req.setMaxSize(0);
		req.setAutoScalingGroupName(asgName);
		asg.updateAutoScalingGroup(req);
		
		DeleteAutoScalingGroupRequest delete = new DeleteAutoScalingGroupRequest();
		asg.deleteAutoScalingGroup(delete);
	}

}
