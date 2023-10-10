package it.unipi.iot.model;

public class TemperatureSample {
	
	private int nodeId;
	private int temperature;
	
	public TemperatureSample() {
	}
	
	public TemperatureSample(int nodeId, int temperature) {
		this.nodeId = nodeId;
		this.temperature = temperature;
	}

	public int getNodeId() {
		return nodeId;
	}

	public void setNodeId(int nodeId) {
		this.nodeId = nodeId;
	}

	public int getTemperature() {
		return temperature;
	}

	public void setTemperature(int temperature) {
		this.temperature = temperature;
	}

}
