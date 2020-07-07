package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONObject;

import common.Fields;

public class ClientUser implements IUser {
	private String username;
	private int uuid;
	private Server server;
	private Socket socket;
	private DataInputStream dis;
	private DataOutputStream dos;
	private Thread input;

	private ExecutorService pool;

	private Boolean terminated = false;
	private Boolean started = false;

	/**
	 * Creates a ClientUser with the specified parameters
	 * 
	 * @param server Server
	 * @param socket Socket
	 * @param uuid   int
	 * 
	 * @throws IOException
	 */
	public ClientUser(Server server, Socket socket, int uuid)
		throws IOException {
		this.server = server;
		this.socket = socket;
		this.uuid = uuid;

		this.dis = new DataInputStream(socket.getInputStream());
		this.dos = new DataOutputStream(socket.getOutputStream());

		this.pool = Executors.newSingleThreadExecutor();

		JSONObject json = new JSONObject(dis.readUTF());
		this.username = json.optString(Fields.USERNAME);

		input = new Thread(() -> {
			while (true) {
				if (isTerminated()) {
					break;
				}

				try {
					String in = dis.readUTF();

					if (!isStarted()) {
						return;
					}

					server.recieve(in, this);
				} catch (IOException ioe) {
					break;
				}
			}

			terminate();
			server.updateUsers();
		});

		input.start();
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public int getUUID() {
		return uuid;
	}

	@Override
	public void send(String message) {
		if (isTerminated()) {
			return;
		}

		pool.execute(() -> {
			try {
				dos.writeUTF(message);
			} catch (IOException ioe) {
				terminate();
				server.updateUsers();
			}
		});
	}

	@Override
	public void start() {
		synchronized (started) {
			started = true;
		}
	}

	@Override
	public boolean terminate() {
		synchronized (terminated) {
			if (terminated) {
				return true;
			}

			terminated = true;

			try {
				input.interrupt();
			} catch (Exception ignored) {
			}
			try {
				dis.close();
			} catch (Exception ignored) {
			}
			try {
				dos.close();
			} catch (Exception ignored) {
			}
			try {
				socket.close();
			} catch (Exception ignored) {
			}
			pool.shutdown();

		}

		return true;
	}

	@Override
	public synchronized boolean isTerminated() {
		return terminated;
	}

	public synchronized boolean isStarted() {
		return started;
	}

//	public void restartPool() {
//		queue.clear();
//	}
}
