package client;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.rmi.*;
import java.rmi.registry.*;
import java.util.ArrayList;
import java.util.*;

import javax.swing.*;

import model.Player;
import server.*;

public class GameClient implements Runnable {
	private String host;
	private GameServer gameServer;
	JFrame mainFrame;
	private Player myInfo;
	private ArrayList<Player> others;
	Vector<String> columnNames = new Vector<String>();
	Object[][] data;
	
	public GameClient(String h, Player p){
		host = h;
		myInfo = p;
		try {
	    	Registry registry = LocateRegistry.getRegistry(host);
	    	gameServer = (GameServer)registry.lookup("GameServer");
	    } catch(Exception e) {
	        System.err.println("Failed accessing RMI: "+e);
	    }
		columnNames.add("Rank");
		columnNames.add("Player");
		columnNames.add("Games won");
		columnNames.add("Games played");
		columnNames.add("Average winning time");
	}
	
	
	public void run(){
		showMain();
	}

	public void showMain(){
		mainFrame = new JFrame("JPoker 24-Game");
		mainFrame.setSize(800, 400);
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JPanel cards, ctrPanel, btnPanel;
		
		ctrPanel = new JPanel();
		ctrPanel.setLayout(new BorderLayout());
		
		CardLayout card = new CardLayout();
		cards = new JPanel(card);
		
		JPanel usrProPanel = new JPanel();
		JPanel playGamePanel = new JPanel();
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
		
		playGamePanel.add(new Label("Play Game"));
		leaderBoardPanel.setLayout(new BorderLayout());
		
		cards.add(usrProPanel, "User Profile");
		cards.add(playGamePanel, "Play Game");
		
		cards.add(leaderBoardPanel, "Leader Board");
		
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
		
		btnArr[2].addActionListener(new ActionListener(){
			@SuppressWarnings("unchecked")
			public void actionPerformed(ActionEvent e) {	
				try{
					if (gameServer != null){
						HashMap<String, Object> resMap = gameServer.queryAll(myInfo.getName());
						if(resMap.get("result").equals("success")){
							others = (ArrayList<Player>)resMap.get("others");
						}
					}
				}catch(RemoteException ex){
    				System.err.println("Failed invoking RMI: " + ex);
    			}

				Vector<Vector<Object>> columnData = new Vector<Vector<Object>>();
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
				leaderBoardPanel.removeAll();
				leaderBoardPanel.setLayout(new BorderLayout());
				leaderBoardPanel.add(table.getTableHeader(), BorderLayout.PAGE_START);
				leaderBoardPanel.add(table, BorderLayout.CENTER);
				leaderBoardPanel.repaint();
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
	
	
}
