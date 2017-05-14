/**********************************************************************


												Author: Chen Jiali 
												UID: 3035085695


***********************************************************************/
package client;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;

import javax.jms.JMSException;
import javax.naming.NamingException;
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

import server.GameServer;
import server.Player;

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
	
	/**
	 * function to control the swing of the login window
	 */
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
        		username = userText.getText();
        		password = new String(passwordText.getPassword());
        		try {
					loginService();
				} catch (NamingException | JMSException e1) {
					e1.printStackTrace();
				}
    		}
        });
        btnPanel.add(loginButton);
        
        JButton regButton = new JButton("register");
        regButton.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e) {
        		Thread t = new Thread(new ClientReg(host));
				t.start();
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
	
	public void loginService() throws NamingException, JMSException{
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
				// response of the login request will come as a hashmap 
				System.out.println("[Register Result: " + result + "]");
				if (result.equals("success")){
					// if the login succeed
					myInfo = (Player)loginRes.get("userInfo");
					login.setVisible(false);
					new GameClient(host, myInfo, gameServer).run();
					return;
				}else{
					//login failed because the account has already been online
					errorHandler(result, login);
				}
			} catch (RemoteException ex) {
	            System.err.println("Failed invoking RMI: " + ex);
	        }
    	}
	}
	  
	/**
	 * directly print the error message
	 * @param error
	 * @param frame
	 */
	public void errorHandler(String error, JFrame frame){
		JOptionPane.showMessageDialog(frame, error, "Error", 
				JOptionPane.ERROR_MESSAGE);
	}
	
}
