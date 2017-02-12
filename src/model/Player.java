package model;

import java.io.Serializable;

public class Player implements Serializable, Comparable<Player>{
	private String name;
	private String password;
	private int wonGames;
	private int playedGames;
	private double avgWinTime;
	private int rank;
	
	public Player(String n, String p){
		name = n;
		password = p;
		wonGames = 0;
		playedGames = 0;
		avgWinTime = 0.0;
	}
	
	public Player(String n, int w, int pg, double awt){
		name = n;
		password = null;
		wonGames = w;
		playedGames = pg;
		avgWinTime = awt;
	}
	
	public Player(String n, String p, int w, int pg, double awt){
		name = n;
		password = p;
		wonGames = w;
		playedGames = pg;
		avgWinTime = awt;
	}
	
	public Player(String n, String p, int w, int pg, double awt, int r){
		name = n;
		password = p;
		wonGames = w;
		playedGames = pg;
		avgWinTime = awt;
		rank = r;
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	
	public int getWonGames() {
		return wonGames;
	}
	public void setWonGames(int wonGames) {
		this.wonGames = wonGames;
	}
	
	public int getPlayedGames() {
		return playedGames;
	}
	public void setPlayedGames(int playedGames) {
		this.playedGames = playedGames;
	}
	
	public double getAvgWinTime() {
		return avgWinTime;
	}
	public void setAvgWinTime(double avgWinTime) {
		this.avgWinTime = avgWinTime;
	}

	/**
	 * @return the rank
	 */
	public int getRank() {
		return rank;
	}

	/**
	 * @param rank the rank to set
	 */
	public void setRank(int rank) {
		this.rank = rank;
	}

	@Override
	public int compareTo(Player o) {
		// TODO Auto-generated method stub
		if (wonGames < o.getWonGames())
			return 1;
		if (wonGames > o.getWonGames())
			return -1;
		if (wonGames ==  o.getWonGames()){
			if (playedGames < o.getPlayedGames())
				return -1;
			if (playedGames > o.getPlayedGames())
				return 1;
			else
				return 0;
		}
		return 0;
	}	
}
