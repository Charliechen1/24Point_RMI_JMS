package client;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import model.Player;
import server.GameServer;

public class ClientLogin implements Runnable{
	GameServer gameServer;
	JFrame login;
	String username, password;
	private Player myInfo;
	String host;
	
	public ClientLogin(String h){
		try {
			host = h;
	    	Registry registry = LocateRegistry.getRegistry(host);
	    	gameServer = (GameServer)registry.lookup("GameServer");
	    } catch(Exception e) {
	        System.err.println("Failed accessing RMI: "+e);
	    }
	}
	
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new ClientLogin(args[0]));
	}
	
	public void run(){
		showLogin();
	}
	
	public void showLogin(){
		login = new JFrame("Login");
		JPanel panel = new JPanel();
		JPanel namePanel = new JPanel();
		JPanel pwPanel = new JPanel();
		JPanel btnPanel = new JPanel();
		login.setSize(400, 300);
		login.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		panel.setBorder(BorderFactory.createTitledBorder(
				new LineBorder(Color.GRAY, 1, false),
				"login", TitledBorder.LEFT, TitledBorder.TOP));
		
		panel.setLayout(new GridLayout(3, 1));
		
		JLabel nameLabel = new JLabel("Login Name:");
        namePanel.add(nameLabel);
        
		JTextField userText = new JTextField(20);
		namePanel.add(userText);
		
        JLabel passwordLabel = new JLabel("Password:");
        pwPanel.add(passwordLabel);
        
		JPasswordField passwordText = new JPasswordField(20);
		pwPanel.add(passwordText);        
        
        JButton loginButton = new JButton("login");
        loginButton.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e) {
    			// TODO Auto-generated method stub
        		username = userText.getText();
        		password = new String(passwordText.getPassword());
        		loginService();
    		}
        });
        btnPanel.add(loginButton);
        
        JButton regButton = new JButton("register");
        regButton.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e) {
        		new ClientReg(host).run();
        		login.setVisible(false);
    		}
        });
        btnPanel.add(regButton);
       
        panel.add(namePanel);
		panel.add(pwPanel);
        panel.add(btnPanel);
        
        login.add(panel);
		login.setVisible(true);
	}
	
	public void loginService(){
		if (username.equals("")){
			errorHandler("Login name should not be empty!", login);
			return;
		}else if (password.length() == 0){
			errorHandler("Password should not be empty!", login);
			return;
		}
		
		if (gameServer != null){
			try{
				HashMap<String, Object> loginRes = gameServer.userLogin(username, password);
				String result = (String)loginRes.get("result");
				System.out.println("[Register Result: " + result + "]");
				if (result.equals("success")){
					myInfo = (Player)loginRes.get("userInfo");
					login.setVisible(false);
					new GameClient(host, myInfo).showMain();
					return;
				}else{
					errorHandler(result, login);
				}
			} catch (RemoteException ex) {
	            System.err.println("Failed invoking RMI: " + ex);
	        }
    	}
	}
	
	public void errorHandler(String error, JFrame frame){
		JOptionPane.showMessageDialog(frame, error, "Error", 
				JOptionPane.ERROR_MESSAGE);
	}
	
}
