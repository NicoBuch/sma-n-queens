package back;

import java.util.HashSet;
import java.util.Set;

public class Message {
	private int method; // 0 for OK, 1 for NOGOOD, 2 for AddNeighbour, 3 for solutionFound
	private int destinatary;
	private Object[] args;
	private Set<Integer> readers = new HashSet<Integer>();
	
	public Message(int method, int destinatary, Object[] args) {
		this.args = args;
		this.method = method;
		this.destinatary = destinatary;
	}

	public int getDestinatary() {
		return destinatary;
	}

	public int getMethod() {
		return method;
	}

	public Object[] getArgs() {
		return args;
	}
	
	public void read(int agent){
		readers.add(agent);
	}
	
	public boolean isFinishedMessage(int n){
		if (readers.size() == n){
			return true;
		}
		return false;
	}
	
	public boolean canRead(int agent){
		if(readers.contains(agent)){
			return false;
		}
		return true;
	}

}
