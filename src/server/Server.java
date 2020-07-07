package server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import common.Fields;

public class Server {
	private static final int CONNECTION_BACKLOG = 50;

	private final int port;
	private List<IUser> users;
	private Integer nextUUID;
	private Manager manager;
	private final String ip;
	private final String managerUsername;

	private Lock userLock = new ReentrantLock();

	/**
	 * Creates the Server with the specified parameters
	 * 
	 * @param ip              IP address to host the server on
	 * @param port            port to receive connections on
	 * @param managerUsername username of the manager
	 */
	public Server(String ip, int port, String managerUsername) {
		this.ip = ip;
		this.port = port;
		this.managerUsername = managerUsername;
		this.users = new ArrayList<>();
		this.nextUUID = 0;
	}

	public static void main(String args[]) {
		if (args.length != 3) {
			System.err.println("usage: <ip> <port> <username>");
			System.exit(1);
		}

		String ip = args[0];
		String portStr = args[1];
		String managerUsername = args[2];

		try {
			new Server(ip, Integer.parseInt(portStr), managerUsername).start();
		} catch (NumberFormatException nfe) {
			System.err.format("Number format error: %s\n", portStr);
		}
	}

	/**
	 * Starts the Server
	 */
	public void start() {
		try {
			final ServerSocket server = new ServerSocket(port,
				CONNECTION_BACKLOG, InetAddress.getByName(ip));

			System.out.format(
				"Whiteboard server now running at %s:%d\n",
				server.getInetAddress().getHostAddress(),
				port);

			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				try {
					server.close();
				} catch (Exception ignored) {
				}
			}));

			this.manager = new Manager(this, managerUsername, nextUUID++);
			addUser(manager);

			// start manager in their own thread
			new Thread(() -> {
				manager.start();
			}).start();

			while (true) {
				try {
					Socket client = server.accept();

					// start client in their own thread
					new Thread(() -> {
						addUser(client);
					}).start();
				} catch (IOException ignored) {
				}
			}
		} catch (IOException ioe) {
			System.err.format(
				"There was an error setting up the server (%s)\n",
				ioe.getMessage());
			System.exit(1);
		}
	}

	/**
	 * Creates and adds a new User for the given Socket, and if the manager
	 * accepts their connection, starts the User
	 * 
	 * @param socket Socket
	 */
	public void addUser(Socket socket) {
		int uuid;
		synchronized (nextUUID) {
			uuid = nextUUID++;
		}

		try {
			ClientUser user = new ClientUser(this, socket, uuid);

			if (manager.checkAcceptNewUser(user.getUsername())) {
				addUser(user);
				user.send(getJSONBoardMessage());
				broadcast(getUserListMessage());

				user.start();
			} else {
				user.terminate();
			}
		} catch (IOException ioe) {
			System.err.format(
				"There was an error with a client (%s)\n",
				ioe.getMessage());
		}
	}

	/**
	 * Adds a user to the list of users and broadcasts the user list
	 * 
	 * @param user User
	 */
	public void addUser(IUser user) {
		userLock.lock();
		try {
			users.add(user);
		} finally {
			userLock.unlock();
		}
		broadcast(getUserListMessage());
	}

	/**
	 * Removes all users from the list that are to be removed and broadcasts the
	 * user list if changes are made
	 */
	public void updateUsers() {
		userLock.lock();
		try {
			if (users.removeIf(IUser::isTerminated)) {
				broadcast(getUserListMessage());
			}
		} finally {
			userLock.unlock();
		}
	}

	/**
	 * Sends a message to all connected users, and updates the user list
	 * 
	 * @param message String
	 */
	public void broadcast(String message) {
		userLock.lock();
		try {
			users.forEach(user -> {
				user.send(message);
			});

			updateUsers();
		} finally {
			userLock.unlock();
		}
	}

	/**
	 * Broadcasts the user list
	 */
	public String getUserListMessage() {
		List<JSONObject> usernames = new ArrayList<>();

		userLock.lock();
		try {
			for (IUser user : users) {
				usernames.add(
					new JSONObject().put(Fields.USERNAME, user.getUsername())
						.put(Fields.UUID, user.getUUID()));
			}
		} finally {
			userLock.unlock();
		}

		return new JSONObject().put(Fields.COMMAND, Fields.USERS)
			.put(Fields.USERS, new JSONArray(usernames)).toString();
	}

	/**
	 * Receives a message from a user. If the message is not to resync, the
	 * server broadcasts the message.
	 * 
	 * @param message String
	 * @param user    User
	 */
	public void recieve(String message, IUser user) {
		System.out.format(
			"%s/%s (%d): %s\n",
			user instanceof Manager ? "manager" : "user",
			user.getUsername(),
			user.getUUID(),
			message);

		try {
			JSONObject json = new JSONObject(message);

			switch (json.optString(Fields.COMMAND)) {
			case Fields.RESYNC:
				if (user instanceof Manager) {
					broadcast(getJSONBoardMessage());
					broadcast(getUserListMessage());
				} else {
					user.send(getJSONBoardMessage());
					user.send(getUserListMessage());
				}
				break;
			default:
				broadcast(message);
				break;
			}
		} catch (JSONException ignored) {
		}
	}

	/**
	 * Terminates all users with the specified uuid, and updates the user list
	 * if any were terminated
	 * 
	 * @param uuid int
	 */
	public void kick(int uuid) {
		userLock.lock();
		try {
			boolean update = false;

			for (IUser user : users) {
				if (user.getUUID() == uuid) {
					update |= user.terminate();
				}
			}

			if (update) {
				updateUsers();
			}
		} finally {
			userLock.unlock();
		}
	}

	/**
	 * Creates a JSONObject (as a String) containing the "board" message
	 * 
	 * @return String
	 */
	public String getJSONBoardMessage() {
		return new JSONObject().put(Fields.COMMAND, Fields.BOARD)
			.put(Fields.BOARD, manager.getBoardString()).toString();
	}

	/**
	 * Gets the Server's IP address
	 * 
	 * @return String
	 */
	public String getIP() {
		return ip;
	}

	/**
	 * Gets the Server's port number
	 * 
	 * @return int
	 */
	public int getPort() {
		return port;
	}
}
