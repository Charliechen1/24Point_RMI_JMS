package model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Scanner;

public class OnlineUser {
	
	private HashSet<String> onlineUser;
	
	public OnlineUser(){
		setOnlineUser(new HashSet<String>());
	}

	/**
	 * @return the onlineUser
	 */
	public HashSet<String> getOnlineUser() {
		return onlineUser;
	}

	/**
	 * @param onlineUser the onlineUser to set
	 */
	public void setOnlineUser(HashSet<String> onlineUser) {
		this.onlineUser = onlineUser;
	}

	public void addOnlineUser(String name){
		readAll();
		onlineUser.add(name);
		writeAll();
	}
	
	public void deleteOnlineUser(String name){
		readAll();
		onlineUser.remove(name);
		writeAll();
	}
	
	public boolean ifContainByName(String name){
		return onlineUser.contains(name);
	}
	
	public void writeAll(){
		try {
			FileWriter fw = new FileWriter("OnlineUser.txt");
			Iterator it = onlineUser.iterator();
			while (it.hasNext()){
				String thisOnline = (String)it.next();
				fw.write(thisOnline);
				fw.write("\n");
			}
			fw.close();
		} catch (IOException e) {
			new File("OnlineUser.txt");
		}
	}
	
	public void readAll(){
		onlineUser = new HashSet<String>(); 	
		try {
			Scanner in = new Scanner(new FileReader("OnlineUser.txt"));
			while (in.hasNextLine()){
				onlineUser.add((String)in.nextLine());
			}
			in.close();
		}catch (FileNotFoundException e) {	
			new File("OnlineUser.txt");
		}
	}

}
