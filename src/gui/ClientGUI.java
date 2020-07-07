package gui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.imageio.ImageIO;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.TitledBorder;

import org.json.JSONArray;
import org.json.JSONObject;

import common.Fields;

public class ClientGUI extends JFrame {
	private static final long serialVersionUID = -3761431599578875571L;

	public static final String TITLE_FORMAT = "Distributed Whiteboard - %s:%d";
	public static final String TITLE_FORMAT_CONNECTING = TITLE_FORMAT
		+ " - Connecting...";

	/**
	 * Enumeration of the possible tools the user can use
	 */
	public enum Tool {
		LINE, RECTANGLE, CIRCLE, OVAL, FREEHAND, ERASER, TEXT, COLOR_PICKER;

		public boolean isDragOnly() {
			return this == FREEHAND || this == ERASER;
		}

		public boolean isSingleClickOnly() {
			return this == TEXT || this == COLOR_PICKER;
		}
	}

	private final GUIController controller;

	private final Lock boardLock = new ReentrantLock();

	private BufferedImage board;
	private JButton selectedToolButton;
	private Color selectedColor = Color.BLACK;
	private Tool selectedTool;

	private final JMenu mnFile;
	private final JMenuItem mntmClose;
	private final JPanel pnlActiveUsers;
	private final JList<String> listUsers;
	private final JPanel pnlBoard;
	private final JPanel pnlColorSelected;
	private final JSpinner spnSize;

	private Integer startX = null;
	private Integer startY = null;
	private Integer endX = null;
	private Integer endY = null;
	private Boolean mouseOn = false;

	private boolean activeInput;

	/**
	 * Creates the ClientGUI with the specified attributes
	 * 
	 * @param controller the controller of this ClientGUI
	 */
	public ClientGUI(GUIController controller, String ip, int port) {
		this.controller = controller;

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				confirmClose();
			}
		});
		setIconImage(
			new ImageIcon(getClass().getResource("res/img/color.png"))
				.getImage());
		setTitle(String.format(TITLE_FORMAT_CONNECTING, ip, port));
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setResizable(false);
		setPreferredSize(new Dimension(800, 700));
		getContentPane().setLayout(new BorderLayout(0, 0));

		Dimension dimBtn = new Dimension(34, 34);

		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);

		mnFile = new JMenu("File");
		menuBar.add(getMnFile());

		JMenuItem mntmResync = new JMenuItem("Resync");
		mntmResync.addActionListener(e -> {
			controller.sendToServer(
				new JSONObject().put(Fields.COMMAND, Fields.RESYNC));
		});
		getMnFile().add(mntmResync);

		JSeparator separator = new JSeparator();
		getMnFile().add(separator);

		mntmClose = new JMenuItem("Close");
		getMntmClose().addActionListener(e -> {
			confirmClose();
		});
		getMnFile().add(getMntmClose());

		JPanel pnlControls = new JPanel();
		getContentPane().add(pnlControls, BorderLayout.WEST);
		GridBagLayout gbl_pnlControls = new GridBagLayout();
		gbl_pnlControls.columnWidths = new int[] { 98
		};
		gbl_pnlControls.rowHeights = new int[] { 23, 23, 0, 0, 0
		};
		gbl_pnlControls.columnWeights = new double[] { 1.0
		};
		gbl_pnlControls.rowWeights = new double[] { 0.0, 0.0, 0.0, 1.0,
			Double.MIN_VALUE
		};
		pnlControls.setLayout(gbl_pnlControls);

		JPanel pnlTools = new JPanel();
		pnlTools.setBorder(
			new TitledBorder(null, "Tools", TitledBorder.LEADING,
				TitledBorder.TOP, null, null));
		GridBagConstraints gbc_pnlTools = new GridBagConstraints();
		gbc_pnlTools.fill = GridBagConstraints.VERTICAL;
		gbc_pnlTools.insets = new Insets(5, 5, 5, 5);
		gbc_pnlTools.gridx = 0;
		gbc_pnlTools.gridy = 0;
		pnlControls.add(pnlTools, gbc_pnlTools);
		GridBagLayout gbl_pnlTools = new GridBagLayout();
		gbl_pnlTools.columnWidths = new int[] { 30, 30, 0
		};
		gbl_pnlTools.rowHeights = new int[] { 23, 23, 23, 23, 0
		};
		gbl_pnlTools.columnWeights = new double[] { 0.0, 0.0, Double.MIN_VALUE
		};
		gbl_pnlTools.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0,
			Double.MIN_VALUE
		};
		pnlTools.setLayout(gbl_pnlTools);

		JButton btnToolLine = new JButton();
		btnToolLine
			.setIcon(new ImageIcon(getClass().getResource("res/img/line.png")));
		btnToolLine.setPreferredSize(dimBtn);
		btnToolLine.setToolTipText("Line");
		btnToolLine.addMouseListener(toolSelector(btnToolLine, Tool.LINE));
		GridBagConstraints gbc_btnToolLine = new GridBagConstraints();
		gbc_btnToolLine.insets = new Insets(5, 5, 5, 5);
		gbc_btnToolLine.gridx = 0;
		gbc_btnToolLine.gridy = 0;
		pnlTools.add(btnToolLine, gbc_btnToolLine);

		JButton btnToolRectangle = new JButton();
		btnToolRectangle.setIcon(
			new ImageIcon(getClass().getResource("res/img/rectangle.png")));
		btnToolRectangle.setPreferredSize(dimBtn);
		btnToolRectangle.setToolTipText("Rectangle");
		btnToolRectangle
			.addMouseListener(toolSelector(btnToolRectangle, Tool.RECTANGLE));
		GridBagConstraints gbc_btnToolRectangle = new GridBagConstraints();
		gbc_btnToolRectangle.insets = new Insets(5, 5, 5, 5);
		gbc_btnToolRectangle.gridx = 1;
		gbc_btnToolRectangle.gridy = 0;
		pnlTools.add(btnToolRectangle, gbc_btnToolRectangle);

		JButton btnToolCircle = new JButton();
		btnToolCircle.setIcon(
			new ImageIcon(getClass().getResource("res/img/circle.png")));
		btnToolCircle.setPreferredSize(dimBtn);
		btnToolCircle.setToolTipText("Circle");
		btnToolCircle
			.addMouseListener(toolSelector(btnToolCircle, Tool.CIRCLE));
		GridBagConstraints gbc_btnToolCircle = new GridBagConstraints();
		gbc_btnToolCircle.insets = new Insets(5, 5, 5, 5);
		gbc_btnToolCircle.gridx = 0;
		gbc_btnToolCircle.gridy = 1;
		pnlTools.add(btnToolCircle, gbc_btnToolCircle);

		JButton btnToolOval = new JButton();
		btnToolOval
			.setIcon(new ImageIcon(getClass().getResource("res/img/oval.png")));
		btnToolOval.setPreferredSize(dimBtn);
		btnToolOval.setToolTipText("Oval");
		btnToolOval.addMouseListener(toolSelector(btnToolOval, Tool.OVAL));
		GridBagConstraints gbc_btnToolOval = new GridBagConstraints();
		gbc_btnToolOval.insets = new Insets(5, 5, 5, 5);
		gbc_btnToolOval.gridx = 1;
		gbc_btnToolOval.gridy = 1;
		pnlTools.add(btnToolOval, gbc_btnToolOval);

		JButton btnToolFreehand = new JButton();
		btnToolFreehand.setIcon(
			new ImageIcon(getClass().getResource("res/img/freehand.png")));
		btnToolFreehand.setPreferredSize(dimBtn);
		btnToolFreehand.setToolTipText("Freehand");
		btnToolFreehand
			.addMouseListener(toolSelector(btnToolFreehand, Tool.FREEHAND));
		GridBagConstraints gbc_btnToolFreehand = new GridBagConstraints();
		gbc_btnToolFreehand.insets = new Insets(5, 5, 5, 5);
		gbc_btnToolFreehand.gridx = 0;
		gbc_btnToolFreehand.gridy = 2;
		pnlTools.add(btnToolFreehand, gbc_btnToolFreehand);

		JButton btnToolEraser = new JButton();
		btnToolEraser.setIcon(
			new ImageIcon(getClass().getResource("res/img/eraser.png")));
		btnToolEraser.setPreferredSize(dimBtn);
		btnToolEraser.setToolTipText("Eraser");
		btnToolEraser
			.addMouseListener(toolSelector(btnToolEraser, Tool.ERASER));
		GridBagConstraints gbc_btnToolEraser = new GridBagConstraints();
		gbc_btnToolEraser.insets = new Insets(5, 5, 5, 5);
		gbc_btnToolEraser.gridx = 1;
		gbc_btnToolEraser.gridy = 2;
		pnlTools.add(btnToolEraser, gbc_btnToolEraser);

		JButton btnToolText = new JButton();
		btnToolText
			.setIcon(new ImageIcon(getClass().getResource("res/img/text.png")));
		btnToolText.setPreferredSize(dimBtn);
		btnToolText.setToolTipText("Text");
		btnToolText.addMouseListener(toolSelector(btnToolText, Tool.TEXT));
		GridBagConstraints gbc_btnToolText = new GridBagConstraints();
		gbc_btnToolText.insets = new Insets(5, 5, 5, 5);
		gbc_btnToolText.gridx = 0;
		gbc_btnToolText.gridy = 3;
		pnlTools.add(btnToolText, gbc_btnToolText);

		JButton btnToolColorPicker = new JButton();
		btnToolColorPicker.setIcon(
			new ImageIcon(getClass().getResource("res/img/colorPicker.png")));
		btnToolColorPicker.setPreferredSize(dimBtn);
		btnToolColorPicker.setToolTipText("Color Picker");
		btnToolColorPicker.addMouseListener(
			toolSelector(btnToolColorPicker, Tool.COLOR_PICKER));
		GridBagConstraints gbc_btnToolColorPicker = new GridBagConstraints();
		gbc_btnToolColorPicker.insets = new Insets(5, 5, 5, 5);
		gbc_btnToolColorPicker.gridx = 1;
		gbc_btnToolColorPicker.gridy = 3;
		pnlTools.add(btnToolColorPicker, gbc_btnToolColorPicker);

		JPanel pnlSize = new JPanel();
		pnlSize.setBorder(
			new TitledBorder(null, "Size", TitledBorder.LEADING,
				TitledBorder.TOP, null, null));
		GridBagConstraints gbc_pnlSize = new GridBagConstraints();
		gbc_pnlSize.fill = GridBagConstraints.BOTH;
		gbc_pnlSize.insets = new Insets(0, 5, 5, 5);
		gbc_pnlSize.gridx = 0;
		gbc_pnlSize.gridy = 1;
		pnlControls.add(pnlSize, gbc_pnlSize);
		GridBagLayout gbl_pnlSize = new GridBagLayout();
		gbl_pnlSize.columnWidths = new int[] { 32, 0
		};
		gbl_pnlSize.rowHeights = new int[] { 23, 0
		};
		gbl_pnlSize.columnWeights = new double[] { 1.0, Double.MIN_VALUE
		};
		gbl_pnlSize.rowWeights = new double[] { 0.0, Double.MIN_VALUE
		};
		pnlSize.setLayout(gbl_pnlSize);

		spnSize = new JSpinner();
		GridBagConstraints gbc_spnSize = new GridBagConstraints();
		gbc_spnSize.insets = new Insets(5, 5, 5, 5);
		gbc_spnSize.fill = GridBagConstraints.HORIZONTAL;
		gbc_spnSize.gridx = 0;
		gbc_spnSize.gridy = 0;
		pnlSize.add(spnSize, gbc_spnSize);
		spnSize.setToolTipText("Size");
		spnSize.setModel(new SpinnerNumberModel(10, 1, 500, 1));

		JPanel pnlColor = new JPanel();
		pnlColor.setBorder(
			new TitledBorder(null, "Color", TitledBorder.LEADING,
				TitledBorder.TOP, null, null));
		GridBagConstraints gbc_pnlColor = new GridBagConstraints();
		gbc_pnlColor.fill = GridBagConstraints.HORIZONTAL;
		gbc_pnlColor.insets = new Insets(0, 5, 5, 5);
		gbc_pnlColor.gridx = 0;
		gbc_pnlColor.gridy = 2;
		pnlControls.add(pnlColor, gbc_pnlColor);
		GridBagLayout gbl_pnlColor = new GridBagLayout();
		gbl_pnlColor.columnWidths = new int[] { 40, 40, 0
		};
		gbl_pnlColor.rowHeights = new int[] { 0
		};
		gbl_pnlColor.columnWeights = new double[] { 0.0, 0.0, Double.MIN_VALUE
		};
		gbl_pnlColor.rowWeights = new double[] { 0.0
		};
		pnlColor.setLayout(gbl_pnlColor);

		JButton btnColor = new JButton();
		GridBagConstraints gbc_btnColor = new GridBagConstraints();
		gbc_btnColor.insets = new Insets(5, 5, 5, 5);
		gbc_btnColor.gridx = 0;
		gbc_btnColor.gridy = 0;
		pnlColor.add(btnColor, gbc_btnColor);
		btnColor.setIcon(
			new ImageIcon(getClass().getResource("res/img/color.png")));
		btnColor.setPreferredSize(dimBtn);
		btnColor.setToolTipText("Color");
		btnColor.addMouseListener(new MouseAdapter() {
			JDialog dialog = null;
			JColorChooser cc = null;

			@Override
			public void mouseClicked(MouseEvent e) {
				if (dialog == null) {
					cc = new JColorChooser(selectedColor);
					dialog = JColorChooser.createDialog(
						ClientGUI.this,
						"Choose a color",
						true,
						cc,
						null,
						null);
				}

				cc.setColor(selectedColor);

				dialog.setVisible(true);
				dialog.dispose();

				Color newColor = cc.getColor();
				if (newColor != null) {
					setColor(newColor);
				}
			}
		});

		pnlColorSelected = new JPanel();
		GridBagConstraints gbc_pnlColorSelected = new GridBagConstraints();
		pnlColorSelected.setPreferredSize(dimBtn);
		gbc_pnlColorSelected.fill = GridBagConstraints.BOTH;
		gbc_pnlColorSelected.insets = new Insets(5, 5, 5, 5);
		gbc_pnlColorSelected.gridx = 1;
		gbc_pnlColorSelected.gridy = 0;
		pnlColor.add(pnlColorSelected, gbc_pnlColorSelected);
		pnlColorSelected.setBackground(selectedColor);
		pnlColorSelected.setToolTipText("Selected Color");

		pnlActiveUsers = new JPanel();
		getPnlActiveUsers().setBorder(
			new TitledBorder(null, "Active Users", TitledBorder.LEADING,
				TitledBorder.TOP, null, null));
		GridBagConstraints gbc_pnlActiveUsers = new GridBagConstraints();
		gbc_pnlActiveUsers.insets = new Insets(0, 5, 5, 5);
		gbc_pnlActiveUsers.fill = GridBagConstraints.BOTH;
		gbc_pnlActiveUsers.gridx = 0;
		gbc_pnlActiveUsers.gridy = 3;
		pnlControls.add(getPnlActiveUsers(), gbc_pnlActiveUsers);
		GridBagLayout gbl_pnlActiveUsers = new GridBagLayout();
		gbl_pnlActiveUsers.columnWidths = new int[] { 71, 0
		};
		gbl_pnlActiveUsers.rowHeights = new int[] { 0, 0
		};
		gbl_pnlActiveUsers.columnWeights = new double[] { 1.0, Double.MIN_VALUE
		};
		gbl_pnlActiveUsers.rowWeights = new double[] { 1.0, Double.MIN_VALUE
		};
		getPnlActiveUsers().setLayout(gbl_pnlActiveUsers);

		listUsers = new JList<>();
		getListUsers().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		getListUsers().setMaximumSize(new Dimension(34, 0));
		getListUsers().addMouseMotionListener(new MouseMotionListener() {
			@Override
			public void mouseMoved(MouseEvent e) {
				int index = getListUsers().locationToIndex(e.getPoint());

				getListUsers().setToolTipText(
					index >= 0 ? getListUsers().getModel().getElementAt(index)
						: null);
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				mouseMoved(e);
			}
		});

		JScrollPane scrUsers = new JScrollPane(getListUsers());
		GridBagConstraints gbc_scrUsers = new GridBagConstraints();
		gbc_scrUsers.insets = new Insets(5, 5, 5, 5);
		gbc_scrUsers.fill = GridBagConstraints.BOTH;
		gbc_scrUsers.gridx = 0;
		gbc_scrUsers.gridy = 0;
		getPnlActiveUsers().add(scrUsers, gbc_scrUsers);
		scrUsers.setPreferredSize(new Dimension(33, 0));
		scrUsers.setHorizontalScrollBarPolicy(
			JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		pnlBoard = new JPanel() {
			private static final long serialVersionUID = 10L;

			@Override
			public void paintComponent(Graphics g) {
				super.paintComponent(g);

				getBoardLock().lock();
				try {
					BufferedImage board = getBoard();
					if (board != null) {
						g.drawImage(
							board,
							0,
							0,
							board.getWidth(this),
							board.getHeight(this),
							this);
					}

					drawPreview(g);
				} finally {
					getBoardLock().unlock();
				}

				g.dispose();
			}
		};
		pnlBoard.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				repaintBoard();

				// update startX, startY
				if (getSelectedTool().isDragOnly() || startX == null
					|| startY == null) {
					startX = endX = e.getX();
					startY = endY = e.getY();
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				repaintBoard();

				// update endX, endY
				endX = e.getX();
				endY = e.getY();

				if (getSelectedTool().isSingleClickOnly()) {
					sendOnSingleClick();

					return;
				}

				sendOnDrag();

				// if the start and end are the same, this isn't a click
				if (endX.equals(startX) && endY.equals(startY)) {
					return;
				}

				sendOnClick();

				resetXY();
			}

			@Override
			public void mouseExited(MouseEvent e) {
				repaintBoard();

				resetXY();
				mouseOn = false;
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				repaintBoard();

				resetXY();
				mouseOn = true;
			}
		});
		pnlBoard.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				repaintBoard();

				if (!mouseOn) {
					return;
				}

				if (startX == null || startY == null) {
					startX = e.getX();
					startY = e.getY();

					return;
				}

				endX = e.getX();
				endY = e.getY();

				if (sendOnDrag()) {
					startX = endX;
					startY = endY;
				}
			}

			@Override
			public void mouseMoved(MouseEvent e) {
				repaintBoard();

				endX = e.getX();
				endY = e.getY();
			}
		});
		pnlBoard.setBackground(Color.WHITE);
		getContentPane().add(pnlBoard, BorderLayout.CENTER);

		pack();

		resetBoard();
		selectTool(btnToolLine, Tool.LINE);
		setActiveInput(false);
	}

	/**
	 * Shows the user a dialog to confirm closing. If they accept, the program
	 * is terminated
	 */
	public void confirmClose() {
		int option = JOptionPane.showConfirmDialog(
			ClientGUI.this,
			getConfirmCloseMessage(),
			"Confirm Exit",
			JOptionPane.YES_NO_OPTION,
			JOptionPane.WARNING_MESSAGE);

		if (option == JOptionPane.YES_OPTION) {
			dispose();
			System.exit(0);
		}
	}

	/**
	 * Gets the message displayed to the user to confirm closing
	 * 
	 * @return String
	 */
	public String getConfirmCloseMessage() {
		return "Are you sure you want to exit?\n\nYou will need to reconnect if you exit!";
	}

	/**
	 * Sets up the userData Map and listUsers with the supplied JSONArray
	 * 
	 * @param users JSONArray
	 */
	public void setUsers(JSONArray users) {
		synchronized (getListUsers()) {
			getListUsers().clearSelection();

			DefaultListModel<String> listModel = new DefaultListModel<>();
			for (int i = 0; i < users.length(); i++) {
				JSONObject user = users.getJSONObject(i);
				int uuid = user.optInt(Fields.UUID);
				listModel.addElement(
					String.format(
						"%s (%d)",
						user.optString(Fields.USERNAME),
						uuid));
			}
			getListUsers().setModel(listModel);
		}
	}

	public synchronized void setActiveInput(boolean active) {
		this.activeInput = active;
	}

	protected synchronized boolean isActiveInput() {
		return activeInput;
	}

	/*
	 * Selects a tool and disables the button, re-enabling the old selected tool
	 * button
	 */
	public void selectTool(JButton button, Tool tool) {
		if (selectedToolButton != null) {
			selectedToolButton.setEnabled(true);
		}

		if (button != null) {
			button.setEnabled(false);
		}

		selectedToolButton = button;
		selectedTool = tool;
	}

	/**
	 * Gets the current selected tool
	 * 
	 * @return Tool
	 */
	public Tool getSelectedTool() {
		return selectedTool;
	}

	/**
	 * Draws a drawing represented by {@code draw}
	 * 
	 * @param draw JSONObject
	 */
	public void draw(JSONObject draw) {
		Graphics2D g = (Graphics2D) getBoard().getGraphics();

		// assume that most of the fields are there if we need them this greatly
		// simplifies this process
		int x = draw.optInt(Fields.X);
		int y = draw.optInt(Fields.Y);
		int w = draw.optInt(Fields.WIDTH);
		int h = draw.optInt(Fields.HEIGHT);
		int size = draw.optInt(Fields.SIZE);
		int color = draw.optInt(Fields.COLOR);

		g.setColor(new Color(color, true));

		try {
			switch (Tool.valueOf(draw.optString(Fields.TOOL).toUpperCase())) {
			case RECTANGLE:
				g.fillRect(x, y, w, h);
				break;
			case OVAL:
				g.fillOval(x, y, w, h);
				break;
			case CIRCLE:
				g.fillOval(x - size, y - size, 2 * size, 2 * size);
				break;
			case ERASER:
				g.setColor(Color.WHITE);
			case FREEHAND:
			case LINE:
				int x2 = draw.optInt(Fields.X2);
				int y2 = draw.optInt(Fields.Y2);
				g.setStroke(
					new BasicStroke(size, BasicStroke.CAP_ROUND,
						BasicStroke.JOIN_BEVEL));
				g.drawLine(x, y, x2, y2);
				break;
			case TEXT:
				String text = draw.optString(Fields.TEXT);
				g.setFont(new Font("Arial", Font.PLAIN, 2 * size));
				g.drawString(text, x, y);
				break;
			default:
				break;
			}
		} catch (IllegalArgumentException ignored) {
		}

		g.dispose();

		pnlBoard.repaint();
	}

	/**
	 * Draws the image represented by the base 64 encoded imageStr on the canvas
	 * 
	 * @param imageStr String
	 */
	public void setBoard(String imageStr) {
		resetBoard();

		try {
			BufferedImage newImage = ImageIO.read(
				new ByteArrayInputStream(Base64.getDecoder().decode(imageStr)));
			getBoardLock().lock();
			try {
				board.getGraphics().drawImage(
					newImage,
					0,
					0,
					board.getWidth(null),
					board.getHeight(null),
					null);
			} finally {
				getBoardLock().unlock();
			}
		} catch (IOException ignored) {
		}

		repaintBoard();
	}

	/**
	 * Resets the board to be a blank white canvas
	 */
	public void resetBoard() {
		getBoardLock().lock();
		try {
			Dimension dim = pnlBoard.getSize();

			if (getBoard() == null) {
				setBoard(
					new BufferedImage(dim.width, dim.height,
						BufferedImage.TYPE_INT_ARGB));
			}

			Graphics g = getBoard().getGraphics();
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, dim.width, dim.height);
			g.dispose();

			repaintBoard();
		} finally {
			getBoardLock().unlock();
		}
	}

	/**
	 * Checks if all of start/endX/Y are not null
	 * 
	 * @return boolean
	 */
	private boolean isAllXYSet() {
		return startX != null && startY != null && endX != null && endY != null;
	}

	/**
	 * Resets all start/endX/Y to null
	 */
	private void resetXY() {
		startX = startY = endX = endY = null;
	}

	/**
	 * Draws a preview of the users current drawing over the board before it is
	 * sent to the server
	 * 
	 * @param g Graphics
	 */
	public void drawPreview(Graphics g) {
		if (isAllXYSet() && isActiveInput()) {
			int x = Math.min(startX, endX);
			int y = Math.min(startY, endY);
			int w = Math.abs(endX - startX);
			int h = Math.abs(endY - startY);
			int r = (int) Math.sqrt(w * w + h * h);

			getBoardLock().lock();
			try {
				g.setColor(selectedColor);

				switch (selectedTool) {
				case RECTANGLE:
					g.fillRect(x, y, w, h);
					break;
				case OVAL:
					g.fillOval(x, y, w, h);
					break;
				case CIRCLE:
					g.fillOval(startX - r, startY - r, 2 * r, 2 * r);
					break;
				case LINE:
					Graphics2D g2d = (Graphics2D) g;
					g2d.setStroke(
						new BasicStroke((int) spnSize.getValue(),
							BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL));
					g.drawLine(startX, startY, endX, endY);
					break;
				default:
					break;
				}
			} finally {
				getBoardLock().unlock();
			}
		}
	}

	/**
	 * Repaints the board (pnlBoard)
	 */
	public void repaintBoard() {
		pnlBoard.repaint();
	}

	public void setColor(Color color) {
		this.selectedColor = color;
		pnlColorSelected.setBackground(color);
	}

	/**
	 * Sends a draw message for a tool that is "on single click"
	 * 
	 * @return boolean {@code true} if a message is sent
	 */
	public boolean sendOnSingleClick() {
		if (!isAllXYSet()) {
			return false;
		}

		JSONObject drawing = new JSONObject()
			.put(Fields.TOOL, selectedTool.toString().toLowerCase())
			.put(Fields.COLOR, selectedColor.getRGB()).put(Fields.X, endX)
			.put(Fields.Y, endY).put(Fields.SIZE, spnSize.getValue());

		boolean send = true;
		switch (selectedTool) {
		case TEXT:
			String text = JOptionPane.showInputDialog(this, "Input Text");

			if (text != null && !text.isEmpty()) {
				drawing.put(Fields.TEXT, text);
			} else {
				send = false;
			}
			break;
		case COLOR_PICKER:
			setColor(new Color(getBoard().getRGB(endX, endY)));
			send = false;
			break;
		default:
			send = false;
			break;
		}

		if (send) {
			JSONObject message = new JSONObject()
				.put(Fields.COMMAND, Fields.DRAWING)
				.put(Fields.DRAWING, drawing);
			getController().sendToServer(message);
		}

		return send;
	}

	/**
	 * Sends a draw message for a tool that is "on Click"
	 * 
	 * @return boolean {@code true} if a message is sent
	 */
	public boolean sendOnClick() {
		if (!isAllXYSet()) {
			return false;
		}

		int x = Math.min(startX, endX);
		int y = Math.min(startY, endY);
		int w = Math.abs(endX - startX);
		int h = Math.abs(endY - startY);

		JSONObject drawing = new JSONObject()
			.put(Fields.TOOL, selectedTool.toString().toLowerCase())
			.put(Fields.COLOR, selectedColor.getRGB()).put(Fields.X, startX)
			.put(Fields.Y, startY);

		boolean send = true;
		switch (selectedTool) {
		case RECTANGLE:
		case OVAL:
			drawing.put(Fields.HEIGHT, h).put(Fields.WIDTH, w).put(Fields.X, x)
				.put(Fields.Y, y);
			break;
		case CIRCLE:
			drawing.put(Fields.SIZE, (int) Math.sqrt(w * w + h * h));
			break;
		case LINE:
			drawing.put(Fields.X2, endX).put(Fields.Y2, endY)
				.put(Fields.SIZE, spnSize.getValue());
			break;
		default:
			send = false;
			break;
		}

		if (send) {
			JSONObject message = new JSONObject()
				.put(Fields.COMMAND, Fields.DRAWING)
				.put(Fields.DRAWING, drawing);
			getController().sendToServer(message);
		}

		return send;
	}

	/**
	 * Sends a draw message for a tool that is "on drag"
	 * 
	 * @return boolean {@code true} if a message is sent
	 */
	public boolean sendOnDrag() {
		if (!isAllXYSet()) {
			return false;
		}

		JSONObject drawing = new JSONObject()
			.put(Fields.TOOL, selectedTool.toString().toLowerCase())
			.put(Fields.X, startX).put(Fields.Y, startY).put(Fields.X2, endX)
			.put(Fields.Y2, endY).put(Fields.SIZE, spnSize.getValue());

		boolean send = true;
		switch (selectedTool) {
		case FREEHAND:
			drawing.put(Fields.COLOR, selectedColor.getRGB());
		case ERASER:
			break;
		default:
			send = false;
			break;
		}

		if (send) {
			JSONObject message = new JSONObject()
				.put(Fields.COMMAND, Fields.DRAWING)
				.put(Fields.DRAWING, drawing);
			getController().sendToServer(message);
		}

		return send;
	}

	/**
	 * Creates a "tool selector". When a mouse event occurs, the specified
	 * button is disabled and the tool is selected
	 * 
	 * @param button
	 * @param tool
	 * @return
	 */
	public MouseAdapter toolSelector(JButton button, Tool tool) {
		return new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				selectTool(button, tool);
			}
		};
	}

	/**
	 * @return the controller
	 */
	public GUIController getController() {
		return controller;
	}

	/**
	 * @return the mnFile
	 */
	public JMenu getMnFile() {
		return mnFile;
	}

	/**
	 * @return the mntmClose
	 */
	public JMenuItem getMntmClose() {
		return mntmClose;
	}

	/**
	 * @return the listUsers
	 */
	public JList<String> getListUsers() {
		return listUsers;
	}

	/**
	 * @return the pnlActiveUsers
	 */
	public JPanel getPnlActiveUsers() {
		return pnlActiveUsers;
	}

	/**
	 * @return the board
	 */
	public BufferedImage getBoard() {
		return board;
	}

	/**
	 * @param board the board to set
	 */
	public void setBoard(BufferedImage board) {
		this.board = board;
	}

	/**
	 * @return the boardLock
	 */
	public Lock getBoardLock() {
		return boardLock;
	}
}
