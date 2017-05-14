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
import java.io.Serializable;
import java.rmi.*;
import java.rmi.server.*;
import java.sql.SQLException;
import java.util.*;

import javax.jms.*;
import javax.naming.NamingException;

public class GameServerImpl extends UnicastRemoteObject 
					implements GameServer{
	private UserInfo userInfo;
	private ArrayList<Player> inGameUser;
	private HashMap<String, Double> timeRec;
	private HashMap<String, Object> resMap;
	private HashMap<String, String[]> queMap;
	private String winAns;
	
	private JMSHelper jmsHelper;
	private MessageConsumer queueReader;
	private MessageProducer topicSender;
	
	private long gameTimer;
	private long waitTimer;
	private long curTime;
	private double waitPeriod;
	private double gamePeriod;
	
	QuestionBase qb;
	private String bestPlayer;
	
	/*
	 * state is to record the state of the server
	 * 0 for initial
	 * 1 for waiting
	 * 2 for already started
	 * 3 for conclusion
	 */
	private int state; 
	protected GameServerImpl() throws RemoteException, NamingException, JMSException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		super();
		// Here is the construction of the map/set objects which will be used later
		queMap = new HashMap<String, String[]>();
		userInfo = new UserInfo();
		inGameUser = new ArrayList<Player>();
		timeRec = new HashMap<String, Double>();
		gameTimer = 0L;
		waitTimer = 0L;
		state = 0;
		jmsHelper = new JMSHelper();
		// initialize the OnlineUser.txt file
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
			app.start();
		} catch(Exception e) {
			System.err.println("Exception thrown: "+e);
		}
	}

	/* (non-Javadoc)
	 * function to handle the login request from the client
	 * @see server.GameServer#userLogin(java.lang.String, java.lang.String)
	 */
	@Override
	public HashMap<String, Object> userLogin(String name, String password) {
		resMap = new HashMap<String, Object>();
		// if user already exists
		if (!userInfo.ifContainByName(name)){
			resMap.put("result", "user not found");
			return resMap;
		}
		// if the password is not correct
		if (!userInfo.passwordByName(name).equals(password)){	
			resMap.put("result", "wrong password");
			return resMap;
		}
		// if the user is already online
		if (userInfo.ifOnline(name)){
			resMap.put("result", "user already log in");
			return resMap;
		}
		userInfo.addOnlineUser(name);
		
		resMap.put("result", "success");
		Player thisPlayer = userInfo.queryByName(name);
		resMap.put("userInfo", thisPlayer);
		return resMap;
	}
	
	/* (non-Javadoc)
	 * handle registry request from the client
	 * @see server.GameServer#userRegister(java.lang.String, java.lang.String)
	 */
	public HashMap<String, Object> userRegister(String name, String password){
		resMap = new HashMap<String, Object>();
		userInfo.queryAll();
		// if the user already existed
		if (userInfo.ifContainByName(name)){
			resMap.put("result", "username already exists");
			return resMap;
		}
		// create the information of this player and insert it into the DB
		Player thisInfo = new Player(name, password);
		userInfo.addUser(thisInfo);
		
		userInfo.addOnlineUser(name);
		
		resMap.put("result", "success");
		resMap.put("userInfo", thisInfo);
		return resMap;
	}

	/* (non-Javadoc)
	 * handle logout request from the client
	 * @see server.GameServer#userLogout(java.lang.String)
	 */
	@Override
	public HashMap<String, Object> userLogout(String name) throws RemoteException {
		userInfo.deleteOnlineUser(name); // mark this user as offline
		resMap = new HashMap<String, Object>();
		resMap.put("result", "success");
		return resMap;
	}

	/* (non-Javadoc)
	 * send others' information for the client to print the leader board
	 * @see server.GameServer#queryAll(java.lang.String)
	 */
	@Override
	public HashMap<String, Object> queryAll(String name) throws RemoteException {
		ArrayList<Player> sortedAll = userInfo.querySortedAll();
		resMap = new HashMap<String, Object>();
		resMap.put("result", "success");
		resMap.put("others", sortedAll);
		return resMap;
	}
	
	/* (non-Javadoc)
	 * this is the webservice for computing the infix expression
	 * @see server.GameServer#compInfix(java.lang.String, java.lang.String)
	 */
	public HashMap<String, Object> compInfix(String infix, String name) throws RemoteException {
		InfixParser inp = new InfixParser();
		double result = inp.evaluate(infix);

		// the result is correct, which means we have got a winner, this round finishes
		if (result == 24){
			winAns = infix; // set answer of the winner
			state = 3;
			bestPlayer = name;
			userInfo.updateWonTime(name, gamePeriod); // update winning information
			userInfo.updateWonGame(name);
		}
		// return value
 		resMap = new HashMap<String, Object>();
		resMap.put("result", result);
		
		return resMap;
	}
	
	public void start() throws JMSException{
		qb = new QuestionBase();
		queueReader = jmsHelper.createQueueReader();
		topicSender = jmsHelper.createTopicSender();
		System.out.println("[DEBUG] start function call");
		Thread t = new Thread(new MsgHandler());
		t.start();
	}
	
	/**
	 * @author jchen
	 * a worker always running for the incoming messages, give the message to request handler, who works as a mapper
	 */
	private class MsgHandler implements Runnable{
		public void run() {
			System.out.println("[DEBUG] message handler start running");
			try {
				while(true){
					HashMap<String, Object> req = receiveMessage(queueReader);
					reqHandler(req);
				}
			} catch (JMSException e) {}
		}
		
	}
	
	/**
	 * read the message and get out the hash map containing the information
	 * @param queueReader
	 * @return
	 * @throws JMSException
	 */
	public HashMap<String, Object> receiveMessage(MessageConsumer queueReader) throws JMSException {
		try {
	        Message jmsMessage = queueReader.receive();
	        System.out.println("[DEBUG] message received");
	        HashMap reqMap = (HashMap) ((ObjectMessage) jmsMessage).getObject();
	        return reqMap;
	    } catch(JMSException e) {
	        System.err.println("Failed to receive message "+e);
	        throw e;
	    }
	}
	
	/**
	 * a mapper for the incoming messages, throw join message to join handler, leave message to leave handler
	 * @param reqMap
	 */
	public void reqHandler(HashMap<String, Object> reqMap){
		String reqType = (String)reqMap.get("request");
		String from = (String)reqMap.get("from");
		System.out.println("[DEBUG] request type: " + reqType);
		if (reqType.equals("join")){
			joinHandler(from);
		} else if (reqType.equals("leave")){
			leaveHandler(from);
		} else {
			
		}
	}
	
	/**
	 * handler for the join requests
	 * @param name
	 */
	public void joinHandler(String name){
		System.out.println("[DEBUG] join handler for: " + name);
		if (state == 0 || state == 3){ // initial state until the first request
			waitPeriod = 0;
			waitTimer = System.currentTimeMillis(); // start the waiting timer
			inGameUser.add(userInfo.queryByName(name)); // record the player enter the room
			state = 1;
			new Thread(new joinWorker()).start();
		} else if (state == 1){ // waiting state
			inGameUser.add(userInfo.queryByName(name));
			if (waitPeriod > 10 || inGameUser.size() == 4){ 
				// start game when wait period > 10 or have 4 players.
				state = 2;
				inGameHandler();
			}
		} else if (state == 2){
			// during the game, client cannot ask for join
			HashMap<String, Object> respMap = new HashMap<String, Object>();
			respMap.put("response", "reject");
			try {
				sendMessageTo(topicSender, respMap, name);
			} catch (JMSException e) {}
			
		} else {
			// in the conclusion period
		}
	}
	
	public void leaveHandler(String name){
		System.out.println("[DEBUG] leave handler for: " + name);
	}
	
	/**
	 * a handler used at the point of the game starts,
	 * responsible for distributed questions and notice the client their opponents 
	 */
	public void inGameHandler(){
		System.out.println("[DEBUG] game start");
		HashMap <String, Object> respMap = new HashMap<String, Object>();
		respMap.put("response", "start");
		respMap.put("opponents", inGameUser);
		for(Player p : inGameUser){
			try {
				String[] question = qb.get(p.hashCode()); // randomly select a question from the question base, use hash code of the player for randomness
				queMap.put(p.getName(), question);
				respMap.put("question", question);
				sendMessageTo(topicSender, respMap, p.getName()); // send a private message to every player
			} catch (JMSException e) {}
		}
		gameTimer = System.currentTimeMillis(); // game timing starts
		new Thread(new inGameWorker()).start();
	}
	
	/**
	 * @author jchen
	 * a helper worker for deciding when to start the game, 
	 * responsible for timing and the start premise: wait > 10s, 2 or 3 players 
	 */
	public class joinWorker implements Runnable{
		@Override
		public void run() {
			while(state == 1){
				curTime=System.currentTimeMillis(); // continue timing during waiting
				waitPeriod = ((double)curTime - (double)waitTimer)/1000;
				if (waitPeriod > 10 && inGameUser.size()>1){ // having 2 or 3 player and have timing > 10s
					state = 2;
					inGameHandler();
					break;
				}
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {}
			}
		}
	}
	
	/**
	 * @author jchen
	 * a helper worker for instant issues during playing
	 * responsible for the timing and when got a winner, 
	 * finishing the game and sending winner information to the players
	 */
	public class inGameWorker implements Runnable{
		@Override
		public void run() {
			while(state == 2){ // keep timing when the game is playing
				curTime = System.currentTimeMillis();
				gamePeriod = ((double)curTime - (double)gameTimer)/1000;
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {}
			}
			// send winner's information to all the in-room players
			HashMap<String, Object> respMap = new HashMap<String, Object>();
			respMap.put("response", "end");
			respMap.put("winner", bestPlayer);
			respMap.put("winnerRes", winAns);
			respMap.put("winTime", gamePeriod);
			for(Player p : inGameUser){
				// every in-game player gets one more played game count
				userInfo.updatePlayedGame(p.getName());
				try {
					sendMessageTo(topicSender, respMap, p.getName());
				} catch (JMSException e) {}
			}
			inGameUser.removeAll(inGameUser);
		}
		
	}
	
	public void broadcastMessage(MessageProducer topicSender, Message jmsMessage) throws JMSException {
		try {
	        topicSender.send(jmsMessage);
	    } catch(JMSException e) {
	        System.err.println("Failed to boardcast message "+e);
	        throw e;
	    }
	}
	
	public void sendMessageTo(MessageProducer topicSender, Serializable obj, String name) throws JMSException{
		Message message = null;
		try {
			message = jmsHelper.createMessage(obj);
			message.setStringProperty("privateMessageTo", name);
			broadcastMessage(topicSender, message);
		} catch (JMSException e) {}
	}
}
	