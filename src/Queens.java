import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

        ExecutorService executor = Executors.newFixedThreadPool(n);
        List<Message> blackboard = new ArrayList<Message>();
        for(int i = 0; i< n; i++){
        	Agent2 newAgent = new Agent2(n, cg, i, blackboard);
        	if(i == n-1){
    			Random rand = new Random();
    			int col = rand.nextInt(((n-1) - 0) + 1) + 0;
//    			int col = ;
    			newAgent.setColumn(col);
    			cg.putQueen(i, col);
    			Object[] argss = { i, col };

    			for(int j : newAgent.getLinks()){
    				synchronized (blackboard) {
    					blackboard.add(new Message(0, j, argss));
    				}
    			}
        	}
        	executor.execute(newAgent);
        }
        executor.shutdown();
        long time = System.currentTimeMillis();
        while(!executor.isTerminated()){
        }
        System.out.println("Elapsed time: " + (System.currentTimeMillis() - time));
        f.dispose();
	}

}


