package server;

/**
 * An interface used by classes that store user information/interact with a user
 * 
 * Users can be clients (UserClient) or Managers
 */
public interface IUser {
	/**
	 * Gets the username associated with this User
	 * 
	 * @return String
	 */
	public String getUsername();

	/**
	 * Gets the uuid (unique user ID) associated with this user
	 * 
	 * @return int
	 */
	public int getUUID();

	/**
	 * Sends a message to the user
	 * 
	 * @param message String
	 */
	public void send(String message);

	/**
	 * Starts the user. This is used to start communication with the user
	 */
	public void start();

	/**
	 * Returns a flag indicating the the user has been terminated or not
	 * 
	 * @return boolean
	 */
	public boolean isTerminated();

	/**
	 * Terminates the user. This is used to
	 * 
	 * @return boolean. {@code true} if termination was successful
	 */
	public boolean terminate();
}
