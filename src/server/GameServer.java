package server;
import model.Player;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;

public interface GameServer extends Remote{
	public HashMap<String, Object> userLogin(String name, String password) throws RemoteException;
	public HashMap<String, Object> userRegister(String name, String password) throws RemoteException;
	public HashMap<String, Object> userLogout(String name) throws RemoteException;
	public HashMap<String, Object> queryAll(String name) throws RemoteException;
	public HashMap<String, Object> compInfix(String infix, String name) throws RemoteException;
}