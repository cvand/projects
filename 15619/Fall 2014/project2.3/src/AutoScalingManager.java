import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.BlockDeviceMapping;
import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.CreateLaunchConfigurationRequest;
import com.amazonaws.services.autoscaling.model.DeleteAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.DeleteLaunchConfigurationRequest;
import com.amazonaws.services.autoscaling.model.EnableMetricsCollectionRequest;
import com.amazonaws.services.autoscaling.model.InstanceMonitoring;
import com.amazonaws.services.autoscaling.model.PutNotificationConfigurationRequest;
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
	
	public CreateAutoScalingGroupRequest createASG(String asgName, String elbName, int minSize, int maxSize, int desiredCapacity, String availabilityZone1, String availabilityZone2, String availabilityZone3, String topicARN) {
		this.asgName = asgName;
		try {
			deleteASG();
			Thread.sleep(10000);
		} catch (Exception e) {
			System.out.println("Failed to delete, because the ASG doesn't exist.");
		}
		
		String launchConfigName = "LaunchConfig";
		asg.createLaunchConfiguration(createLaunchConfiguration(launchConfigName, "ami-3c8f3a54", "m3.large", "sg-294d794c"));
		CreateAutoScalingGroupRequest req = new CreateAutoScalingGroupRequest();
		req.setAutoScalingGroupName(this.asgName);
		linkToELB(req, elbName);
		
		req.setDesiredCapacity(desiredCapacity);
		req.setMinSize(minSize);
		req.setMaxSize(maxSize);
		req.withAvailabilityZones(availabilityZone1, availabilityZone2, availabilityZone3);
		req.setVPCZoneIdentifier("subnet-1267773a,subnet-8744b5de,subnet-c7bb6db0");
		req.setDefaultCooldown(120);
		
		Tag tag = new Tag();
		tag.setKey(MSBMain.tagKey);
		tag.setValue(MSBMain.tagValue);
		req.withTags(tag);
		req.setLaunchConfigurationName(launchConfigName);
		asg.createAutoScalingGroup(req);
		
		EnableMetricsCollectionRequest metrics = new EnableMetricsCollectionRequest();
		metrics.setAutoScalingGroupName(asgName);
		metrics.setGranularity("1Minute");
		metrics.withMetrics("GroupInServiceInstances", "GroupMinSize", "GroupDesiredCapacity", "GroupTotalInstances", "GroupTerminatingInstances", "GroupMaxSize", "GroupPendingInstances");
		asg.enableMetricsCollection(metrics);
		
		PutScalingPolicyRequest policy = createScalingPolicy(asgName, "Increase_Group_Size", "ChangeInCapacity", 2, 10);
		PutScalingPolicyResult result = null;
		result = asg.putScalingPolicy(policy);
		createActionFromAlarm(result, "ScaleUp");
		
		policy = createScalingPolicy(asgName, "Decrease_Group_Size", "ChangeInCapacity", -1, 15);
		result = asg.putScalingPolicy(policy);
		createActionFromAlarm(result, "ScaleDown");
		
		PutNotificationConfigurationRequest notification = new PutNotificationConfigurationRequest();
		notification.setAutoScalingGroupName(this.asgName);
		notification.setTopicARN(topicARN);
		notification.withNotificationTypes("autoscaling:EC2_INSTANCE_LAUNCH", "autoscaling:EC2_INSTANCE_TERMINATE");
		asg.putNotificationConfiguration(notification);
		return req;
	}
	
	private CreateLaunchConfigurationRequest createLaunchConfiguration(String name, String ami, String type, String securityGroup) {
		DeleteLaunchConfigurationRequest deleteIfExists = new DeleteLaunchConfigurationRequest();
		deleteIfExists.setLaunchConfigurationName(name);
		try {
			asg.deleteLaunchConfiguration(deleteIfExists);
		} catch (Exception e) {
			System.out.println("Launch Configuration Delete Failed");
		}
		CreateLaunchConfigurationRequest request = new CreateLaunchConfigurationRequest();
		request.setLaunchConfigurationName(name);
		request.setImageId(ami);
		request.setInstanceType(type);
		InstanceMonitoring monitoring = new InstanceMonitoring();
		monitoring.setEnabled(true);
		request.setInstanceMonitoring(monitoring);
		
		request.withSecurityGroups(securityGroup);
		request.setKeyName("15619demo");
		
		BlockDeviceMapping blockDev = new BlockDeviceMapping();
		blockDev.setVirtualName("ebs");
		blockDev.setDeviceName("/dev/sda1");
//		request.withBlockDeviceMappings(blockDev);
		
		return request;
	}
	
	private void linkToELB(CreateAutoScalingGroupRequest req, String elbName) {
		req.withLoadBalancerNames(elbName);
		req.setHealthCheckType("ELB");
		req.setHealthCheckGracePeriod(120);
	}
	
	private void createActionFromAlarm(PutScalingPolicyResult policy, String scaleType) {
		AmazonCloudWatchClient cw = new AmazonCloudWatchClient(credentials);
		if (scaleType.equals("ScaleUp")) {
			cw.putMetricAlarm(createCloudWatchAlarm("CPU-Utilization-85", "CPUUtilization", "Average", "GreaterThanOrEqualToThreshold", 85.0, 120, policy.getPolicyARN()));
		} else {
			cw.putMetricAlarm(createCloudWatchAlarm("CPU-Utilization-40", "CPUUtilization", "Average", "LessThanThreshold", 40.0, 120, policy.getPolicyARN()));
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
	
	public void deleteASG() throws Exception {
		UpdateAutoScalingGroupRequest req = new UpdateAutoScalingGroupRequest();
		req.setDesiredCapacity(0);
		req.setMinSize(0);
		req.setMaxSize(0);
		req.setAutoScalingGroupName(asgName);
		asg.updateAutoScalingGroup(req);
		
		DeleteAutoScalingGroupRequest delete = new DeleteAutoScalingGroupRequest();
		delete.setAutoScalingGroupName(asgName);
		asg.deleteAutoScalingGroup(delete);
	}

}
