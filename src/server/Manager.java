package server;

import javax.swing.JOptionPane;

import org.json.JSONException;
import org.json.JSONObject;

import common.Fields;
import gui.ClientGUI;
import gui.GUIController;
import gui.ManagerGUI;

public class Manager extends GUIController implements IUser {
	private String username;
	private int uuid;
	private Server server;
	private ManagerGUI gui;

	/**
	 * Creates a Manager with the specified parameters
	 * 
	 * @param server   Server
	 * @param username String
	 * @param uuid     int
	 */
	public Manager(Server server, String username, int uuid) {
		this.server = server;
		this.username = username;
		this.uuid = uuid;
		this.gui = new ManagerGUI(this, server.getIP(), server.getPort());
	}

	@Override
	public String getUsername() {
		return username;
	}

	/**
	 * Sends a message to the manager. As the manager is hosted locally, this
	 * message is passed directly to the GUI
	 */
	@Override
	public void send(String message) {
		receive(message);
	}

	@Override
	public int getUUID() {
		return uuid;
	}

	/**
	 * Asks the manager if they want to accept a new user
	 * 
	 * @param username String
	 * 
	 * @return boolean, true if accepted
	 */
	public boolean checkAcceptNewUser(String username) {
		return JOptionPane.showConfirmDialog(
			gui,
			String.format(
				"Do you want to allow %s to join the whiteboard?",
				username),
			"New Connection",
			JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
	}

	/**
	 * Sends a message directly to the server
	 * 
	 * @param message Object
	 */
	public void sendToServer(Object message) {
		server.recieve(message.toString(), this);
	}

	/**
	 * Managers can never be terminated, This always returns {@code false}
	 */
	@Override
	public boolean isTerminated() {
		return false;
	}

	/**
	 * Displays the Mnager's GUI
	 */
	@Override
	public void start() {
		gui.setVisible(true);
	}

	@Override
	public ClientGUI getGUI() {
		return gui;
	}

	/**
	 * Kicks users from the server with associated uuid
	 * 
	 * @param uuid Integer
	 */
	public void kick(Integer uuid) {
		if (uuid == null) {
			return;
		}
		server.kick(uuid);
	}

	/**
	 * Gets the base 64 representation of the current state of the manager's
	 * board
	 * 
	 * @return String
	 */
	public String getBoardString() {
		return gui.getBoardString();
	}

	/**
	 * Manager's can never be terminated. always returns {@code false}
	 */
	@Override
	public boolean terminate() {
		return false;
	}

	public void receive(String message) {
		try {
			JSONObject json = new JSONObject(message);
			if (Fields.RESYNC.equals(json.optString(Fields.COMMAND))) {
				return;
			}
		} catch (JSONException ignored) {
			return;
		}

		super.receive(message);
	}
}
