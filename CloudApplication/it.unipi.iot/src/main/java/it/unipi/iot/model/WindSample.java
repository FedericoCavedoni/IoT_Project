package it.unipi.iot.model;

public class WindSample {
	
	private int nodeId;
	private int speed;
	
	public WindSample() {
	}
	
	public WindSample(int nodeId, int speed) {
		this.nodeId = nodeId;
		this.speed = speed;
	}

	public int getNodeId() {
		return nodeId;
	}

	public void setNodeId(int nodeId) {
		this.nodeId = nodeId;
	}

	public int getSpeed() {
		return speed;
	}

	public void setSpeed(int speed) {
		this.speed = speed;
	}
	
	

}
