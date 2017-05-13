package server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.rmi.*;
import java.rmi.server.*;
import java.util.*;

import javax.jms.*;
import javax.naming.NamingException;

import model.*;
import helper.*;

public class GameServerImpl extends UnicastRemoteObject 
					implements GameServer{
	private UserInfo userInfo;
	private OnlineUser onlineUser;
	private ArrayList<Player> inGameUser;
	private HashMap<String, Double> timeRec;
	private HashMap<String, Object> resMap;
<<<<<<< HEAD
	private HashMap<String, String[]> queMap;
	private String winAns;
=======
	private HashMap<String, Integer[]> queMap;
>>>>>>> origin/master
	
	private JMSHelper jmsHelper;
	private MessageConsumer queueReader;
	private MessageProducer topicSender;
	
	private long gameTimer;
	private long waitTimer;
	private long curTime;
	private double waitPeriod;
	private double gamePeriod;
	
<<<<<<< HEAD
	QuestionBase qb;
=======
>>>>>>> origin/master
	private String bestPlayer;
	
	/*
	 * state is to record the state of the server
	 * 0 for initial
	 * 1 for waiting
	 * 2 for already started
	 * 3 for conclusion
	 */
	private int state; 
	protected GameServerImpl() throws RemoteException, NamingException, JMSException {
		super();
		// Here is the construction of the map/set objects which will be used later
		queMap = new HashMap<String, String[]>();
		userInfo = new UserInfo();
		onlineUser = new OnlineUser(); 	
		inGameUser = new ArrayList<Player>();
		timeRec = new HashMap<String, Double>();
		gameTimer = 0L;
		waitTimer = 0L;
		state = 0;
		jmsHelper = new JMSHelper();
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
			app.start();
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
	
	public HashMap<String, Object> compInfix(String infix, String name) throws RemoteException {
		InfixParser inp = new InfixParser();
		double result = inp.evaluate(infix);

		// TODO finish recording and comparing
		if (validInfix(infix, name) && result == 24){
<<<<<<< HEAD
			winAns = infix;
=======
>>>>>>> origin/master
			state = 3;
			bestPlayer = name;
		}
		// return value
 		resMap = new HashMap<String, Object>();
		resMap.put("result", result);
		
		return resMap;
	}
	
	/**
	 * function to evaluate whether player got a valid infix expression
	 * @return
	 */
	protected boolean validInfix(String infix, String name){
		// TODO
		return true;
	}
	
	public void start() throws JMSException{
<<<<<<< HEAD
		qb = new QuestionBase();
=======
>>>>>>> origin/master
		queueReader = jmsHelper.createQueueReader();
		topicSender = jmsHelper.createTopicSender();
		System.out.println("[DEBUG] start function call");
		Thread t = new Thread(new MsgHandler());
		t.start();
	}
	
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
	
	public void joinHandler(String name){
		System.out.println("[DEBUG] join handler for: " + name);
		if (state == 0 || state == 3){ // initial state until the first request
			waitPeriod = 0;
			waitTimer = System.currentTimeMillis();
			inGameUser.add(userInfo.queryByName(name));
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
	
	public void inGameHandler(){
		System.out.println("[DEBUG] game start");
		HashMap <String, Object> respMap = new HashMap<String, Object>();
		respMap.put("response", "start");
		respMap.put("opponents", inGameUser);
		for(Player p : inGameUser){
			try {
<<<<<<< HEAD
				String[] question = qb.get(p.hashCode());
				queMap.put(p.getName(), question);
				respMap.put("question", question);
=======
>>>>>>> origin/master
				sendMessageTo(topicSender, respMap, p.getName());
			} catch (JMSException e) {}
		}
		gameTimer = System.currentTimeMillis();
		new Thread(new inGameWorker()).start();
	}
	
	public class joinWorker implements Runnable{
		@Override
		public void run() {
			while(state == 1){
				curTime=System.currentTimeMillis();
				waitPeriod = ((double)curTime - (double)waitTimer)/1000;
				if (waitPeriod > 10 && inGameUser.size()>1){
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
	
	public class inGameWorker implements Runnable{
		@Override
		public void run() {
			while(state == 2){
				curTime = System.currentTimeMillis();
				gamePeriod = ((double)curTime - (double)gameTimer)/1000;
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {}
			}
			HashMap<String, Object> respMap = new HashMap<String, Object>();
			respMap.put("response", "end");
			respMap.put("winner", bestPlayer);
<<<<<<< HEAD
			respMap.put("winnerRes", winAns);
=======
			respMap.put("time", gamePeriod);
>>>>>>> origin/master
			for(Player p : inGameUser){
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
	