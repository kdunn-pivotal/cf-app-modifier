package io.pivotal;

public class CfAppModifierProperties {

	/*
	 * The Cloud Foundry application to control
	 */
	String targetApplication;

	Integer minInstances = 1;
	
	Integer maxInstances = 3;
	
	String cfAdminUser = "nobody";
	
	String cfAdminPassword = "password";
	
	String cfLoginHost = "https://localhost/login";
	
	String cfApiUri = "https://localhost/api/v2";
	
	public String getTargetApplication() {
		return targetApplication;
	}

	public void setTargetApplication(String targetApplication) {
		this.targetApplication = targetApplication;
	}

	public Integer getMinInstances() {
		return minInstances;
	}

	public void setMinInstances(Integer minInstances) {
		this.minInstances = minInstances;
	}

	public Integer getMaxInstances() {
		return maxInstances;
	}

	public void setMaxInstances(Integer maxInstances) {
		this.maxInstances = maxInstances;
	}

	public String getCfAdminUser() {
		return cfAdminUser;
	}

	public void setCfAdminUser(String cfAdminUser) {
		this.cfAdminUser = cfAdminUser;
	}

	public String getCfAdminPassword() {
		return cfAdminPassword;
	}

	public void setCfAdminPassword(String cfAdminPassword) {
		this.cfAdminPassword = cfAdminPassword;
	}

	public String getCfLoginHost() {
		return cfLoginHost;
	}

	public void setCfLoginHost(String cfLoginHost) {
		this.cfLoginHost = cfLoginHost;
	}

	public String getCfApiUri() {
		return cfApiUri;
	}

	public void setCfApiUri(String cfApiUri) {
		this.cfApiUri = cfApiUri;
	}
	

	
}
