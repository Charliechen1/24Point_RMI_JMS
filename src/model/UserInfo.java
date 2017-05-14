/**********************************************************************


												Author: Chen Jiali 
												UID: 3035085695


***********************************************************************/
package server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Scanner;

public class UserInfo {
	// DB connection information
	private static final String DB_HOST = "sophia";
	private static final String DB_USER = "jchen";
	private static final String DB_PASS = "PNeufFgv";
	private static final String DB_NAME = "jchen";
	private Connection conn;
	
	private HashMap<String, Player> allCurrPlayer;
	private HashSet<String> onlineUser;
	
	/**
	 * function to establish connection with the sophia server
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	private void establishConn() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException{
		Class.forName("com.mysql.jdbc.Driver").newInstance();
		conn = DriverManager.getConnection("jdbc:mysql://"+DB_HOST+
				"/"+DB_NAME+
				"?user="+DB_USER+
				"&password="+DB_PASS);
		System.out.println("[DEBUG] Database connection successful.");
	}
	
	public UserInfo() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException{
		allCurrPlayer = new HashMap<String,  Player>();
		establishConn();
	}
	
	public UserInfo(HashMap<String, Player> cp) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException{
		allCurrPlayer = cp;
		establishConn();
	}
	
	public HashMap<String, Player> getAllCurrPlayer(){
		return allCurrPlayer;
	}
	
	/**
	 * function for adding a user
	 * @param p
	 */
	public void addUser(Player p){
		allCurrPlayer.put(p.getName(), p);
		setRank(p);
		try {
			PreparedStatement stmt = conn.prepareStatement("INSERT INTO UserInfo (username, password, wonGames, playedGames, avgTime, online) "
					+ "VALUES (?, ?, ?, ?, ?, ?)");

			stmt.setString(1, p.getName());
			stmt.setString(2, p.getPassword());
			stmt.setInt(3, 0);
			stmt.setInt(4, 0);
			stmt.setDouble(5, 0);
			stmt.setBoolean(6, true);
			stmt.execute();

			System.out.println("[DEBUG] Record created");

		} catch (SQLException | IllegalArgumentException e) {
			System.err.println("Error inserting record: "+e);
		}

	}
	
	/**
	 * function to fetch information for all player
	 * @return
	 */
	public HashMap<String, Player> queryAll(){
		try {
			PreparedStatement stmt = conn.prepareStatement("SELECT * FROM UserInfo");
					
			ResultSet rs = stmt.executeQuery();
			while(rs.next()) {
				Player p = new Player(rs.getString(1), 
						rs.getString(2),
						rs.getInt(3), 
						rs.getInt(4), 
						rs.getDouble(5)); 
				allCurrPlayer.put(p.getName(),  p);
				
			} 
		} catch (SQLException e) {
			System.err.println("Error reading record: "+e);
		}

		return allCurrPlayer;
	}
	
	/**
	 * function to query a user by name
	 * @param name
	 * @return
	 */
	public Player queryByName(String name){
		Player p=null;
		try {
			PreparedStatement stmt = conn.prepareStatement("SELECT * FROM UserInfo WHERE username=?");
			stmt.setString(1, name);
			ResultSet rs = stmt.executeQuery();
			if(rs.next()) {
				p = new Player(rs.getString(1), 
						rs.getString(2),
						rs.getInt(3), 
						rs.getInt(4), 
						rs.getDouble(5)); 
				setRank(p);
			} 
		} catch (SQLException e) {
			System.err.println("Error reading record: "+e);
		}
		return p;
	}

	/**
	 * check whether a username exists
	 * @param name
	 * @return
	 */
	public boolean ifContainByName(String name){
		return queryByName(name) != null;
	}
	
	/**
	 * get password of an account
	 * @param name
	 * @return
	 */
	public String passwordByName(String name){
		Player p = queryByName(name);
		if (p != null) 
			return p.getPassword();
		return null;
	}
	
	public ArrayList<Player> querySortedAll(){
		queryAll();
		ArrayList<Player> others = new ArrayList<Player>();
		Iterator it = allCurrPlayer.entrySet().iterator();
		while (it.hasNext()){
			HashMap.Entry pair = (HashMap.Entry)it.next();
			Player thisP = (Player)pair.getValue();
			setRank(thisP);
			others.add(thisP);
		}
		Collections.sort(others);
		return others;
	}
	
	/**
	 * Set current rank
	 * @param p
	 */
	public void setRank(Player p){
		queryAll();
		int rank = 1;
		Iterator it = allCurrPlayer.entrySet().iterator();
		while (it.hasNext()){
			HashMap.Entry pair = (HashMap.Entry)it.next();
			Player o = (Player)pair.getValue();
			if (p.compareTo(o) > 0){
				rank ++;
			}
		}
		p.setRank(rank);
	}
	
	/**
	 * function to increment the number of won games of an account
	 * @param name
	 */
	public void updateWonGame(String name){
		Player p = queryByName(name);
		try {
			PreparedStatement stmt = conn.prepareStatement("UPDATE UserInfo SET wonGames = ? WHERE username = ?");
			p.setWonGames(p.getWonGames()+1);
			stmt.setInt(1, p.getWonGames());
			stmt.setString(2, name);
					
			int rows = stmt.executeUpdate();
			if(rows > 0) {
				System.out.println("[DEBUG] wonGames of "+name+" updated");
			} else {
				System.out.println("[DEBUG] " + name+" not found!");
			}
		} catch (SQLException e) {
			System.err.println("Error reading record: "+e);
		}
	}
	
	/**
	 * function to increment the number of played games of an account
	 * @param name
	 */
	public void updatePlayedGame(String name){
		Player p = queryByName(name);
		try {
			PreparedStatement stmt = conn.prepareStatement("UPDATE UserInfo SET playedGames = ? WHERE username = ?");
			p.setPlayedGames(p.getPlayedGames()+1);
			stmt.setInt(1, p.getPlayedGames());
			stmt.setString(2, name);
					
			int rows = stmt.executeUpdate();
			if(rows > 0) {
				System.out.println("[DEBUG] playedGames of "+name+" updated");
			} else {
				System.out.println("[DEBUG] " + name+" not found!");
			}
		} catch (SQLException e) {
			System.err.println("Error reading record: "+e);
		}
	}
	
	/**
	 * update average win time for the winner
	 * @param name
	 * @param time
	 */
	public void updateWonTime(String name, double time){
		Player p = queryByName(name);
		try {
			PreparedStatement stmt = conn.prepareStatement("UPDATE UserInfo SET avgTime = ? WHERE username = ?");
			p.setPlayedGames(p.getPlayedGames()+1);
			double newTime = (p.getAvgWinTime() * p.getWonGames() + time)/(p.getWonGames()+1);
			stmt.setDouble(1, newTime);
			stmt.setString(2, name);
					
			int rows = stmt.executeUpdate();
			if(rows > 0) {
				System.out.println("[DEBUG] average wining time of "+name+" updated");
			} else {
				System.out.println("[DEBUG] " + name+" not found!");
			}
		} catch (SQLException e) {
			System.err.println("Error reading record: "+e);
		}
	}
	
	/**
	 * Read all online user
	 */
	public void onlineReadAll(){
		onlineUser = new HashSet<String>(); 	
		try {
			PreparedStatement stmt = conn.prepareStatement("SELECT username FROM UserInfo WHERE online = true");
					
			ResultSet rs = stmt.executeQuery();
			while(rs.next()) {
				onlineUser.add(rs.getString(1));
			} 
		} catch (SQLException e) {
			System.err.println("Error reading record: "+e);
		}
	}
	
	/**
	 * function to deal with userlogin
	 * @param name
	 */
	public void addOnlineUser(String name){
		try {
			PreparedStatement stmt = conn.prepareStatement("UPDATE UserInfo SET online = true WHERE username = ?");

			stmt.setString(1, name);
					
			int rows = stmt.executeUpdate();
			if(rows > 0) {
				System.out.println("[DEBUG] "+name+" is online");
			} else {
				System.out.println("[DEBUG] " + name+" not found!");
			}
		} catch (SQLException e) {
			System.err.println("Error reading record: "+e);
		}
		onlineReadAll();
	}
	
	/**
	 * function to deal with user logout
	 * @param name
	 */
	public void deleteOnlineUser(String name){
		try {
			PreparedStatement stmt = conn.prepareStatement("UPDATE UserInfo SET online = false WHERE username = ?");

			stmt.setString(1, name);
					
			int rows = stmt.executeUpdate();
			if(rows > 0) {
				System.out.println("[DEBUG] "+name+" is offline");
			} else {
				System.out.println("[DEBUG] " + name+" not found!");
			}
		} catch (SQLException e) {
			System.err.println("Error reading record: "+e);
		}
		onlineReadAll();
	}
	
	/**
	 * check whether an user is online
	 * @param name
	 * @return
	 */
	public boolean ifOnline(String name){
		onlineReadAll();
		return onlineUser.contains(name);
	}
}
