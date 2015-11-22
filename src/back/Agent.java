package back;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import front.ChessGUI;

public class Agent implements Runnable {

	private final int row;
	private final int n;
	private int column = -1;
	private ChessGUI gc;
	private Map<Integer, Integer> localView = new HashMap<Integer, Integer>();
	private Queue<Message> blackboard;
	private Map<Integer, Set<Integer>> nogoods = new HashMap<Integer, Set<Integer>>(); // Key: Sender of the nogood, value: invalid column
	

public Agent(int row, ChessGUI gc, int n, Queue<Message> blackboard) {
		this.row = row;
		this.gc = gc;
		this.n = n;
		this.blackboard = blackboard;
	}

	public int getRow() {
		return row;
	}

	public void setColumn(int column) {
		this.column = column;
	}

	public int getColumn() {
		return column;
	}

	public boolean isConsistent(int column) {
		for(Set<Integer> values : nogoods.values()){
			if(values.contains(column)){
				return false;
			}
		}
		for (Integer otherAgent : localView.keySet()) {
			if (otherAgent != row) {
				int otherColumn = localView.get(otherAgent);
				if (otherColumn == column)
					return false;
				if (Math.abs(otherAgent - row) ==  Math.abs(otherColumn - column))
					return false;
			}
		}
		return true;
	}
	
	public void nogood(int row, int column, int sender){
		if(row == this.row){
			System.out.println("Im agent "+ row + "and recieved nogood at column " + column + "from " + sender);
			
			if(this.column == column){
				gc.removeQueen(row, this.column);
				this.column = -1;
				Set<Integer> values = nogoods.get(sender);
				if(values == null){
					values = new HashSet<Integer>();
					nogoods.put(sender, values);
				}
				values.add(column);
				checkLocalView();
			}

		}
		else{
			if(localView.containsKey(row)){
				localView.remove(row);			
			}
		}
	}


	public void ok(Integer agent, int agentColumn) {
		if(agent == row){
			return;
		}
		if(column != -1){
//			localView.put(agent, agentColumn);
//			if(!isConsistent(column))
//				nogood(row, column, agent);
			return;
		}
		System.out.println("Im agent " + row + " and received OK  from agent: " + agent + ", column: " + agentColumn);
		localView.put(agent, agentColumn);	
		if(nogoods.containsKey(agent)){
			nogoods.remove(agent);
		}
//		checkLocalView();
	}
	
	private void checkLocalView() {
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (column == -1) {
			for (int i = 0; i < n; i++) {
				if(isConsistent(i)){
					column = i;
					gc.putQueen(row, column);				
					Integer[] args = { row, column };
					synchronized (blackboard) {
						blackboard.add(new Message(0, args));
					}
					System.out.println("My localview is : " + localView);
					System.out.println("Im agent " + row + " sending ok in column " + i);
					return;
				}
			}
			if(localView.isEmpty()){
				@SuppressWarnings("unused")
				int a = 0;
			}
			Integer nogoodAgent = getLowestPriorityAgentInLocalView(localView);
			Object[] args = { nogoodAgent, localView.get(nogoodAgent), row };
			System.out.println("Im Agent " + row + " sending nogood to " + nogoodAgent);
			synchronized (blackboard) {
				blackboard.add(new Message(1, args));
			}
		}
	}

	@Override
	public void run() {
//		while(true){
			synchronized (blackboard) {
//				for(int i = 0; i< blackboard.size(); i++){
				if(!blackboard.isEmpty()){
					Message m = blackboard.peek();
					if(m.canRead(row)){
						synchronized (m) {		
							m.read(row);					
						}
						if(m.isFinishedMessage(n)){
							blackboard.remove();
						}
						if(m.getMethod() == 0){
							ok((Integer)m.getArgs()[0], (Integer)m.getArgs()[1]);
						}
						else if(m.getMethod() == 1){
							nogood((Integer)m.getArgs()[0], (Integer)m.getArgs()[1], (Integer)m.getArgs()[2]);	
						}
					}
				}
			}
			if(blackboard.isEmpty())
				checkLocalView();
//		}
	}
	
	private Integer getLowestPriorityAgentInLocalView(Map<Integer, Integer> localView){
//		return row -1;
		Integer maxAgent = -1;
			for(Integer agent : localView.keySet()){
				if(maxAgent == -1 || (agent != row && agent > maxAgent)){
					maxAgent = agent;
				}
		}
		return maxAgent;
	}
	
	@Override
	public int hashCode() {
		return row;
	}
	
	@Override
	public boolean equals(Object obj) {
		return row == ((Agent)obj).getRow();
	}

}
