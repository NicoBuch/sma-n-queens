package back;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;

import front.ChessGUI;

public class WeakAgent implements Runnable{
	private int n;
	private int priority = 0;
	private int column = -1;
	private int row;
	private ChessGUI cg;
	private Set<Integer> links = new HashSet<Integer>();
	private Map<Integer, Integer> agentView = new HashMap<Integer, Integer>();
	private Set<Set<Entry<Integer, Integer>>> nogoods = new HashSet<Set<Entry<Integer, Integer>>>();
	private List<Message> blackboard;
	
	private int[] priorities;
	
	public WeakAgent(int n, ChessGUI cg, int row, List<Message> blackboard) {
		this.n = n;
		priorities = new int[n];
		this.cg = cg;
		this.row = row;
		for(int i = 0; i < n; i++){
			if(row != i)
				links.add(i);
		}
		this.blackboard = blackboard;
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	public void run(){
		Random rand = new Random();
		if(row == n-1)
			column = n-1;
		else
			column = rand.nextInt(((n-1) - 0) + 1) + 0;
		sendOk(column);
		boolean end = false;
		while (!end) {
			if (isSolved()) {
				System.out.println("Agent " + row + " realized it is solved");
				agentView.put(row, column);
				Object[] args2 = { agentView };
				broadcast(3, args2);
				end = true;
			} else {
				Message message = null;
				synchronized (blackboard) {
					for (int i = 0; i < blackboard.size(); i++) {
						Message m = blackboard.get(i);
						if (m.getDestinatary() == row) {
							message = m;
							blackboard.remove(i);
							break;
						}
					}
				}
				if (message != null) {
					switch (message.getMethod()) {
					case 0:
						ok((Integer) message.getArgs()[0], (Integer) message.getArgs()[1], (Integer) message.getArgs()[2]);
						break;
					case 1:
						nogood((Integer) message.getArgs()[0],
							   (Set<Entry<Integer, Integer>>) message.getArgs()[1]);
						break;
					case 2:
						addLink((Integer) message.getArgs()[0]);
						break;
					case 3:
						buildSolution((Map<Integer, Integer>) message.getArgs()[0]);
						 System.out.println("Im agent " + row + " and Im out");
						end = true;
						break;
					}
				}
			}
		}
	}
	
	public boolean isSolved() {
		if (agentView.keySet().size() != n - 1)
			return false;
		Set<Entry<Integer, Integer>> totalView = totalAgentView(column);

		for (Entry<Integer, Integer> anAgent : totalView) {
			for (Entry<Integer, Integer> otherAgent : totalView) {
				if (otherAgent.getKey() != anAgent.getKey()) {
					if (anAgent.getValue() == otherAgent.getValue())
						return false;
					if (Math.abs(otherAgent.getKey() - anAgent.getKey()) == Math
							.abs(anAgent.getValue() - otherAgent.getValue()))
						return false;
				}
			}
		}
		return true;
	}
	
	
	public Set<Entry<Integer, Integer>> totalAgentView(int newColumn) {
		Map<Integer, Integer> map = new HashMap<Integer, Integer>();
		map.putAll(agentView);
		map.put(row, newColumn);
		return map.entrySet();
	}
	
	
	public int[] randomDomain() {
		int[] domain = new int[n];
		for (int i = 0; i < n; i++) {
			domain[i] = i;
		}
		Random rnd = ThreadLocalRandom.current();
		for (int i = domain.length - 1; i > 0; i--) {
			int index = rnd.nextInt(i + 1);
			// Simple swap
			int a = domain[index];
			domain[index] = domain[i];
			domain[i] = a;
		}
		return domain;
	}
	
	public void broadcast(int method, Object[] args) {
		for (int i = 0; i < n; i++) {
			if (i != row) {
				synchronized (blackboard) {
					blackboard.add(new Message(method, i, args));
				}
			}
		}
	}
	
	public boolean isSubset(Set<Entry<Integer, Integer>> set,
			Set<Entry<Integer, Integer>> nogood) {
		for (Entry<Integer, Integer> element : nogood) {
			if (!set.contains(element)) {
				return false;
			}
		}
		return true;
	}
	
	public Set<Entry<Integer, Integer>> cloneEntrySet(
			Set<Entry<Integer, Integer>> set) {
		Map<Integer, Integer> clone = new HashMap<Integer, Integer>();
		for (Entry<Integer, Integer> entry : set) {
			clone.put(entry.getKey(), entry.getValue());
		}
		return clone.entrySet();
	}
	
	public void addLink(int link) {
		links.add(link);
	}
	
	public void ok(int otherRow, int otherColumn, int priority) {
		System.out.println("Agent " + row + " receiving ok? " + otherRow + ", " + otherColumn + " with priority " + priority);
		agentView.put(otherRow, otherColumn);
		priorities[otherRow] = priority;
		checkLocalView();
	}
	
	
	public void nogood(int sender, Set<Entry<Integer, Integer>> nogood) {
		System.out.println("Agent " + row + " receiving nogood " + nogood + " from " + sender);
		nogoods.add(nogood);
//		for (Entry<Integer, Integer> entry : nogood) {
//			if (entry.getKey() != row && !links.contains(entry.getKey())) {
//				Object[] args = { row };
//				synchronized (blackboard) {
//					blackboard.add(new Message(2, entry.getKey(), args));
//				}
//				links.add(entry.getKey());
//				agentView.put(entry.getKey(), entry.getValue());
//			}
//		}
//		checkLocalView();
	}
	
	public void buildSolution(Map<Integer, Integer> solution) {
		if (column != -1)
			cg.removeQueen(row, column);
		cg.putQueen(row, solution.get(row));
		return;
	}
	
	public void checkLocalView() {
//		if(agentView.size() < n-1){
//			return;
//		}
//		try {
//			Thread.sleep(1000);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
		if (column == -1 || constraintViolations(column, true) != 0) {
			for (int i : randomDomain()) {
				if (constraintViolations(i, true) == 0) {
					selectBaseValue();
					return;
				}
			}
			Set<Entry<Integer, Integer>> nogood = newNogood();
			if (nogood.isEmpty()) {
				 System.out.println("NO SOLUTION!!!!!!");
				 // TODO: Terminate the algorithm
			}
			if(!nogoods.add(nogood))
				return;
			sendNogood(nogood);
			selectBaseValue();
		}

	}
	
	public int constraintViolations(int newColumn, boolean onlyHigherPriority){
		int violations = 0;
		Set<Entry<Integer, Integer>> totalView = totalAgentView(newColumn);
		for (Set<Entry<Integer, Integer>> nogood : nogoods) {
			if (isSubset(totalView, nogood)) {
				violations++;
			}
		}

		for (Entry<Integer, Integer> otherAgent : agentView.entrySet()) {
			boolean analize = otherAgent.getKey() != row;
			if(onlyHigherPriority){
				if(priorities[otherAgent.getKey()] != priority)
					analize = priorities[otherAgent.getKey()] > priority;
				else
					analize = otherAgent.getKey() > row;				
			}
			if (analize) {
				if (newColumn == otherAgent.getValue())
					violations++;
				if (Math.abs(otherAgent.getKey() - row) == Math.abs(newColumn
						- otherAgent.getValue()))
					violations++;
			}
		}

		return violations;

	}
	
	public void selectBaseValue(){
		int minViolationsColumn = -1;
		int minViolations = -1;
		for(int i = 0; i < n; i++){
			int violations = constraintViolations(i, false);
			if(minViolations == -1 || minViolations > violations){
				minViolationsColumn = i;
				minViolations = violations;
			}
		}
		sendOk(minViolationsColumn);
	}
	
	
	public void sendOk(int newColumn){
		if (column != -1)
			cg.removeQueen(row, column);
		column = newColumn;
		cg.putQueen(row, column);
		Object[] args = { row, column, priority };
		for (Integer link : links) {
			boolean send;
			if(priorities[link] != priority)
				send = priorities[link] < priority;
			else
				send = link < row;	
			if(send){
				synchronized (blackboard) {
//					System.out.println("Agent " + row + " sending ok? at " + column + " to " + link);
					blackboard.add(new Message(0, link, args));
				}
			}
		}	
	}
	
	
	
	public Set<Entry<Integer, Integer>> newNogood() {
		Set<Set<Entry<Integer, Integer>>> aux = new HashSet<Set<Entry<Integer, Integer>>>();
		aux.addAll(nogoodsWithoutMe());
		Set<Entry<Integer, Integer>> ans = new HashSet<Entry<Integer, Integer>>();
		ans.addAll(cloneEntrySet(agentView.entrySet()));
		for (Set<Entry<Integer, Integer>> nogood : aux) {
			boolean needsToAnalizeNogood = false; // Only if nogood contains
													// entry with current
													// row
			for (Entry<Integer, Integer> entry : nogood) {
				if (entry.getKey() == row)
					needsToAnalizeNogood = true;
			}
			if (needsToAnalizeNogood) {
				for (Entry<Integer, Integer> entry : nogood) {
					if (entry.getKey() != row) {
						boolean needToRemove = false; // Only if has two
														// different values
														// for same key
						boolean needToAdd = true; // Only if doesn't have
													// key.
						for (Entry<Integer, Integer> ansEntry : ans) {
							if (entry.getKey() == ansEntry.getKey()) {
								needToAdd = false;
								if (entry.getValue() != ansEntry.getValue())
									needToRemove = true;
							}
						}
						if (needToRemove)
							ans.remove(entry);
						if (needToAdd)
							ans.add(entry);
					}

				}

			}
		}
		return ans;
	}
	
	public Set<Set<Entry<Integer, Integer>>> nogoodsWithoutMe() {
		Set<Set<Entry<Integer, Integer>>> ans = new HashSet<Set<Entry<Integer, Integer>>>();
		for (Set<Entry<Integer, Integer>> nogood : nogoods) {
			Set<Entry<Integer, Integer>> newNogood = cloneEntrySet(nogood);
			for (Entry<Integer, Integer> entry : nogood) {
				if (entry.getKey() == row) {
					newNogood.remove(entry);
				}
			}
			ans.add(newNogood);
		}
		return ans;
	}
	
	public void sendNogood(Set<Entry<Integer, Integer>> nogood){
		int priorityMax = -1;
		for(int p : priorities){
			if(p > priorityMax)
				priorityMax = p;
		}
		
		
		Object[] args = { row, nogood };
		for (Entry<Integer, Integer> entry : nogood) {
			synchronized (blackboard) {
				blackboard.add(new Message(1, entry.getKey(), args));
			}
		}		
		priority = priorityMax + 1;
	}
	
	
	public int getLowestPriorityAgentInNogood(
			Set<Entry<Integer, Integer>> nogood) {
		int aux = -1;
		int minAgent = -1;
		for (Entry<Integer, Integer> agent : nogood) {
			boolean minFound = priorities[agent.getKey()] == aux;
			if(minFound)
				minFound = agent.getKey() < minAgent;
			else
				minFound = priorities[agent.getKey()] < aux;
			if (aux == -1 || minFound) {
				aux = priorities[agent.getKey()];
				minAgent = agent.getKey();
			}
		}
		return minAgent;
	}

}
