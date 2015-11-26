package back;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import front.ChessGUI;

public class Agent2 implements Runnable {
	private int n;
	private int column = -1;
	private int row;
	private ChessGUI cg;
	private Set<Integer> links = new HashSet<Integer>();
	private Map<Integer, Integer> agentView = new HashMap<Integer, Integer>();
	private Set<Set<Entry<Integer, Integer>>> nogoods = new HashSet<Set<Entry<Integer, Integer>>>();
	private List<Message> blackboard;

	public Agent2(int n, ChessGUI cg, int row, List<Message> blackboard) {
		this.n = n;
		this.cg = cg;
		this.row = row;
		for (int i = 0; i < n; i++) {
			if (i < row) {
				links.add(i);
			}
		}
		this.blackboard = blackboard;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void run() {
	}
	@SuppressWarnings("unchecked")
	public boolean runSync(){
		boolean end = false;
//		while (!end) {
			if (isSolved()) {
//				 System.out.println("Agent " + row + " realized it is solved");
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
						ok((Integer) message.getArgs()[0],
								(Integer) message.getArgs()[1]);
						break;
					case 1:
						nogood((Integer) message.getArgs()[0],
								(Set<Entry<Integer, Integer>>) message
										.getArgs()[1]);
						break;
					case 2:
						addLink((Integer) message.getArgs()[0]);
						break;
					case 3:
						buildSolution((Map<Integer, Integer>) message.getArgs()[0]);
//						 System.out.println("Im agent " + row + " and Im out");
						end = true;
						break;
					case 4:
						broadcast(4, new Object[1]);
						end = true;
						break;
					}
				}
			}
			return end;
//		}
	}

	public void buildSolution(Map<Integer, Integer> solution) {
		if (column != -1)
			cg.removeQueen(row, column, true);
		cg.putQueen(row, solution.get(row), agentView, nogoods);
		return;
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

	public void ok(int otherRow, int otherColumn) {	
		agentView.put(otherRow, otherColumn);
		checkLocalView();
	}

	public void nogood(int sender, Set<Entry<Integer, Integer>> nogood) {
//		System.out.println("Agent " + row + " receiving nogood " + nogood);
		nogoods.add(nogood);
		checkLocalView();
	}

	public void addLink(int link) {
		links.add(link);
	}

	public void checkLocalView() {
//		try {
//			Thread.sleep(350);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
		cg.returnOriginalColors();
		if(column == -1){
			System.out.println("Es turno del agente de la fila " + (n - row) + " que todavia no tiene columna asignada");
		}
		else{
			System.out.println("Es turno del agente de la fila " + (n - row) + " que está en la columna " + (column+1));
		}
		System.out.println("Los nogoods de este agente son: " + nogoods);
		cg.cleanBoard(n);
		
		for(Entry<Integer, Integer> entry : agentView.entrySet()){
			cg.putQueen(entry.getKey(), entry.getValue());
		}
		System.out.println();
		System.out.println();
		if(column != -1)
			cg.putQueen(row, column, agentView, nogoods);
		cg.paintforbiddenDomain(row, forbiddenDomain());
		if (column == -1 || !isConsistent(column)) {
			for (int i : randomDomain()) {
				if (isConsistent(i)) {
					if (column != -1)
						cg.removeQueen(row, column, true);
					column = i;
					cg.putQueen(row, column, agentView, nogoods);
					Object[] args = { row, column };
					for (Integer link : links) {
						synchronized (blackboard) {
//							 System.out.println("Agent " + row
//									+ " sending ok? at " + column + " to "
//									+ link);
							blackboard.add(new Message(0, link, args));
						}
					}
					return;
				}
			}
			Set<Entry<Integer, Integer>> nogood = newNogood();
			if (nogood.isEmpty()) {
				 System.out.println("NO SOLUTION!!!!!!");
				 broadcast(4, new Object[1]);
			}
			Object[] args = { row, nogood };
			int minAgent = getLowestPriorityAgentInNogood(nogood);
			 System.out.println("Agente de la fila " + row + " enviando nogood " + nogood
					+ " al de la fila " + minAgent);
			synchronized (blackboard) {
				blackboard.add(new Message(1, minAgent, args));
			}
		}

	}

	public Set<Entry<Integer, Integer>> cloneEntrySet(
			Set<Entry<Integer, Integer>> set) {
		Map<Integer, Integer> clone = new HashMap<Integer, Integer>();
		for (Entry<Integer, Integer> entry : set) {
			clone.put(entry.getKey(), entry.getValue());
		}
		return clone.entrySet();
	}

	public boolean isConsistent(int newColumn) {
		Set<Entry<Integer, Integer>> totalView = totalAgentView(newColumn);
		for (Set<Entry<Integer, Integer>> nogood : nogoods) {
			if (isSubset(totalView, nogood)) {
				return false;
			}
		}

		for (Entry<Integer, Integer> otherAgent : agentView.entrySet()) {
			if (otherAgent.getKey() != row) {
				if (newColumn == otherAgent.getValue())
					return false;
				if (Math.abs(otherAgent.getKey() - row) == Math.abs(newColumn
						- otherAgent.getValue()))
					return false;
			}
		}

		return true;

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

	public int getLowestPriorityAgentInNogood(
			Set<Entry<Integer, Integer>> nogood) {
		int aux = n + 1;
		for (Entry<Integer, Integer> agent : nogood) {
			if (agent.getKey() < aux) {
				aux = agent.getKey();
			}
		}
		return aux;
	}

	public void setColumn(int column) {
		this.column = column;
	}

	public Set<Integer> getLinks() {
		return links;
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

	public void broadcast(int method, Object[] args) {
		for (int i = 0; i < n; i++) {
			if (i != row) {
				synchronized (blackboard) {
					blackboard.add(new Message(method, i, args));
				}
			}
		}
	}	
	
	public Map<Integer, Integer> getAgentView(){
		return agentView;
	}
	
	public Set<Set<Entry<Integer, Integer>>> getNogoods(){
		return nogoods;

	}
	
	public List<Integer> forbiddenDomain(){
		List<Integer> ans = new ArrayList<Integer>();
		for(int i = 0;i<n;i++){
			if(!isConsistent(i)){
				ans.add(i);
			}
		}
		return ans;
	}
}
