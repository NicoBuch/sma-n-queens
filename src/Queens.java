import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

import javax.swing.JFrame;

import back.Agent2;
import back.Message;
import front.ChessGUI;



public class Queens {

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
         f.setVisible(true);
        List<Agent2> agents = new ArrayList<Agent2>();
        ExecutorService executor = Executors.newFixedThreadPool(n);
        List<Message> blackboard = new ArrayList<Message>();
        Random rand = new Random();
        for(int i = 0; i< n; i++){
        	Agent2 newAgent = new Agent2(n, cg, i, blackboard);
        	agents.add(newAgent);
//        	executor.execute(newAgent);
        }
//        executor.shutdown();
        
        long time = System.currentTimeMillis();
//        while(!executor.isTerminated());
        int allEnded = 0;
        while(allEnded < n){
        	int i = rand.nextInt(((n-1) - 0) + 1) + 0;
    		if(agents.get(i).runSync())
    			allEnded++;
        }
        System.out.println("Elapsed time: " + (System.currentTimeMillis() - time));
        System.out.println("count: "+ cg.getCount());
        f.dispose();
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


