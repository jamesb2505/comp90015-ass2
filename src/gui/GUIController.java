package gui;

import javax.swing.SwingUtilities;

import org.json.JSONException;
import org.json.JSONObject;

import common.Fields;

/**
 * A class that controls some underlying GUI
 */
public abstract class GUIController {
	/**
	 * Receives a message from some source, passing it off to the GUI
	 * 
	 * @param message Object
	 */
	public void receive(Object message) {
		SwingUtilities.invokeLater(() -> {
			try {
				JSONObject json;
				if (message instanceof JSONObject) {
					json = (JSONObject) message;
				} else {
					json = new JSONObject(message.toString());
				}

				ClientGUI gui = getGUI();

				switch (json.optString(Fields.COMMAND)) {
				case Fields.USERS:
					gui.setUsers(json.optJSONArray(Fields.USERS));
					break;
				case Fields.DRAWING:
					gui.draw(json.optJSONObject(Fields.DRAWING));
					break;
				case Fields.BOARD:
					gui.setBoard(json.optString(Fields.BOARD));
					break;
				}
			} catch (JSONException je) {
				System.out.format("JSON parse error: %s\n", je.getMessage());
			}
		});
	}

	/**
	 * Sends a message to the server
	 * 
	 * @param message Object
	 */
	public abstract void sendToServer(Object message);

	/**
	 * Gets the gui associated with this controller
	 * 
	 * @return ClientGUI
	 */
	public abstract ClientGUI getGUI();

	/**
	 * Starts the GUI
	 */
	public abstract void start();
}
