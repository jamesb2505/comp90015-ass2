package client;

import java.awt.EventQueue;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JOptionPane;

import org.json.JSONObject;

import common.Fields;
import gui.ClientGUI;
import gui.GUIController;

public class Client extends GUIController {

	private String ip;
	private int port;
	private String username;
	private ClientGUI gui;
	private DataOutputStream dos;
	private DataInputStream dis;
	private Socket socket;
	private Thread input;

	private Boolean active = true;

	private Lock sendLock = new ReentrantLock();

	/**
	 * Creates a Client with the associated attributes
	 * 
	 * @param ip       String
	 * @param port     int
	 * @param username String
	 */
	public Client(String ip, int port, String username) {
		this.ip = ip;
		this.port = port;
		this.username = username;
	}

	public static void main(String args[]) {
		if (args.length != 3) {
			System.err.println("usage: <ip> <port> <username>");
			System.exit(1);
		}

		String ip = args[0];
		String portStr = args[1];
		String username = args[2];

		int port = 0;
		try {
			port = Integer.parseInt(portStr);
		} catch (NumberFormatException nfe) {
			System.err.format(
				"There was an error in your entered port (%s)\n",
				portStr);
			System.exit(1);
		}

		Client client = new Client(ip, port, username);
		client.start();
	}

	/**
	 * Starts the Client
	 */
	public void start() {
		try {
			this.socket = new Socket(ip, port);
			this.dos = new DataOutputStream(socket.getOutputStream());
			this.dis = new DataInputStream(socket.getInputStream());

			sendToServer(
				new JSONObject().put(Fields.COMMAND, Fields.USERNAME)
					.put(Fields.USERNAME, username));

			this.gui = new ClientGUI(this, ip, port);

			setActive(false);

			// accept input in separate thread
			this.input = new Thread(() -> {
				while (true) {
					try {
						String in = dis.readUTF();
						setActive(true);
						receive(in);
					} catch (IOException ioe) {
						showErrorDialog(ioe);
						break;
					}
				}

				showErrorDialog(null);
			});

			EventQueue.invokeLater(() -> {
				try {
					gui.setVisible(true);
					input.start();
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		} catch (IOException ioe) {
			System.err.format(
				"There was an error setting up the socket (%s)\n",
				ioe.getMessage());
			System.exit(1);
		}
	}

	/**
	 * Checks if the active flag is set
	 * 
	 * @return boolean
	 */
	public synchronized boolean isActive() {
		return active;
	}

	/**
	 * Sets the active flag and updates the title and GUI
	 * 
	 * @param active boolean
	 */
	public synchronized void setActive(boolean active) {
		this.active = active;

		if (gui != null) {
			gui.setTitle(
				String.format(
					active ? ClientGUI.TITLE_FORMAT
						: ClientGUI.TITLE_FORMAT_CONNECTING,
					ip,
					port));
			gui.setActiveInput(active);
		}
	}

	/**
	 * Displays an error dialog to the user and shuts down
	 * 
	 * @param e Exception
	 */
	private void showErrorDialog(Exception e) {
		String message = e == null ? null : e.getMessage();

		JOptionPane.showMessageDialog(
			gui,
			"It looks like your connection with the server was lost.\n\n"
				+ (message != null && !message.isEmpty() ? "Error: " + message
					: "Maybe you were kicked."),
			"Uh Oh!",
			JOptionPane.ERROR_MESSAGE);

		System.exit(0);
	}

	@Override
	public void sendToServer(Object message) {
		if (!isActive()) {
			return;
		}

		sendLock.lock();
		try {
			try {
				dos.writeUTF(message.toString());
			} catch (IOException ioe) {
				showErrorDialog(ioe);
				try {
					input.interrupt();
				} catch (SecurityException ignored) {
				}
			}
		} finally {
			sendLock.unlock();
		}
	}

	@Override
	public ClientGUI getGUI() {
		return gui;
	}
}
