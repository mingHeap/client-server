import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;


public class FTPServer{

	private static final int sPort = 8000;   //The server will be listening on this port number

	public static void main(String[] args) throws Exception {
		System.out.println("The server is running."); 
		ServerSocket listener = new ServerSocket(sPort);
		int clientNum = 1;
		try {
			while(true) {
				//.start() will call run()
				new Handler(listener.accept(),clientNum).start();
				System.out.println("Client "  + clientNum + " is connected!");
				clientNum++;
			}
		} finally {
			listener.close();
		} 

	}

	/**
	 * A handler thread class.  Handlers are spawned from the listening  处理程序是从监听中产生的
	 * loop and are responsible for dealing with a single client's requests. 每个线程里做什么
	 */
	private static class Handler extends Thread {
		
		private String message;    //message received from the client
		private String MESSAGE;    //Uppercase message send to the client
		private Socket connection;
		private ObjectInputStream in;	//stream read from the socket
		private ObjectOutputStream out;    //stream write to the socket
		private int no;		//The index number of the client
		private boolean is_Logged = false;
		
		
		public Handler(Socket connection, int no) {
			this.connection = connection;
			this.no = no;
		}
		
		
		public void run() {
			try{
				//initialize Input and Output streams
				iniStream();
				
				try{
					while(true)
					{
						message = (String)in.readObject();
						String[] arguments = message.split(" ");
						String command = arguments[0];
						if(is_Logged == true) {
							switch(command) {
							case "get": get(arguments[1]); continue;						
							case "upload": upload(arguments[1]); continue;
							case "dir": dir(); continue;
							default: illegalcmd();
							}
						} else if (command.contentEquals("login")){
							try {
								login(arguments[1], arguments[2]);
							}catch (Exception e) {
								sendMessage("Incorrect username or password!");
							}
						} else {
							needLogin();
						}
					}
					
				}
				catch(ClassNotFoundException classnot){
					System.err.println("Data received in unknown format");
				}
				catch(EOFException eofe) {
					eofe.printStackTrace();
					System.out.println("Disconnect with Client " + no);
				}
			}
			catch(IOException ioException){
				ioException.printStackTrace();
				System.out.println("Disconnect with Client " + no);
			}
			finally{
				//Close connections
				try{
					in.close();
					out.close();
					connection.close();
				}
				catch(IOException ioException){
					System.out.println("Disconnect with Client " + no);
				}
			}
		}
		
	
		private void iniStream() throws IOException {
			out = new ObjectOutputStream(connection.getOutputStream());
			out.flush();
			in = new ObjectInputStream(connection.getInputStream());
		}
		
		
		// client login
		void login(String username, String password) {
			String user1 = "user1";
			String pw1 = "pw1";
			String user2 = "user2";
			String pw2 = "pw2";
			if((username.contentEquals(user1) && password.contentEquals(pw1))||(username.contentEquals(user2) && password.contentEquals(pw2)))
			{
				is_Logged = true;
				sendMessage("Login Sucessfully!");
			}
			else
				sendMessage("Incorrect username or password!");

		}

		
		//invalid command
		void illegalcmd() {
			sendMessage("Your command is invalid!");
		}    

		
		// retrieve the list of file names available at the server
		void dir(){
			//specify the directory of files in the server 
			File file = new File("./");
			//get the files in this directory
			String[] f_arr = file.list();	
			String f_list = "";
			for (String i : f_arr) {
				f_list += i; 
				f_list += '\n';
			}
			sendMessage(f_list);	
		}

		
		//send file to client
		void get(String fileName) throws IOException {
			FileInputStream fileInput = null;
			DataOutputStream dataOutput = null;
			try {		
				File file = new File(fileName);
				if(!file.exists()) {
					sendMessage("File not exist");
					
				}else {
					sendMessage("File exist");
					fileInput = new FileInputStream(file);
					dataOutput = new DataOutputStream(connection.getOutputStream()); 

					//send (filename & file length)
					dataOutput.writeUTF(file.getName());
					dataOutput.flush();
					dataOutput.writeLong(file.length());
					dataOutput.flush();

					//sending file
					System.out.println("File transfering...");
					byte[] bytes = new byte[1000];
					int length = 0;
					while((length = fileInput.read(bytes)) != -1) {
						out.write(bytes,0,length);
						out.flush();				
					}
					System.out.println("File is sent sucessfully");				
				}			
			}catch(Exception e){
				e.printStackTrace();
			}

		}

	
		//receive file from client
		void upload(String fileName) {
			DataInputStream dataInput = null;
			FileOutputStream fileOutput = null;
			try {
				dataInput = new DataInputStream(connection.getInputStream());
				
				//receive filename & length
				fileName = dataInput.readUTF();
				System.out.println("Name: " + fileName);
				long fileLength = dataInput.readLong();
				System.out.println("Length: " + fileLength);
				
				File directory = new File(".");
				if(!directory.exists()) {
					directory.mkdir();				
				}
				File file = new File(directory.getAbsolutePath() + File.separatorChar + fileName);
				fileOutput = new FileOutputStream(file);
				
				//receiving file
				System.out.println("File transferring...");
				byte[] bytes = new byte[1024];
				int length = 0;
				long progress = 0;
				while((length = dataInput.read(bytes,0,bytes.length)) != -1) {
					fileOutput.write(bytes,0,length);
					fileOutput.flush();
					progress += length;
					System.out.println("progress： " + progress + "; fileLength: " + fileLength);
					if(progress >= fileLength) {
						System.out.println("File is sent!");
						break;
					}
				}				
				System.out.println("File Uploaded sucessfully");										
			}catch(Exception e){
				e.printStackTrace();
			}

		}	


		//send a message to the output stream
		public void sendMessage(String msg)
		{
			try{
				out.writeObject(msg);
				out.flush();
				System.out.println("Send message: " + msg + " to Client " + no);
			}
			catch(IOException ioException){
				ioException.printStackTrace();
			}
		}

		
		private void needLogin() {
			sendMessage("Please log in first!");
		}
	}

}
