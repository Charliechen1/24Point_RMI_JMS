package server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.*;
import java.rmi.server.*;
import java.util.*;
import model.*;

public class GameServerImpl extends UnicastRemoteObject 
					implements GameServer{
	private UserInfo userInfo;
	private OnlineUser onlineUser;
	private HashMap<String, Object> resMap;
	
	protected GameServerImpl() throws RemoteException {
		super();
		// Here is the construction of the map/set objects which will be used later
		userInfo = new UserInfo();
		onlineUser = new OnlineUser(); 	
		// initialize the OnlineUser.txt file
		onlineUser.writeAll();
		// Currently the current user information is read from a txt file, later it will 
		// replace by JDBC operations
		userInfo.queryAll();
	}

	public static void main(String[] args) {
		try{
			GameServerImpl app = new GameServerImpl();
			System.setSecurityManager(new SecurityManager());
			Naming.rebind("GameServer", app);
			System.out.println("Service registered");
		} catch(Exception e) {
			System.err.println("Exception thrown: "+e);
		}
	}

	@Override
	public HashMap<String, Object> userLogin(String name, String password) {
		resMap = new HashMap<String, Object>();
		if (!userInfo.ifContainByName(name)){
			resMap.put("result", "user not found");
			return resMap;
		}
		
		if (!userInfo.passwordByName(name).equals(password)){	
			resMap.put("result", "wrong password");
			return resMap;
		}
		
		onlineUser.readAll();
		if (onlineUser.ifContainByName(name)){
			resMap.put("result", "user already log in");
			return resMap;
		}
		onlineUser.addOnlineUser(name);
		
		resMap.put("result", "success");
		Player thisPlayer = userInfo.queryByName(name);
		resMap.put("userInfo", thisPlayer);
		return resMap;
	}
	
	public HashMap<String, Object> userRegister(String name, String password){
		resMap = new HashMap<String, Object>();
		userInfo.queryAll();
		if (userInfo.ifContainByName(name)){
			resMap.put("result", "username already exists");
			return resMap;
		}
		
		Player thisInfo = new Player(name, password);
		userInfo.addUser(thisInfo);
		
		onlineUser.addOnlineUser(name);
		// currently file IO, need to be replaced by JDBC
		
		resMap.put("result", "success");
		resMap.put("userInfo", thisInfo);
		return resMap;
	}

	@Override
	public HashMap<String, Object> userLogout(String name) throws RemoteException {
		onlineUser.deleteOnlineUser(name);
		resMap = new HashMap<String, Object>();
		resMap.put("result", "success");
		return resMap;
	}

	@Override
	public HashMap<String, Object> queryAll(String name) throws RemoteException {
		ArrayList<Player> sortedAll = userInfo.querySortedAll();
		resMap = new HashMap<String, Object>();
		resMap.put("result", "success");
		resMap.put("others", sortedAll);
		return resMap;
	}
}
	