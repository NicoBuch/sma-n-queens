package front;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.imageio.ImageIO;
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
	private final JLabel message = new JLabel("Empecemos!");
	public static final int QUEEN = 0, KING = 1, ROOK = 2, KNIGHT = 3,
			BISHOP = 4, PAWN = 5;
	public static final int[] STARTING_ROW = { ROOK, KNIGHT, BISHOP, KING,
			QUEEN, BISHOP, KNIGHT, ROOK };
	public static final int BLACK = 0, WHITE = 1;

	public Map<Integer, Integer>[] agentViews;
	public Set<Set<Entry<Integer, Integer>>>[] nogoods;
	public MouseAdapter[] adapters;
	private AtomicBoolean paused = new AtomicBoolean(false);
	private AtomicBoolean realPositions = new AtomicBoolean(false);
	private Map<Integer, Integer> actualMap = new HashMap<Integer, Integer>();
	private Map<Integer, Integer>[] previosViews;
	private List<Integer> actualReds = new ArrayList<Integer>();
	private int actualRow;
	public Integer count = 0;
	public int[] columns;
	private Entry<Integer, Integer> previosActive;

	public ChessGUI(int n) {
		chessBoardSquares = new JButton[n][n];
		initializeGui(n);
	}

	public final void initializeGui(int n) {
		nogoods = (Set<Set<Entry<Integer, Integer>>>[]) new Set[n];
		agentViews = (Map<Integer, Integer>[]) new Map[n];
		previosViews = (Map<Integer, Integer>[]) new Map[n];
		adapters = new MouseAdapter[n];
		columns = new int[n];
		for(int i = 0; i < n; i++){
			columns[i] = -1;
		}

		// create the images for the chess pieces
		createImages();

		// set up the main GUI
		gui.setBorder(new EmptyBorder(5, 5, 5, 5));

		JToolBar tools = new JToolBar();
		tools.setFloatable(false);
		gui.add(tools, BorderLayout.PAGE_START);
		
		Color ochre = new Color(204, 119, 34);
		Color green = new Color(10, 147, 79);
		
		//Button to change board and show real positions
		JButton positionsButton = new JButton("Ver Posiciones Reales");
		positionsButton.addMouseListener(new MouseListener() {
			
			@Override
			public void mouseReleased(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void mousePressed(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void mouseExited(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void mouseEntered(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void mouseClicked(MouseEvent e) {
				cleanBoard(n);
				
				// GO back to agentView Positions
				if(realPositions.get()){
					chessBoard.setBackground(ochre);
					for(Entry<Integer, Integer> entry : actualMap.entrySet()){
						internalPutQueen(entry.getKey(), entry.getValue());
					}
					for(Integer column : actualReds){
						chessBoardSquares[column][actualRow].setBackground(Color.RED);
					}
					if(previosActive != null){
						ImageIcon icon = new ImageIcon(chessPieceImages[BLACK][QUEEN]);
						chessBoardSquares[previosActive.getValue()][previosActive.getKey()].setIcon(icon);
					}
					realPositions.set(false);
				}
				
				//Show real positions of all agents
				else{
					chessBoard.setBackground(green);
					for(Integer column : actualReds){
						Color bg = null;
						if((column + actualRow) % 2 == 0){
							bg = Color.WHITE;
						}
						else{
							bg = Color.BLACK;
						}
						chessBoardSquares[column][actualRow].setBackground(bg);
					}
					for(int i = 0; i < n; i++){
						if(columns[i] != -1)
							internalPutQueen(i, columns[i]);
					}
					realPositions.set(true);
				}
			}
		});
		
		
		
		tools.add(positionsButton);
		tools.addSeparator();
		tools.add(message);

		gui.add(new JLabel("?"), BorderLayout.LINE_START);

		chessBoard = new JPanel(new GridLayout(0, n + 1)) {

			/**
			 * Override the preferred size to return the largest it can, in a
			 * square shape. Must (must, must) be added to a GridBagLayout as
			 * the only component (it uses the parent as a guide to size) with
			 * no GridBagConstaint (so it is centered).
			 */
			@Override
			public final Dimension getPreferredSize() {
				Dimension d = super.getPreferredSize();
				Dimension prefSize = null;
				Component c = getParent();
				if (c == null) {
					prefSize = new Dimension((int) d.getWidth(),
							(int) d.getHeight());
				} else if (c != null && c.getWidth() > d.getWidth()
						&& c.getHeight() > d.getHeight()) {
					prefSize = c.getSize();
				} else {
					prefSize = d;
				}
				int w = (int) prefSize.getWidth();
				int h = (int) prefSize.getHeight();
				// the smaller of the two sizes
				int s = (w > h ? h : w);
				return new Dimension(s, s);
			}
		};

		gui.addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent e) {
				if (paused.get()) {
					realPositions.set(false);
					chessBoard.setBackground(ochre);
					paused.set(false);
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {

			}

			@Override
			public void keyPressed(KeyEvent e) {

			}
		});

		gui.addMouseListener(new MouseListener() {

			@Override
			public void mouseReleased(MouseEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mousePressed(MouseEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseExited(MouseEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseEntered(MouseEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseClicked(MouseEvent e) {
				if (paused.get()) {
					realPositions.set(false);
					chessBoard.setBackground(ochre);
					paused.set(false);
				}

			}
		});
		gui.setFocusable(true);

		chessBoard.setBorder(new CompoundBorder(new EmptyBorder(n, n, n, n),
				new LineBorder(Color.BLACK)));
		// Set the BG to be ochre
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
				ImageIcon icon = new ImageIcon(new BufferedImage(64, 64,
						BufferedImage.TYPE_INT_ARGB));
				b.setIcon(icon);
				if ((jj % 2 == 1 && ii % 2 == 1)
				// ) {
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
			JLabel topNumber = new JLabel(String.valueOf(ii),
					SwingConstants.CENTER);
			topNumber.setForeground(Color.WHITE);
			chessBoard.add(topNumber);
		}
		// fill the black non-pawn piece row
		for (int ii = 0; ii < n; ii++) {
			for (int jj = 0; jj < n; jj++) {
				switch (jj) {
				case 0:
					JLabel leftNumbers = new JLabel("" + ii, SwingConstants.CENTER);
					leftNumbers.setForeground(Color.WHITE);
					chessBoard.add(leftNumbers);
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
			BufferedImage bi = ImageIO.read(new File(
					"resources/chess_pieces.png"));
			for (int ii = 0; ii < 2; ii++) {
				for (int jj = 0; jj < 6; jj++) {
					chessPieceImages[ii][jj] = bi.getSubimage(jj * 64, ii * 64,
							64, 64);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void putQueen(int i, int j, Map<Integer, Integer> agentView,
			Set<Set<Entry<Integer, Integer>>> nogoods) {
		synchronized (count) {
			count++;
		}
		this.nogoods[i] = nogoods;
		this.agentViews[i] = agentView;
		this.columns[i] = j;
		this.actualMap.put(i, j);
		chessBoardSquares[j][i].setIcon(new ImageIcon(
				chessPieceImages[WHITE][QUEEN]));
		adapters[i] = new MouseAdapter() {

			@Override
			public void mouseEntered(MouseEvent me) {
				chessBoardSquares[j][i]
						.setToolTipText("<html><p>Columna Actual: " + j
								+ "</p><br>" + "<p>Local View: " + agentView
								+ "</p><br>" + "Nogoods: " + nogoods
								+ "</p></html>");
			}
		};

		chessBoardSquares[j][i].addMouseListener(adapters[i]);
	}

	public void putQueen(int i, int j) {
		synchronized (count) {
			count++;
		}
		actualMap.put(i, j);
		chessBoardSquares[j][i].setIcon(new ImageIcon(
				chessPieceImages[WHITE][QUEEN]));
		adapters[i] = new MouseAdapter() {

			@Override
			public void mouseEntered(MouseEvent me) {
				chessBoardSquares[j][i]
						.setToolTipText("<html><p>Columna Actual: "
								+ columns[i] + "</p><br>" + "<p>Local View: "
								+ agentViews[i] + "</p><br>" + "<p>Nogoods: "
								+ nogoods[i] + "</p></html>");
			}
		};

		chessBoardSquares[j][i].addMouseListener(adapters[i]);
	}

	public void removeQueen(int i, int j, boolean removeListeners) {
		chessBoardSquares[j][i].setIcon(null);
		if (removeListeners && adapters[i] != null) {
			chessBoardSquares[j][i].removeMouseListener(adapters[i]);
		}
	}

	public void cleanBoard(int n) {
		for (int i = 0; i < n; i++)
			for (int j = 0; j < n; j++)
				removeQueen(i, j, true);
	}

	public void paintforbiddenDomain(int row, List<Integer> domain) {
		actualReds.clear();
		actualRow = row;
		for (Integer column : domain) {
			actualReds.add(column);
			chessBoardSquares[column][row].setBackground(Color.RED);
		}
	}

	public void returnOriginalColors() {
		actualMap.clear();
		for (int ii = 0; ii < chessBoardSquares.length; ii++) {
			for (int jj = 0; jj < chessBoardSquares[ii].length; jj++) {

				if ((jj % 2 == 1 && ii % 2 == 1)
				// ) {
						|| (jj % 2 == 0 && ii % 2 == 0)) {
					chessBoardSquares[ii][jj].setBackground(Color.WHITE);
				} else {
					chessBoardSquares[ii][jj].setBackground(Color.BLACK);
				}
			}
		}
	}

	public void alert(String message, int agent,
			Set<Set<Entry<Integer, Integer>>> nogoods) {
		this.message
				.setText("<html>Turno del agente " + agent + "<br>" + message
						+ "<br>" + "Nogoods actuales: " + nogoods + "</html>");

		paused.set(true);

		while (paused.get()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public Integer getCount() {
		return count;
	}

	public void pause() {
		paused.set(true);

		while (paused.get()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	/**
	 * Initializes the icons of the initial chess board piece places
	 */
	private final void setupNewGame() {
		// message.setText("Make your move!");
		// // set up the black pieces
		// for (int ii = 0; ii < STARTING_ROW.length; ii++) {
		// chessBoardSquares[ii][0].setIcon(new ImageIcon(
		// chessPieceImages[BLACK][STARTING_ROW[ii]]));
		// }
		// for (int ii = 0; ii < STARTING_ROW.length; ii++) {
		// chessBoardSquares[ii][1].setIcon(new ImageIcon(
		// chessPieceImages[BLACK][PAWN]));
		// }
		// // set up the white pieces
		// for (int ii = 0; ii < STARTING_ROW.length; ii++) {
		// chessBoardSquares[ii][6].setIcon(new ImageIcon(
		// chessPieceImages[WHITE][PAWN]));
		// }
		// for (int ii = 0; ii < STARTING_ROW.length; ii++) {
		// chessBoardSquares[ii][7].setIcon(new ImageIcon(
		// chessPieceImages[WHITE][STARTING_ROW[ii]]));
		// }
	}
	
	private void internalPutQueen(int i, int j){
		chessBoardSquares[j][i].setIcon(new ImageIcon(
				chessPieceImages[WHITE][QUEEN]));
		adapters[i] = new MouseAdapter() {

			@Override
			public void mouseEntered(MouseEvent me) {
				chessBoardSquares[j][i]
						.setToolTipText("<html><p>Columna Actual: "
								+ columns[i] + "</p><br>" + "<p>Local View: "
								+ agentViews[i] + "</p><br>" + "<p>Nogoods: "
								+ nogoods[i] + "</p></html>");
			}
		};

		chessBoardSquares[j][i].addMouseListener(adapters[i]);
		
	}
	
	public void setPreviousValue(int agent, Map<Integer, Integer> agentView){
		previosActive = null;
		if(previosViews[agent] == null){
			previosViews[agent] = new HashMap<Integer, Integer>();
		}
		previosViews[agent].putAll(agentView);
	}
	
	public void putMovement(int agent, int row){
		if(previosViews[agent].containsKey(row)){
			ImageIcon icon = new ImageIcon(chessPieceImages[BLACK][QUEEN]);
			chessBoardSquares[previosViews[agent].get(row)][row].setIcon(icon);
			previosActive = new Entry<Integer, Integer>(){

				@Override
				public Integer getKey() {
					// TODO Auto-generated method stub
					return row;
				}

				@Override
				public Integer getValue() {
					// TODO Auto-generated method stub
					return previosViews[agent].get(row);
				}

				@Override
				public Integer setValue(Integer value) {
					// TODO Auto-generated method stub
					return null;
				}
				
			};
		}
	}
}
