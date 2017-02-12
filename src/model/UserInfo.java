package model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;

public class UserInfo {
	
	private HashMap<String, Player> allCurrPlayer;
	
	public UserInfo(){
		allCurrPlayer = new HashMap<String,  Player>();
	}
	
	public UserInfo(HashMap<String, Player> cp){
		allCurrPlayer = cp;
	}
	
	public HashMap<String, Player> getAllCurrPlayer(){
		return allCurrPlayer;
	}
	
	public void setAllCurrPlayer(HashMap<String, Player> cp){
		allCurrPlayer = cp;
	}
	
	public void addUser(Player p){
		allCurrPlayer.put(p.getName(), p);
		setRank(p);
		try {
			// currently using file io operation, later will be altered by DB
			FileWriter fw = new FileWriter("UserInfo.txt", true);
			String thisInfoLine = "";
			thisInfoLine += p.getName() + " ";
			thisInfoLine += p.getPassword() + " ";
			thisInfoLine += p.getWonGames() + " ";
			thisInfoLine += p.getPlayedGames() + " ";
			thisInfoLine += p.getAvgWinTime() + "\n";
			fw.append(thisInfoLine);
			fw.flush();
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			new File("UserInfo.txt");
		}
	}
	
	public HashMap<String, Player> queryAll(){
		try {
			allCurrPlayer = new HashMap<String, Player>();
			Scanner in = new Scanner(new FileReader("UserInfo.txt"));
			while (in.hasNextLine()){
				String[] thisUserInfo = in.nextLine().split(" ");
				// currently use a map of map to store the information, later will change to sql
				Player p = new Player(thisUserInfo[0], 
						thisUserInfo[1],
						Integer.parseInt(thisUserInfo[2]), 
						Integer.parseInt(thisUserInfo[3]), 
						Double.parseDouble(thisUserInfo[4]));
				allCurrPlayer.put(thisUserInfo[0],  p);
			}
		} catch (FileNotFoundException e) {
			new File("UserInfo.txt");
		}
		return allCurrPlayer;
	}
	
	public Player queryByName(String name){
		Player p = allCurrPlayer.get(name);
		if (p != null){
			setRank(p);
		}
		return p;
	}

	public boolean ifContainByName(String name){
		return allCurrPlayer.containsKey(name);
	}
	
	public String passwordByName(String name){
		Player p = allCurrPlayer.get(name);
		if (p != null) 
			return p.getPassword();
		return null;
	}
	
	public ArrayList<Player> querySortedAll(){
		ArrayList<Player> others = new ArrayList<Player>();
		Iterator it = allCurrPlayer.entrySet().iterator();
		while (it.hasNext()){
			HashMap.Entry pair = (HashMap.Entry)it.next();
			Player thisP = (Player)pair.getValue();
				// here rank is computed by Java, later will be get directed from the DB
			setRank(thisP);
			others.add(thisP);
		}
		Collections.sort(others);
		return others;
	}
	
	public void setRank(Player p){
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
}
