import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;

import javax.swing.JFrame;

import front.ChessGUI;


public class SyncQueens {
	
	public static void main(String[] args) {
		int n = Integer.valueOf(args[0]);
		
        ChessGUI cg = new ChessGUI(n);

        JFrame f = new JFrame("ChessChamp");
        f.add(cg.getGui());
        // Ensures JVM closes after frame(s) closed and
        // all non-daemon threads are finished
        f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        // See http://stackoverflow.com/a/7143398/418556 for demo.
        f.setLocationByPlatform(true);

        // ensures the frame is the minimum size it needs to be
        // in order display the components within it
        f.pack();
        // ensures the minimum size is enforced.
        f.setMinimumSize(f.getSize());
//        f.setVisible(true);
        
        long time = System.currentTimeMillis();
        Map<Integer, Integer> board = new HashMap<Integer, Integer>();
        if(backtrack(0, n, board, cg)){
        	System.out.println("Elapsed time: " + (System.currentTimeMillis() - time));
        }
        else{
        	System.out.println("FAILED");
        }
        f.dispose();
	}
	
	public static boolean backtrack(int row, int n, Map<Integer, Integer> board, ChessGUI cg) {
		if(board.size() == n && isConsistent(board)){
			return true;
		}
		if(!board.isEmpty() && !isConsistent(board)){
			return false;
		}
		
		for(int column : randomDomain(n)){
			board.put(row, column);
			cg.putQueen(row, column);
			if(backtrack(row+1, n, board, cg)){
				return true;
			}
			cg.removeQueen(row, column);
			board.remove(row);
		}
		return false;
		
	}
	
	
	public static boolean isConsistent(Map<Integer, Integer> board){
		for(Entry<Integer, Integer> queen : board.entrySet()){
			for (Entry<Integer, Integer> otherQueen : board.entrySet()) {
				if (queen.getKey() != otherQueen.getKey()) {
					if (otherQueen.getValue() == queen.getValue())
						return false;
					if (Math.abs(queen.getKey() - otherQueen.getKey()) == Math.abs(otherQueen.getValue()
							- queen.getValue()))
						return false;
				}
			}
		}
		return true;
	}
	
	public static int[] randomDomain(int n) {
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

}
