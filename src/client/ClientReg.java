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

public class ClientReg implements Runnable{
	GameServer gameServer;
	JFrame registry;
	String username, password, comPassword;
	private Player myInfo;
	String host;
	
	public ClientReg(String h){
		try {
			host = h;
	    	Registry registry = LocateRegistry.getRegistry(h);
	    	gameServer = (GameServer)registry.lookup("GameServer");
	    } catch(Exception e) {
	        System.err.println("Failed accessing RMI: "+e);
	    }
	}
	
	public void run(){
		showRegistry();
	}
	
	/**
	 * function to show the swing of the registry window
	 */
	public void showRegistry(){
		registry = new JFrame("Login");
		JPanel panel = new JPanel();
		JPanel namePanel = new JPanel();
		JPanel pwPanel = new JPanel();
		JPanel btnPanel = new JPanel();
		JPanel comPanel = new JPanel();
		registry.setSize(400, 400);
		registry.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		panel.setBorder(BorderFactory.createTitledBorder(
				new LineBorder(Color.GRAY, 1, false),
				"register", TitledBorder.LEFT, TitledBorder.TOP));
		
		panel.setLayout(new GridLayout(4, 1));
		
		JLabel nameLabel = new JLabel("Login Name:");
		namePanel.add(nameLabel);
        
		JTextField userText = new JTextField(20);
		namePanel.add(userText);
		
        JLabel passwordLabel = new JLabel("Password:");
        pwPanel.add(passwordLabel);
        
		JPasswordField passwordText = new JPasswordField(20);
		pwPanel.add(passwordText);        
		
        JLabel comPassLabel = new JLabel("Comfirm Password:");
        comPanel.add(comPassLabel);
        
        JPasswordField comPassText = new JPasswordField(20);
        comPanel.add(comPassText); 
        
        JButton regButton = new JButton("register");
        regButton.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e) {
    			// TODO Auto-generated method stub
        		username = userText.getText();
        		password = new String(passwordText.getPassword());
        		comPassword = new String(comPassText.getPassword());
        		try {
					registerService();
				} catch (NamingException | JMSException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
    		}
        });
        btnPanel.add(regButton);
        
        JButton cancelButton = new JButton("cancel");
        cancelButton.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e) {
    			// TODO Auto-generated method stub
        		SwingUtilities.invokeLater(new ClientLogin(host));
        		registry.setVisible(false);
    		}
        });
        btnPanel.add(cancelButton);
        
        panel.add(namePanel);
        panel.add(pwPanel);
        panel.add(comPanel);
        panel.add(btnPanel);
        
        registry.add(panel);
        registry.setVisible(true);
	}

	
	/**
	 * request for registry to the server
	 * @throws NamingException
	 * @throws JMSException
	 */
	public void registerService() throws NamingException, JMSException{
		// only when username is not empty and password == compassword will the request be sent
		if (username.equals("")){
			errorHandler("Login name should not be empty!", registry);
			return;
		}else if (password.length() == 0){
			errorHandler("Password should not be empty!", registry);
			return;
		}else if (comPassword.length() == 0){
			errorHandler("Please comfirm your password!", registry);
			return;
		}
		if (!password.equals(comPassword)){
			errorHandler("The comfirm password is not the same!", registry);
			return;
		}
		if (gameServer != null){
			try{
				HashMap<String, Object>regRes = gameServer.userRegister(username, password);
				String result = (String)regRes.get("result");
				System.out.println("[Login Result: " + result + "]");
				// response will be a hashmap
				if (result.equals("success")){
					// register succeed, will directly login
					myInfo = (Player)regRes.get("userInfo");
					registry.setVisible(false);
					new GameClient(host, myInfo, gameServer).run();
					return;
				}else{
					// failed for some reason
					errorHandler(result, registry);
				}
			} catch (RemoteException ex) {
	            System.err.println("Failed invoking RMI: ");
	        }
    	}
		
	}
	
	/**
	 * directly pop out the error message
	 * @param error
	 * @param frame
	 */
	public void errorHandler(String error, JFrame frame){
		JOptionPane.showMessageDialog(frame, error, "Error", 
				JOptionPane.ERROR_MESSAGE);
	}


	
}
