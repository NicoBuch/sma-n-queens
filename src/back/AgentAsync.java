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

public class AgentAsync implements Runnable{
	private int n;
	private int column = -1;
	private int row;
	private ChessGUI cg;
	private Set<Integer> links = new HashSet<Integer>();
	private Map<Integer, Integer> agent_view = new HashMap<Integer, Integer>();
	private Set<Set<Entry<Integer, Integer>>> nogoods = new HashSet<Set<Entry<Integer, Integer>>>();
	private List<Message> blackboard;
	
	public AgentAsync(int n, ChessGUI cg, int row, List<Message> blackboard) {
		this.n = n;
		this.cg = cg;
		this.row = row;
//		if(row > 0){
//			links.add(row - 1);			
//		}
		for(int i = 0; i< n; i++){
			if(i != row){
				links.add(i);
			}
		}
		this.blackboard = blackboard;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void run() {	
		while(true){
			if(isSolved()){
				System.out.println("Agent " + row + " realized it is solved");
				agent_view.put(row, column);
				Object[] args2 = { agent_view };
				synchronized (blackboard) {
					for(Integer link : links){
						blackboard.add(new Message(3, link, args2));
					}
				}
				return;
			}
			Message message = null;
			synchronized (blackboard) {
				for(int i = 0; i < blackboard.size(); i++){
					Message m = blackboard.get(i);
					if(m.getDestinatary() == row){
						message = m;
						blackboard.remove(i);
						break;
					}
				}
			}
			if(message != null){
				switch (message.getMethod()) {
				case 0:
					ok((Integer)message.getArgs()[0], (Integer)message.getArgs()[1]);
					break;
				case 1:
					nogood((Integer)message.getArgs()[0], (Set<Entry<Integer,Integer>>)message.getArgs()[1]);
					break;
				case 2:
					add_link((Integer)message.getArgs()[0]);
					break;
				case 3:
					build_solution((Map<Integer, Integer>)message.getArgs()[0]);
					System.out.println("Im agent " + row + "and Im out");
					return;
				}
			}
		}
	}
	
	public void ok(int other_agent, int other_column){
		agent_view.put(other_agent, other_column);
		check_agent_view();
	}
	
	public void nogood(Integer sender, Set<Entry<Integer, Integer>> nogood){
		nogoods.add(clone_entry_set(nogood));
//		int old_column = column;
		check_agent_view();
//		if(old_column == column){
//			Object[] args = { row, column };
//			synchronized (blackboard) {
//				blackboard.add(new Message(0, sender, args));
//			}
//				
//		}
	}
	
	public void add_link(int other_agent){
		links.add(other_agent);
	}
	
	public void check_agent_view(){
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(column == -1 || !check_consistency(agent_view.entrySet(), column)){
			int[] array = random_domain();
			for(int new_column : array){
				if(check_consistency(agent_view.entrySet(), new_column)){
					if(column != -1)
						cg.removeQueen(row, column);
					column = new_column;
					cg.putQueen(row, column);
					Object[] args = { row, column };
					for(Integer i : links){
						synchronized (blackboard) {
							blackboard.add(new Message(0, i, args));
						}
					}	
					return;
				}
			}
			backtrack();
		}
	}
	
	public void backtrack(){
		Set<Set<Entry<Integer, Integer>>> calculated_nogoods = calculate_nogoods();
			
		System.out.println(calculated_nogoods.size());
		nogoods.addAll(calculated_nogoods);
		Set<Entry<Integer, Integer>> nogoodEntries = most_nogood_entry(nogoods);
		Entry<Integer, Integer> min_entry = get_lowest_priority_entry(nogoodEntries);
		if(min_entry == null){
			return;
		}
		Object [] args = { row, get_actual_agent_view() };
		synchronized (blackboard) {
			blackboard.add(new Message(1, min_entry.getKey(), args));
		}
//		agent_view.remove(min_entry.getKey());
//		check_agent_view();				
	}
	
	public Entry<Integer, Integer> get_lowest_priority_entry(Set<Entry<Integer, Integer>> nogood){
		Entry<Integer, Integer> min_entry = null;
		for(Entry<Integer, Integer> entry : nogood){
			if(min_entry == null){
				min_entry = entry;
			}
			else{
				if(entry.getKey() < min_entry.getKey()){
					min_entry = entry;
				}
			}
		}
		return min_entry;
	}
	
	public boolean check_consistency(Set<Entry<Integer, Integer>> view, int new_column) {
		if(!is_consistent(view, new_column, row)){
			return false;
		}
		for(Set<Entry<Integer, Integer>> nogood : nogoods){
			if(is_compatible_nogood(view, nogood, new_column)){
				return false;
			}
		}
		return true;
		
	}
	
	
	public boolean is_consistent(Set<Entry<Integer, Integer>> view, int new_column, int new_row){
		for (Entry<Integer, Integer> agent : view) {
			for(Entry<Integer, Integer> other_agent : view){
				if (other_agent.getKey() != agent.getKey()) {
					if (agent.getValue() == other_agent.getValue())
						return false;
					if (Math.abs(other_agent.getKey() - agent.getKey()) ==  Math.abs(agent.getValue() - other_agent.getValue()))
						return false;
				}				
			}
			if(agent.getKey() != new_row){
				int other_column = agent.getValue();
				if (other_column == new_column)
					return false;
				if (Math.abs(agent.getKey() - new_row) ==  Math.abs(other_column - new_column))
					return false;	
			}
		}
		return true;
	}
	
	public Set<Set<Entry<Integer, Integer>>> calculate_nogoods(){
		Set<Set<Entry<Integer, Integer>>> answer = new HashSet<Set<Entry<Integer, Integer>>>();
		Set<Set<Entry<Integer, Integer>>> subsets = powerSet(agent_view.entrySet());
		for(Set<Entry<Integer, Integer>> subset : subsets){
			if(!is_consistent_set(subset) && !subset.isEmpty()){
				answer.add(subset);
			}
		}
		return answer;
	}
	
	public boolean is_compatible_nogood(Set<Entry<Integer, Integer>> view, Set<Entry<Integer, Integer>> nogood, int new_column){
		for(Entry<Integer, Integer> entry : nogood){
			if(entry.getKey() == row && entry.getValue() != new_column){
				return false;
			}
			else if(!view.contains(entry)){
				return false;
			}
		}
		return true;
	}
	
	public boolean is_consistent_set(Set<Entry<Integer, Integer>> set){
		for(int i = 0; i < n; i++){
			if(check_consistency(set, i)){
				return true;
			}
		}
		return false;
	}
	
	
	public Set<Set<Entry<Integer, Integer>>> powerSet(Set<Entry<Integer, Integer>> originalSet) {
        Set<Set<Entry<Integer, Integer>>> sets = new HashSet<Set<Entry<Integer, Integer>>>();
        if (originalSet.isEmpty()) {
            sets.add(new HashSet<Entry<Integer, Integer>>());
            return sets;
        }
        List<Entry<Integer, Integer>> list = new ArrayList<Entry<Integer, Integer>>(clone_entry_set(originalSet));
        Entry<Integer, Integer> head = list.get(0);
        Set<Entry<Integer, Integer>> rest = new HashSet<Entry<Integer, Integer>>(list.subList(1, list.size()));
        for (Set<Entry<Integer, Integer>> set : powerSet(rest)) {
            Set<Entry<Integer, Integer>> newSet = new HashSet<Entry<Integer, Integer>>();
            newSet.add(head);
            newSet.addAll(set);
            sets.add(newSet);
            sets.add(set);
        }
        return sets;
    }
	
	public boolean isSolved(){
		if(agent_view.keySet().size() == n-1 && is_consistent(agent_view.entrySet(), column, row))
			return true;
		return false;
	}
	
	public int[] random_domain(){
		int[] domain = new int[n];
		for(int i = 0; i< n; i++){
			domain[i] = i;
		}
	    Random rnd = ThreadLocalRandom.current();
	    for (int i = domain.length - 1; i > 0; i--)
	    {
	      int index = rnd.nextInt(i + 1);
	      // Simple swap
	      int a = domain[index];
	      domain[index] = domain[i];
	      domain[i] = a;
	    }
	    return domain;
	}
	
	public Set<Entry<Integer, Integer>> clone_entry_set(Set<Entry<Integer, Integer>> set){
		Map<Integer, Integer> clone = new HashMap<Integer, Integer>();
		for(Entry<Integer, Integer>  entry : set){
			clone.put(entry.getKey(), entry.getValue());
		}
		return clone.entrySet();
	}
	
//	public Set<Entry<Integer, Integer>> new_nogood(Set<Entry<Integer, Integer>> subset){
//		Map<Integer, Integer> nogood_map = new HashMap<Integer, Integer>();
//		for(Entry<Integer, Integer>  entry : subset){
//			nogood_map.put(entry.getKey(), entry.getValue());
//		}
//	}
	
	public void build_solution(Map<Integer, Integer> solution){
		if(column != -1)
			cg.removeQueen(row, column);
		cg.putQueen(row, solution.get(row));
		return;
	}
	
	public void setColumn(int column){
		this.column = column;
	}
	
	public Set<Integer> getLinks(){
		return links;
	}
	
	public Set<Entry<Integer, Integer>> most_nogood_entry(Set<Set<Entry<Integer, Integer>>> nogoods){
		Map<Entry<Integer, Integer>, Integer> map = new HashMap<Entry<Integer, Integer>, Integer>();
		nogoods.forEach(nogood -> {
			nogood.forEach( entry ->{
				Integer size = map.get(entry);
				map.put(entry, size == null ? 1 : size + 1);
			});
		});
		Set<Entry<Integer, Integer>> set = new HashSet<Entry<Integer, Integer>>();
		int max = 0;
		for(Entry<Integer, Integer> entry : map.keySet()){
			int value = map.get(entry);
			if(value > max){
				max = value;
				set.clear();
				set.add(entry);
			}
			else if(max == value){
				set.add(entry);
			}
		}
		return set;
	}
	
	public Set<Entry<Integer, Integer>> get_actual_agent_view(){
		Map<Integer, Integer> map = new HashMap<Integer, Integer>();
		map.putAll(agent_view);
		map.put(row, column);
		return map.entrySet();
	}

}
