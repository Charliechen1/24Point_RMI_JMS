package client;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.rmi.*;
import java.rmi.registry.*;
import java.util.*;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.naming.NamingException;
import javax.swing.*;

import model.Player;
import server.GameServer;
import helper.*;

public class GameClient implements Runnable, MessageListener {
	private String host;
	private String winner = "no one";
	private String winnerQue = "nothing";
	private String currRes = "Not defined";

	private JTextField msgBox;
	private GameServer gameServer;
	JFrame mainFrame;
	
	private Player myInfo;
	private ArrayList<Player> others;
	private ArrayList<Player> oppos;
	
	private JMSHelper jmsHelper;
	private MessageProducer queueSender;
	private MessageConsumer topicReceiver;
	
	PlayGamePanel playGamePanel;
	
	private char[][] cards;
	Vector<String> columnNames = new Vector<String>();
	int coun = 0;

	/* this is the state of the client, values are 0,1,2,3
	  	0: initial state, 
	 	1: game joining state,
	 	2: game playing state,
	 	3: game over state
	*/
	private int state;
	
	public GameClient(String h, Player p, GameServer g) throws NamingException, JMSException{
		jmsHelper = new JMSHelper();
		host = h;
		myInfo = p;
		gameServer = g;
		state = 0;
		if (g == null){
			try {
		    	Registry registry = LocateRegistry.getRegistry(host);
		    	gameServer = (GameServer)registry.lookup("GameServer");
		    } catch(Exception e) {
		        System.err.println("Failed accessing RMI: "+e);
		    }
		}
		columnNames.add("Rank");
		columnNames.add("Player");
		columnNames.add("Games won");
		columnNames.add("Games played");
		columnNames.add("Average winning time");

		init();
	}
	
	/**
	 * initial some useful global variables
	 * @throws JMSException
	 */
	public void init() throws JMSException{
		oppos = new ArrayList<Player>();
		cards = new char[4][2];
		queueSender = jmsHelper.createQueueSender();
		topicReceiver = jmsHelper.createTopicReader(myInfo.getName());
		topicReceiver.setMessageListener(this);
	}
	
	public void run(){
		showMain();
	}

	/**
	 * function to deal with GUI in the main frame
	 */
	public void showMain(){
		mainFrame = new JFrame("JPoker 24-Game");
		mainFrame.setSize(800, 400);
		mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		mainFrame.addWindowListener(new WindowAdapter() {
	        @Override
	        public void windowClosing(WindowEvent event) {
	            try {
					exitProcedure(myInfo.getName(), mainFrame);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
	        }
	    });
		JPanel cards, ctrPanel, btnPanel;
		
		ctrPanel = new JPanel();
		ctrPanel.setLayout(new BorderLayout());
		
		CardLayout card = new CardLayout();
		cards = new JPanel(card);
		
		JPanel usrProPanel = new JPanel();
		playGamePanel = new PlayGamePanel(gameServer);
		JPanel leaderBoardPanel = new JPanel();

		usrProPanel.setLayout(new GridLayout(6, 1));
		usrProPanel.add(new Label("User Profile"));
		
		if (myInfo != null) {
			usrProPanel.add(new JLabel(myInfo.getName()));
			usrProPanel.add(new JLabel("Number of wins: " + myInfo.getWonGames()));
			usrProPanel.add(new JLabel("Number of games: " + myInfo.getPlayedGames()));
			usrProPanel.add(new JLabel("Average time to win: " + myInfo.getAvgWinTime() + "s"));
			usrProPanel.add(new JLabel("Rank: #" + myInfo.getRank()));
		}
		
		playGamePanel.setLayout(new BorderLayout());
		playGamePanel.refresh();
		
		cards.add(usrProPanel, "User Profile");
		cards.add(playGamePanel, "Play Game");

		leaderBoardPanel.setLayout(new BorderLayout());
		cards.add(leaderBoardPanel, "Leader Board");
		refreshLeaderBoard(leaderBoardPanel);
		
		btnPanel = new JPanel();
		btnPanel.setLayout(new GridLayout(1, 4));
		
		String[] taskArr = {"User Profile", "Play Game", "Leader Board", "Logout"};
		JButton[] btnArr = new JButton[4];
		
		for (int i=0; i<4; i++){
			String task = taskArr[i];
			btnArr[i] = new JButton(task);
			btnArr[i].addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e) {	
	    			card.show(cards, task);
	    		}
			});
		}
		
		btnArr[1].addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {	
				playGamePanel.refresh();
				new Thread(new panelPrintWorker(playGamePanel)).start();
			}
		});
		
		btnArr[2].addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {	
				refreshLeaderBoard(leaderBoardPanel);
				new Thread(new panelPrintWorker(leaderBoardPanel)).start();
			}
		});
		
		btnArr[3].addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {	
    			try{
    				if (gameServer != null){
    					HashMap<String, Object> res = gameServer.userLogout(myInfo.getName());
    					if (res.get("result").equals("success")){
    						mainFrame.setVisible(false);
    						new ClientLogin(host).run();
    					}
        			}
    			}catch(RemoteException ex){
    				System.err.println("Failed invoking RMI: " + ex);
    			}
    		}
		});
		
		for (JButton jbtn : btnArr){
			btnPanel.add(jbtn);
		}
		
		ctrPanel.add(btnPanel, BorderLayout.NORTH);
		ctrPanel.add(cards, BorderLayout.CENTER);
		
		mainFrame.add(ctrPanel);
		mainFrame.setVisible(true);
	}
	
	/**
	 * Used in leader board, get all players
	 */
	protected void getAllPlayers(){
		//System.out.println("[DEBUG] Getting others.");
		HashMap<String, Object> resMap;
		try {
			resMap = gameServer.queryAll(myInfo.getName());
			if(resMap.get("result").equals("success")){
				others = (ArrayList<Player>)resMap.get("others");
			}

			//System.out.println("[DEBUG] Other size: "+others.size());
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return;
	}
	
	/**
	 * function to refresh the leader board, get all players and re-construct the table
	 * @param p
	 */
	protected void refreshLeaderBoard(JPanel p){
		Vector<Vector<Object>> columnData = new Vector<Vector<Object>>();
		getAllPlayers();
		for (Player o : others){
			Vector<Object> row = new Vector<Object>();
			row.add(o.getRank());
			row.add(o.getName());
			row.add(o.getWonGames());
			row.add(o.getPlayedGames());
			row.add(o.getAvgWinTime());
			columnData.add(row);
		}
		JTable table = new JTable(columnData, columnNames);
		p.removeAll();
		p.add(table.getTableHeader(), BorderLayout.PAGE_START);
		p.add(table, BorderLayout.CENTER);
		System.out.println("[DEBUG] leader board refreshed.");
	}
	
//	private class LeaderBoardPanel extends JPanel implements MouseListener {
//		private GameServer gameServer;
//		public LeaderBoardPanel(GameServer gs){
//			//addMouseListener(this);
//			gameServer = gs;
//			refresh(this);
//		}
//		
//		public void mouseClicked(MouseEvent e) {
//			new Thread(new panelPrintWorker(this)).start();
//		}
//		public void mouseEntered(MouseEvent e) {}
//		public void mouseExited(MouseEvent e) {}
//		public void mousePressed(MouseEvent e) {}
//		public void mouseReleased(MouseEvent e) {}	
//	}
	
	/**
	 * @author Charliechen
	 * a worker for painting thread
	 */
	private class panelPrintWorker implements Runnable{
		private JPanel thisPanel;
		
		panelPrintWorker(JPanel p){
			thisPanel = p;
		}
		
		public void run() {
			thisPanel.revalidate();
			thisPanel.repaint();
		}
	}	
	
	private class PlayGamePanel extends JPanel implements MouseListener{
		GameServer gs;
		private PlayGamePanel(GameServer g){
			this.gs = g;
			state = 0;
			this.addMouseListener(this);
		}
		
		@Override
		public void mouseClicked(MouseEvent e) {
			System.out.println("[DEBUG] play game panel clicked");
			this.refresh();
		}

		@Override
		public void mousePressed(MouseEvent e) {}

		@Override
		public void mouseReleased(MouseEvent e) {}

		@Override
		public void mouseEntered(MouseEvent e) {}

		@Override
		public void mouseExited(MouseEvent e) {}	
		
		/**
		 * function to control the elements of the PlayGamePanel according to its state
		 * @param playGamePanel
		 */
		protected void refresh(){
			JLabel label;
			switch(state){
			case 0: // initial state
				System.out.println("[DEBUG] Now initial state.");
				this.removeAll();
				JButton newGameBtn = new JButton("New Game"); // button for next game
				newGameBtn.addActionListener(new ActionListener(){
					public void actionPerformed(ActionEvent e) {	
						// send join request to the server
						System.out.println("[DEBUG] new game button clicked");
						state = 1; // change state to wait response from the server
						sendJoinReq();
						refresh();
					}
				});
				this.add(newGameBtn, BorderLayout.CENTER);
				break;
			case 1: // game joining state
				System.out.println("[DEBUG] Now game joining state.");
				this.removeAll();
				label = new JLabel("Waiting for players...");
				label.setHorizontalAlignment(JLabel.CENTER);
				this.add(label, BorderLayout.CENTER);
				break;
			case 2: // game playing state
				System.out.println("[DEBUG] Now game playing state.");
				this.removeAll();
				
				JPanel oppo = new JPanel(); // panel to put name and record for all the player in room
				oppo.setLayout(new GridLayout(4, 1));
				// need further work, place to put information of the players in room
				oppo.add(new JLabel("<html>"+myInfo.getName()+
						"<br>Win: "+myInfo.getWonGames()+"/"+myInfo.getPlayedGames()+
						" Avg: "+myInfo.getAvgWinTime()+"s</html>"));
				
				for (int i=0; i<oppos.size(); i++){
					if (!oppos.get(i).getName().equals(myInfo.getName()))
						oppo.add(new JLabel("<html>"+oppos.get(i).getName()+
							"<br>Win: "+oppos.get(i).getWonGames()+"/"+oppos.get(i).getPlayedGames()+
							" Avg: "+oppos.get(i).getAvgWinTime()+"s</html>"));
				}
				this.add(oppo, BorderLayout.EAST);
				
				JPanel mainBoard = new JPanel(); // place for cards and the computation text 
				mainBoard.setLayout(new BorderLayout());
				
				JPanel cardPane = new JPanel();
				cardPane.setLayout(new GridLayout(1, 4));
				
				// wait for further modification
				cardPane.add(new Label("Card1"));
				cardPane.add(new Label("Card2"));
				cardPane.add(new Label("Card3"));
				cardPane.add(new Label("Card4"));
				mainBoard.add(cardPane, BorderLayout.CENTER);
				
				JPanel comPane = new JPanel();
				msgBox = new JTextField();
				msgBox.setPreferredSize(new Dimension(400, 30));
				msgBox.addKeyListener(new KeyAdapter() {
					public void keyPressed(KeyEvent e) {
						if(e.getKeyCode() == KeyEvent.VK_ENTER) {
							try {
								sendComp();
							} catch (RemoteException e1) {
								e1.printStackTrace();
							}
						}
					}
				});
				
				comPane.add(msgBox);
				comPane.add(new Label(" = " + currRes));
				mainBoard.add(comPane, BorderLayout.PAGE_END);
				
				this.add(mainBoard, BorderLayout.CENTER);
				break;
			case 3: // game over state
				System.out.println("[DEBUG] Now game over state.");
				this.removeAll();
				JButton nextGameBtn = new JButton("Next Game"); // button for next game
				nextGameBtn.addActionListener(new ActionListener(){
					public void actionPerformed(ActionEvent e) {	
						// send join request to the server
						System.out.println("[DEBUG] next game button clicked");
						// reinitialize the global variables
						winner = "no one";
						winnerQue = "nothing";
						currRes = "Not defined";
						state = 1; // change state to wait response from the server
						sendJoinReq();
						refresh();
					}
				});
				String labelInfo = "<html>Winner: "+winner + "<br>" + winnerQue + "</html>";
				label = new JLabel(labelInfo);
				label.setHorizontalAlignment(JLabel.CENTER);
				this.add(nextGameBtn, BorderLayout.SOUTH);
				this.add(label, BorderLayout.CENTER);
				break;
			}
			new Thread(new panelPrintWorker(this)).start();
		}
		
		protected void sendComp() throws RemoteException{
			String infix = msgBox.getText().trim();
			System.out.println("[DEBUG] infix: " + infix);
			HashMap<String, Object> resMap = this.gs.compInfix(infix, myInfo.getName());
			double result = (double)resMap.get("result");
			System.out.println("[DEBUG] result: " + result);
			if (result == -99999){
				currRes = "Error";
			} else {
				currRes = (int)result + "";
			}
			this.refresh();
			msgBox.setText("");
		}
	}
	
	/**
	 * function to send message for join a game
	 */
	protected void sendJoinReq(){
		System.out.println("[DEBUG] request for join a game");
		HashMap reqMap = new HashMap<String, Object>();
		reqMap.put("request", "join");
		reqMap.put("from", myInfo.getName());
		Message message = null;
		try {
			message = jmsHelper.createMessage(reqMap);
		} catch (JMSException e) {}
		if(message != null) {
			try {
				queueSender.send(message);
			} catch (JMSException e) {
				System.err.println("Failed to send message");
			}
		}
	}
	
	protected void rejectHandler(HashMap<String, Object> respMap){
		System.out.println("[DEBUG] rejected");
		state = 0;
		playGamePanel.refresh();
	}
	
	protected void startHandler(HashMap<String, Object> respMap){
		if (respMap != null){
			oppos = (ArrayList<Player>) respMap.get("opponents");
			state = 2;
			playGamePanel.refresh();
		}
	}
	
	protected void endHandler(HashMap<String, Object> respMap){
		if (respMap != null){
			state = 3;
			winner = (String)respMap.get("winner");
			winnerQue = (double)respMap.get("time")+"s";
			playGamePanel.refresh();
		}
	}
	
	protected void exitProcedure(String name, JFrame frame) throws RemoteException{
		gameServer.userLogout(name);
		frame.dispose();
	    System.exit(0);
	}
	
	/* (non-Javadoc)
	 * @see javax.jms.MessageListener#onMessage(javax.jms.Message)
	 */
	public void onMessage(Message jmsMessage) {
		try {
	        System.out.println("[DEBUG] message received");
	        HashMap respMap = (HashMap) ((ObjectMessage) jmsMessage).getObject();
	        String respType = (String)respMap.get("response");
	        if (respType.equals("reject")){
	        	rejectHandler(respMap);
	        } else if (respType.equals("start")){
	        	startHandler(respMap);
	        } else if (respType.equals("end")){
	        	endHandler(respMap);
	        }
	    } catch (JMSException e) {
	        System.err.println("Failed to receive message");
	    }
	}
}

