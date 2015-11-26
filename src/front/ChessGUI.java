package front;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

public class ChessGUI {

    private final JPanel gui = new JPanel(new BorderLayout(3, 3));
    private JButton[][] chessBoardSquares;
    private Image[][] chessPieceImages = new Image[2][6];
    private JPanel chessBoard;
    private final JLabel message = new JLabel(
            "Chess Champ is ready to play!");
    public static final int QUEEN = 0, KING = 1,
            ROOK = 2, KNIGHT = 3, BISHOP = 4, PAWN = 5;
    public static final int[] STARTING_ROW = {
        ROOK, KNIGHT, BISHOP, KING, QUEEN, BISHOP, KNIGHT, ROOK
    };
    public static final int BLACK = 0, WHITE = 1;
    
    public Map<Integer, Integer>[] agentViews;
    public Set<Set<Entry<Integer, Integer>>>[] nogoods;
    public MouseAdapter[] adapters;

    public ChessGUI(int n) {
    	chessBoardSquares = new JButton[n][n];
        initializeGui(n);
    }

    public final void initializeGui(int n) {
    	nogoods = (Set<Set<Entry<Integer, Integer>>>[]) new Set[n];
    	agentViews = (Map<Integer, Integer>[]) new Map[n];
    	adapters = new MouseAdapter[n];
        // create the images for the chess pieces
        createImages();

        // set up the main GUI
        gui.setBorder(new EmptyBorder(5, 5, 5, 5));

        gui.add(new JLabel("?"), BorderLayout.LINE_START);

        chessBoard = new JPanel(new GridLayout(0, n+1)) {

            /**
             * Override the preferred size to return the largest it can, in
             * a square shape.  Must (must, must) be added to a GridBagLayout
             * as the only component (it uses the parent as a guide to size)
             * with no GridBagConstaint (so it is centered).
             */
            @Override
            public final Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                Dimension prefSize = null;
                Component c = getParent();
                if (c == null) {
                    prefSize = new Dimension(
                            (int)d.getWidth(),(int)d.getHeight());
                } else if (c!=null &&
                        c.getWidth()>d.getWidth() &&
                        c.getHeight()>d.getHeight()) {
                    prefSize = c.getSize();
                } else {
                    prefSize = d;
                }
                int w = (int) prefSize.getWidth();
                int h = (int) prefSize.getHeight();
                // the smaller of the two sizes
                int s = (w>h ? h : w);
                return new Dimension(s,s);
            }
        };
        chessBoard.setBorder(new CompoundBorder(
                new EmptyBorder(n,n,n,n),
                new LineBorder(Color.BLACK)
                ));
        // Set the BG to be ochre
        Color ochre = new Color(204,119,34);
        chessBoard.setBackground(ochre);
        JPanel boardConstrain = new JPanel(new GridBagLayout());
        boardConstrain.setBackground(ochre);
        boardConstrain.add(chessBoard);
        gui.add(boardConstrain);

        // create the chess board squares
        Insets buttonMargin = new Insets(0, 0, 0, 0);
        for (int ii = 0; ii < chessBoardSquares.length; ii++) {
            for (int jj = 0; jj < chessBoardSquares[ii].length; jj++) {
                JButton b = new JButton();
                b.setMargin(buttonMargin);
                // our chess pieces are 64x64 px in size, so we'll
                // 'fill this in' using a transparent icon..
                ImageIcon icon = new ImageIcon(
                        new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB));
                b.setIcon(icon);
                if ((jj % 2 == 1 && ii % 2 == 1)
//                        ) {
                        || (jj % 2 == 0 && ii % 2 == 0)) {
                    b.setBackground(Color.WHITE);
                } else {
                    b.setBackground(Color.BLACK);
                }
                chessBoardSquares[jj][ii] = b;
            }
        }

        /*
         * fill the chess board
         */
        chessBoard.add(new JLabel(""));
        // fill the top row
        for (int ii = 0; ii < n; ii++) {
            chessBoard.add(
                    new JLabel(String.valueOf(ii),
                    SwingConstants.CENTER));
        }
        // fill the black non-pawn piece row
        for (int ii = 0; ii < n; ii++) {
            for (int jj = 0; jj < n; jj++) {
                switch (jj) {
                    case 0:
                        chessBoard.add(new JLabel("" + ii,
                                SwingConstants.CENTER));
                    default:
                        chessBoard.add(chessBoardSquares[jj][ii]);
                }
            }
        }
    }

    public final JComponent getGui() {
        return gui;
    }

    private final void createImages() {
        try {
            BufferedImage bi = ImageIO.read(new File("resources/chess_pieces.png"));
            for (int ii = 0; ii < 2; ii++) {
                for (int jj = 0; jj < 6; jj++) {
                    chessPieceImages[ii][jj] = bi.getSubimage(
                            jj * 64, ii * 64, 64, 64);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void putQueen(int i, int j, Map<Integer, Integer> agentView, Set<Set<Entry<Integer, Integer>>> nogoods){
    	this.nogoods[i] = nogoods;
    	this.agentViews[i] = agentView;
    	chessBoardSquares[j][i].setIcon(new ImageIcon(chessPieceImages[WHITE][QUEEN]));
    	adapters[i] = new MouseAdapter() {

            @Override
            public void mouseEntered(MouseEvent me) {
            	chessBoardSquares[j][i].setToolTipText("<html><p>Local View: " + agentView + "</p><br>" + "Nogoods: " + nogoods + "</p></html>");
            }
        };
        
        chessBoardSquares[j][i].addMouseListener(adapters[i]);
    }
    
    public void putQueen(int i, int j){
    	chessBoardSquares[j][i].setIcon(new ImageIcon(chessPieceImages[WHITE][QUEEN]));
    	adapters[i] = new MouseAdapter() {

            @Override
            public void mouseEntered(MouseEvent me) {
            	chessBoardSquares[j][i].setToolTipText("<html><p>Local View: " + agentViews[i] + "</p><br>" + "<P>Nogoods: " + nogoods[i] + "</p></html>");
            }
        };
        
        chessBoardSquares[j][i].addMouseListener(adapters[i]);
    }

    public void removeQueen(int i, int j, boolean removeListeners){
    	chessBoardSquares[j][i].setIcon(null);
    	if(removeListeners && adapters[i] != null){
    		chessBoardSquares[j][i].removeMouseListener(adapters[i]);
    	}
    }
    
    public void cleanBoard(int n){
    	for(int i = 0; i < n; i++)
    		for(int j=0; j< n; j++)
    			removeQueen(i, j, true);
    }
    
    public void paintforbiddenDomain(int row, List<Integer> domain){
    	for(Integer column : domain){
    		chessBoardSquares[column][row].setBackground(Color.RED);
    	}
    }
    
    public void returnOriginalColors(){
        for (int ii = 0; ii < chessBoardSquares.length; ii++) {
            for (int jj = 0; jj < chessBoardSquares[ii].length; jj++) {
            	
                if ((jj % 2 == 1 && ii % 2 == 1)
//                        ) {
                        || (jj % 2 == 0 && ii % 2 == 0)) {
                    chessBoardSquares[ii][jj].setBackground(Color.WHITE);
                } else {
                	chessBoardSquares[ii][jj].setBackground(Color.BLACK);
                }    	
            }
        }
    }

    /**
     * Initializes the icons of the initial chess board piece places
     */
    private final void setupNewGame() {
//        message.setText("Make your move!");
//        // set up the black pieces
//        for (int ii = 0; ii < STARTING_ROW.length; ii++) {
//            chessBoardSquares[ii][0].setIcon(new ImageIcon(
//                    chessPieceImages[BLACK][STARTING_ROW[ii]]));
//        }
//        for (int ii = 0; ii < STARTING_ROW.length; ii++) {
//            chessBoardSquares[ii][1].setIcon(new ImageIcon(
//                    chessPieceImages[BLACK][PAWN]));
//        }
//        // set up the white pieces
//        for (int ii = 0; ii < STARTING_ROW.length; ii++) {
//            chessBoardSquares[ii][6].setIcon(new ImageIcon(
//                    chessPieceImages[WHITE][PAWN]));
//        }
//        for (int ii = 0; ii < STARTING_ROW.length; ii++) {
//            chessBoardSquares[ii][7].setIcon(new ImageIcon(
//                    chessPieceImages[WHITE][STARTING_ROW[ii]]));
//        }
    }
}
