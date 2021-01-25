import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

public class FTPClient {
	
	Socket requestSocket;           //socket connect to the server
	ObjectOutputStream out;         //stream write to the socket
	ObjectInputStream in;          //stream read from the socket
	String message;                //message send to the server
	String MESSAGE;                //capitalized message read from the server
	String address;
	boolean is_Connected = false;
	boolean is_Logged = false;


	public void ftpClient() {}


	public static void main(String args[]) throws Exception
	{
		FTPClient client = new FTPClient();
		client.run();
	}

	
	private void run() throws Exception
	{
		try{		
			while(is_Connected == false) {
				connectServer();
			}	
				
			iniStream();
			
			while(is_Logged == false) {
				login();
			}
			
			inputCommand();		
		}
		catch (ConnectException e) {
			System.err.println("Connection refused. You need to initiate a server first.");
		} 
		catch ( ClassNotFoundException e ) {
			System.err.println("Class not found");
		} 
		catch(UnknownHostException unknownHost){
			System.err.println("You are trying to connect to an unknown host!");
		}
		catch(IOException ioException){
			ioException.printStackTrace();
		}
		finally{
			//Close connections
			try{
				in.close();
				out.close();
				requestSocket.close();
			}
			catch(IOException ioException){
				ioException.printStackTrace();
			}
		}
	}

	
	private void connectServer(){
		while(true) 
		{
			System.out.println("Please connet to the server");

			// initialize bufferReader to get input from standard input		
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
			//用户输入连接指令 比如： connect localhost 8000
			try {
				address = bufferedReader.readLine();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			String[] arguments = address.split(" "); //split指令，根据指令各个参数 连接服务器

			//determine whether command is ftpclient or not
			if(!arguments[0].contentEquals("ftpclient")) {
				illCommand();
			} else  {

				try	{
					//create a socket to connect to the server
					int port = Integer.parseInt(arguments[2]);
					requestSocket = new Socket(arguments[1], port); 
					is_Connected = true;
					System.out.println("Connected to " + arguments[1]+ " in port " + port);					
					break;
				} catch (Exception e) {
					System.out.println("Error! Icorrect IP address or port.");
				}
			}
		}
		
	}
	
	//initialize inputStream and outputStream
	private void iniStream() throws IOException{
		out = new ObjectOutputStream(requestSocket.getOutputStream());
		out.flush();
		in = new ObjectInputStream(requestSocket.getInputStream());	
	}
	
	
	private void login() throws IOException{

		while(true) {
			System.out.println("Please login");
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
			message = bufferedReader.readLine();						
			String[] arguments = message.split(" ");				
			String command = arguments[0];	

			if (command.contentEquals("login")){					
				try {
					sendMessage(message);
					MESSAGE = (String)in.readObject();
					System.out.println("Receive message: " + MESSAGE);
					if(MESSAGE.equals("Login Sucessfully!") ) {
						is_Logged = true;
						System.out.println("You have logged in!");
						break;
					} else {
						System.out.println("invalid username or password");
					}
				}catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				needLogin();
			}
					
		}

		
	}
	
	
	private void inputCommand() throws IOException, ClassNotFoundException {
		
		while(true) {
			
			System.out.print("Please input ftp command: ");	
			
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
			
			message = bufferedReader.readLine();		
			
			String[] arguments = message.split(" ");
			
			String command = arguments[0];
			
			switch(command) {
			
				case "dir": 
					dir(); 
					continue;
					
				case "get": 
					get(arguments[1]);
					continue;
					
				case "upload": 
					upload(arguments[1]);	
					continue;
					
				default: illCommand();
			}
		}
		
	}
	
	
	private void needLogin() {
		System.out.println("Error! Please log in first!");
	}

	
	private void illCommand() {
		System.out.println("Error! Please input correct command!");
	}  
	
	
	//send a message to the output stream
	private void sendMessage(String msg)
	{
		try{
			//stream write the message
			out.writeObject(msg);
			out.flush();
		}
		catch(IOException ioException){
			ioException.printStackTrace();
		}
	}


	private void dir() throws IOException, ClassNotFoundException{
		sendMessage(message);
		//Receive the upperCase sentence from the server
		MESSAGE = (String)in.readObject();
		//show the message to the user
		System.out.println("list of the files: " + MESSAGE);		
	}
		
	
	private void upload(String fileName) {
		FileInputStream fileInput = null;
		
		DataOutputStream dataOutput = null;
		
		try {		
			
			File file = new File(fileName);
			
			if(!file.exists()) {
				
				System.out.println("File not exist");
				
			} else {
				
				fileInput = new FileInputStream(file);
				
				dataOutput = new DataOutputStream(requestSocket.getOutputStream()); 
				
				sendMessage("upload "+ fileName);
				
				//send (filename & file length)
				dataOutput.writeUTF(file.getName());
				
				dataOutput.flush();
				
				dataOutput.writeLong(file.length());
				
				dataOutput.flush();

				//sending file
				System.out.println("Uploading file to the server");
				
				byte[] bytes = new byte[1024];
				
				int length = 0;
				
				while((length = fileInput.read(bytes,0,bytes.length)) != -1) {
					
					out.write(bytes,0,length);
					
					out.flush();				
				}
				
				System.out.println("File is uploaded to the server");				
			}		
			
		}catch(Exception e){			
			
			e.printStackTrace();			
		}

	}

	
	private void get(String fileName) throws IOException {
		
		DataInputStream dataInput = null;
		
		FileOutputStream fileOutput = null;
		
		try {		

			dataInput = new DataInputStream(requestSocket.getInputStream());
			
			sendMessage(message);
			
			MESSAGE = (String) in.readObject();
			
			System.out.println("Receive message: " + MESSAGE);
			
			if(MESSAGE.equals("File not exist")){
				
				System.out.println("Please input valid filename");				
				
			}else {			
				
			//receive filename & length
				
			fileName = dataInput.readUTF();
			
			long fileLength = dataInput.readLong();
			
			File directory = new File(".");
			
			if(!directory.exists()) {
				
				directory.mkdir();		
				
			}
			
			File file = new File(directory.getAbsolutePath() + File.separatorChar + fileName);
			
			fileOutput = new FileOutputStream(file);

			//receiving file
			System.out.println("File is transferring");
			
			byte[] bytes = new byte[1024];
			
			int length = 0;
			
			long progress = 0;
			
			while((length = dataInput.read(bytes)) != -1) {
				
				fileOutput.write(bytes,0,length);
				
				fileOutput.flush();
				
				progress += length;
				
				System.out.println("progress: " + progress + "; fileLength: "+ fileLength);
				
				if(progress >= fileLength) {
					
					break;
					
				}
			}				
			
			System.out.println("File is downloaded sucessfully");	
			
			}
			
		}catch(Exception e){
			
			e.printStackTrace();
			
		}

	}


}

