package gui;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.json.JSONArray;
import org.json.JSONObject;

import common.Fields;
import server.Manager;

public class ManagerGUI extends ClientGUI {
	private static final long serialVersionUID = -1805283715841095762L;

	private static final String EXT_WB = "wb";
	private static final String DOT_EXT_WB = "." + EXT_WB;
	private static final String ACTUAL_FILE_TYPE = "png";

	private static final String TITLE_FORMAT_MANAGER = TITLE_FORMAT
		+ " - Manager";

	private File currentFile = null;

	private Map<Integer, Integer> userUUIDs;

	/**
	 * Creates a ManagerGUI with specified attributes
	 * 
	 * @param manager Manager
	 */
	public ManagerGUI(Manager manager, String ip, int port) {
		super(manager, ip, port);

		setTitle(String.format(TITLE_FORMAT_MANAGER, ip, port));

		this.userUUIDs = new HashMap<>();

		JMenuItem mntmNew = new JMenuItem("New");
		mntmNew.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JSONObject json = new JSONObject()
					.put(Fields.COMMAND, Fields.BOARD);
				getController().sendToServer(json);
			}
		});
		getMnFile().add(mntmNew);

		JMenuItem mntmSave = new JMenuItem("Save");

		JMenuItem mntmOpen = new JMenuItem("Open...");
		mntmOpen.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser fc = initFileChooser();

				int opt = fc.showOpenDialog(ManagerGUI.this);
				if (opt == JFileChooser.APPROVE_OPTION) {
					currentFile = fc.getSelectedFile();

					mntmSave.setEnabled(true);

					try {
						BufferedImage image = ImageIO.read(currentFile);

						JSONObject json = new JSONObject()
							.put(Fields.COMMAND, Fields.BOARD)
							.put(Fields.BOARD, getBoardString(image));
						getController().sendToServer(json);
					} catch (IOException | IllegalArgumentException ignored) {
						JOptionPane.showMessageDialog(
							ManagerGUI.this,
							"There was an error opening the file.",
							"Error",
							JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		});
		getMnFile().add(mntmOpen);

		mntmSave.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveBoardToFile();

				mntmSave.setEnabled(true);
			}
		});
		mntmSave.setEnabled(false);
		getMnFile().add(mntmSave);

		JMenuItem mntmSaveAs = new JMenuItem("Save As...");
		mntmSaveAs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser fc = initFileChooser();

				int opt = fc.showSaveDialog(ManagerGUI.this);
				if (opt == JFileChooser.APPROVE_OPTION) {
					currentFile = fc.getSelectedFile();

					mntmSave.setEnabled(true);

					saveBoardToFile();
				}
			}
		});
		getMnFile().add(mntmSaveAs);

		JSeparator separator = new JSeparator();
		getMnFile().add(separator);

		getMnFile().remove(getMntmClose());
		getMnFile().add(getMntmClose());

		JButton btnKick = new JButton("Kick");
		btnKick.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				int index = getListUsers().getSelectedIndex();

				synchronized (userUUIDs) {
					if (index >= 0 && userUUIDs.containsKey(index)) {
						manager.kick(userUUIDs.get(index));
					}
				}
			}
		});
		GridBagConstraints gbc_btnKick = new GridBagConstraints();
		gbc_btnKick.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnKick.insets = new Insets(0, 5, 5, 5);
		gbc_btnKick.gridx = 0;
		gbc_btnKick.gridy = 1;
		getPnlActiveUsers().add(btnKick, gbc_btnKick);

		pack();
		
		setActiveInput(true);
	}

	/**
	 * Gets the base 64 representation of the supplied image
	 * 
	 * @param image BufferedImage
	 * @return String {@code null} if an error occurred
	 */
	public String getBoardString(BufferedImage image) {
		String imageStr = null;

		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO
				.write(image, ACTUAL_FILE_TYPE, Base64.getEncoder().wrap(baos));
			imageStr = baos.toString();
			baos.close();
		} catch (IOException | IllegalArgumentException e) {
			System.out
				.format("Error getting image string: %s\n", e.getMessage());
		}

		return imageStr;
	}

	/**
	 * Gets the base 64 representation of the ManagerGUI's board
	 * 
	 * @return String
	 */
	public String getBoardString() {
		String boardString;

		getBoardLock().lock();
		try {
			boardString = getBoardString(getBoard());
		} finally {
			getBoardLock().unlock();
		}

		return boardString;
	}

	@Override
	public String getConfirmCloseMessage() {
		return "Are you sure you want to close the whiteboard?\n\n"
			+ "All unsaved progress will be lost!";
	}

	/**
	 * Initializes a JFileChooser with it's directory at {@code currentFile}
	 * 
	 * @return JFileChooser
	 */
	public JFileChooser initFileChooser() {
		JFileChooser fc = new JFileChooser(currentFile);
		fc.removeChoosableFileFilter(fc.getAcceptAllFileFilter());
		fc.setFileFilter(
			new FileNameExtensionFilter(
				String.format("Whiteboard (*%s)", DOT_EXT_WB), EXT_WB));

		return fc;
	}

	/**
	 * Save the ManagerGUI's image to the currentFile
	 */
	public void saveBoardToFile() {
		if (currentFile == null) {
			return;
		}

		try {
			if (!currentFile.getName().endsWith(DOT_EXT_WB)) {
				currentFile = new File(
					currentFile.getAbsolutePath() + DOT_EXT_WB);
			}

			getBoardLock().lock();
			try {
				ImageIO.write(getBoard(), ACTUAL_FILE_TYPE, currentFile);
			} finally {
				getBoardLock().unlock();
			}
		} catch (IOException e1) {
			JOptionPane.showMessageDialog(
				ManagerGUI.this,
				"There was an error saving the file.",
				"Error",
				JOptionPane.ERROR_MESSAGE);

			return;
		}
	}

	public void setUsers(JSONArray users) {
		super.setUsers(users);

		synchronized (userUUIDs) {
			userUUIDs.clear();
			for (int i = 0; i < users.length(); i++) {
				JSONObject user = users.getJSONObject(i);
				int uuid = user.optInt(Fields.UUID);
				userUUIDs.put(i, uuid);
			}
		}
	}
}
