package it.unipi.iot.model;

public class SeismicSample {

	private int nodeId;
	private int frequency;
	
	public SeismicSample() {
	}
	
	public SeismicSample(int nodeId, int frequency) {
		this.nodeId = nodeId;
		this.frequency = frequency;
	}

	public int getNodeId() {
		return nodeId;
	}

	public void setNodeId(int nodeId) {
		this.nodeId = nodeId;
	}

	public int getFrequency() {
		return frequency;
	}

	public void setFrequency(int frequency) {
		this.frequency = frequency;
	}
	
}
