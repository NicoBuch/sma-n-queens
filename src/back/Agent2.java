package back;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

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
		checkLocalView("OK de " + otherRow + " en " + otherColumn);
	}

	public void nogood(int sender, Set<Entry<Integer, Integer>> nogood) {
		addNogood(nogood);
		checkLocalView("Nogood de " + sender + ": " + nogood);
	}

	public void addLink(int link) {
		links.add(link);
	}

	public void checkLocalView(String message) {
		cg.returnOriginalColors();
		cg.cleanBoard(n);
		
		for(Entry<Integer, Integer> entry : agentView.entrySet()){
			cg.putQueen(entry.getKey(), entry.getValue());
		}
		if(column != -1)
			cg.putQueen(row, column, agentView, nogoods);
		cg.paintforbiddenDomain(row, forbiddenDomain());
		cg.alert(message, row, nogoods);
		if (column == -1 || !isConsistent(column)) {
			for (int i : randomDomain()) {
				if (isConsistent(i)) {
					if (column != -1)
						cg.removeQueen(row, column, true);
					column = i;
					cg.putQueen(row, column, agentView, nogoods);
					cg.alert("Pone en la columna " + column + " y envía OK", row, nogoods);
					Object[] args = { row, column };
					for (Integer link : links) {
						synchronized (blackboard) {
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
			addNogood(nogood);
			Object[] args = { row, nogood };
			int minAgent = getLowestPriorityAgentInNogood(nogood);
			cg.alert("Enviando nogood " + nogood + " al agente " + minAgent, row, nogoods);
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
		Random rnd = new Random();
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
		Set<Entry<Integer, Integer>> auxAns = cloneEntrySet(ans); 
		List<Set<Entry<Integer, Integer>>> combinedNogoods = new ArrayList<Set<Entry<Integer, Integer>>>();
		for(Entry<Integer, Integer> entry : ans){
			boolean goOn = true;
			Set<Integer> completedDomain = new HashSet<Integer>();
			for(int i = 0; i < n && goOn; i++){
				goOn = false;
				if(entry.getValue() == i){
					goOn = true;
					Set<Entry<Integer, Integer>> asd =  cloneEntrySet(ans);
					combinedNogoods.add(asd);
					completedDomain.add(i);
				}
				for(Set<Entry<Integer, Integer>> nogood : aux){
					for(Entry<Integer, Integer> nogoodEntry : nogood){
						if(nogoodEntry.getKey() == entry.getKey() && nogoodEntry.getValue() == i){
							goOn = true;
							Set<Entry<Integer, Integer>> asd =  cloneEntrySet(nogood);
							combinedNogoods.add(asd);
							completedDomain.add(i);
							break;
						}
					}
				}
			}
			if(completedDomain.size() == n){
				if(combinedNogoods.size() == n){
					List<Set<Entry<Integer, Integer>>> toAnalize = new ArrayList<Set<Entry<Integer, Integer>>>();
					for(Set<Entry<Integer, Integer>> nogood: combinedNogoods){
						nogoods.remove(nogood);
						Entry<Integer, Integer> toRemove = null;
						for(Entry<Integer, Integer> anEntry : nogood){
							if(anEntry.getKey() == entry.getKey()){
								toRemove = anEntry;
							}
						}
						Set<Entry<Integer, Integer>> asd = cloneEntrySet(nogood);
						asd.remove(toRemove);
						toAnalize.add(asd);
					}
					auxAns = concatNogoods(toAnalize);
					if(auxAns == null || auxAns.isEmpty()){
						return ans;
					}
					else{
						System.out.println(auxAns);
						return auxAns;
					}
					
				}
				else if(combinedNogoods.size() > n){
					auxAns = handleLargeNogoodSet(entry.getKey(), combinedNogoods);
					if(auxAns == null || auxAns.isEmpty()){
						return ans;
					}
					else{
						return auxAns;
					}
				}
			}
			combinedNogoods.clear();
			completedDomain.clear();
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
	
	public void addNogood(Set<Entry<Integer, Integer>> nogood){
		Set<Set<Entry<Integer, Integer>>> toRemove = new HashSet<Set<Entry<Integer, Integer>>>();
		for(Set<Entry<Integer, Integer>> myNogood : nogoods){
			if(isSubset(nogood, myNogood)){
				nogoods.removeAll(toRemove);
				return;
			}
			else if(isSubset(myNogood, nogood)){
				toRemove.add(myNogood);
			}
		}
		nogoods.removeAll(toRemove);
		nogoods.add(nogood);
	}
	
	public Set<Entry<Integer, Integer>> concatNogoods(List<Set<Entry<Integer, Integer>>> combinedNogoods){
		Set<Entry<Integer, Integer>> answer = null;
		for(Set<Entry<Integer, Integer>> nogood : combinedNogoods){
			if(answer == null){
				answer = nogood;
			}
			else{
				answer = concat(nogood, answer);
			}
		}
		return answer;
	}
	
	public Set<Entry<Integer, Integer>> concat(Set<Entry<Integer, Integer>> nogood, Set<Entry<Integer, Integer>> otherNogood){
		Set<Entry<Integer, Integer>> concatedNogood = new HashSet<Entry<Integer, Integer>>();
		if(nogood.isEmpty() || otherNogood.isEmpty()){
			return concatedNogood;
		}
		if(nogood != otherNogood){
			boolean noNogood = false;
			for(Entry<Integer, Integer> entry : nogood){
				if(noNogood){
					concatedNogood.clear();
					break;
				}
				for(Entry<Integer, Integer> otherEntry : otherNogood){
					if(entry.getKey() == otherEntry.getKey()){
						if(entry.getValue() == otherEntry.getValue()){
							for(Entry<Integer, Integer> cEntry : concatedNogood){
								if(cEntry.getKey() == entry.getKey() && cEntry.getValue() != entry.getValue()){
									noNogood = true;
								}
							}
							if(!noNogood)
								concatedNogood.add(entry);
						}
						else{
							noNogood = true;
						}
					}
					else{
						for(Entry<Integer, Integer> cEntry : concatedNogood){
							if(cEntry.getKey() == entry.getKey() && cEntry.getValue() != entry.getValue()){
								noNogood = true;
							}
							if(cEntry.getKey() == otherEntry.getKey() && cEntry.getValue() != otherEntry.getValue()){
								noNogood = true;
							}
						}
						if(!noNogood){
							concatedNogood.add(entry);
							concatedNogood.add(otherEntry);
						}
					}
				}
			}
		}
		return concatedNogood;
	}
	
	public Set<Entry<Integer, Integer>> handleLargeNogoodSet(int key, List<Set<Entry<Integer, Integer>>> combinedNogoods){
		Set<Map<Integer, Set<Entry<Integer, Integer>>>> alreadyAnalized = new HashSet<Map<Integer, Set<Entry<Integer, Integer>>>>();
		
		Map<Integer, Set<Entry<Integer, Integer>>> toAnalize = new HashMap<Integer, Set<Entry<Integer, Integer>>>();
		for(int i = 0; i < combinedNogoods.size(); i++){
			for(Set<Entry<Integer, Integer>> otherNogood : combinedNogoods){
				int lastKey = -1;
				for(Entry<Integer, Integer> entry : otherNogood){
					if(entry.getKey() == key){
						if(!toAnalize.containsKey(entry.getValue())){
							Set<Entry<Integer, Integer>> asd = cloneEntrySet(otherNogood);
							asd.remove(entry);
							toAnalize.put(entry.getValue(), asd);
							if(alreadyAnalized.contains(toAnalize)){
								toAnalize.remove(entry.getValue());
							}
							else{
								lastKey = entry.getValue();
								break;
							}
						}
					}
				}
				if(toAnalize.size() == n){
					Set<Entry<Integer, Integer>> ans = concatNogoods(new ArrayList<Set<Entry<Integer, Integer>>>(toAnalize.values()));
					if(ans == null || ans.isEmpty()){
						alreadyAnalized.add(toAnalize);
						toAnalize.remove(lastKey);
					}
					else{
						for(Set<Entry<Integer, Integer>> theNogood : toAnalize.values()){
							nogoods.remove(theNogood);
						}
						return ans;
					}
				}
			}
		}
		return new HashSet<Entry<Integer, Integer>>();
	}
	
}
