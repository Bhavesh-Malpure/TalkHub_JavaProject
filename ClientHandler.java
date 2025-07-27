import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ClientHandler implements Runnable {

    public static ArrayList<ClientHandler> clientHandlers = new ArrayList<>();
    public static Map<String, ClientHandler> userMap = new HashMap<>();

    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    String clientUsername;

    public ClientHandler(Socket socket){
        try{
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.clientUsername = bufferedReader.readLine();

            clientHandlers.add(this);
            userMap.put(clientUsername, this);

            broadcastMessage("Server : " + clientUsername + " has entered the chat !");
            broadcastUserList();
        }catch (IOException e){
            closeEverything(socket , bufferedReader ,bufferedWriter);
        }
    }

    @Override
    public void run() {
        String messageFromClient;
        while (socket.isConnected()){
            try{
                messageFromClient = bufferedReader.readLine();
                if (messageFromClient == null) {
                    closeEverything(socket, bufferedReader, bufferedWriter);
                    break;
                }

                if (messageFromClient.startsWith("__DM__:")) {
                    handlePrivateMessage(messageFromClient);
                } else {
                    broadcastMessage(messageFromClient);
                }
            } catch (IOException e) {
                closeEverything(socket , bufferedReader ,bufferedWriter);
                break;
            }
        }
    }

    private void handlePrivateMessage(String raw) {
        // format: __DM__:<toUsername>:<message>
        try {
            String withoutPrefix = raw.substring("__DM__:".length());
            int firstColon = withoutPrefix.indexOf(':');
            if (firstColon == -1) return;

            String toUser = withoutPrefix.substring(0, firstColon).trim();
            String message = withoutPrefix.substring(firstColon + 1).trim();

            ClientHandler toHandler = userMap.get(toUser);
            if (toHandler != null) {
                // send to target
                String payloadToTarget = "__DM__:" + clientUsername + ":" + message;
                toHandler.bufferedWriter.write(payloadToTarget);
                toHandler.bufferedWriter.newLine();
                toHandler.bufferedWriter.flush();
                // echo back to sender
                String payloadToSelf = "__DM__:" + toUser + ":" + message;
                this.bufferedWriter.write(payloadToSelf);
                this.bufferedWriter.newLine();
                this.bufferedWriter.flush();
            } else {
                this.bufferedWriter.write("Server : User " + toUser + " not found.");
                this.bufferedWriter.newLine();
                this.bufferedWriter.flush();
            }
        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    public void broadcastMessage(String messageToSend){
        for (ClientHandler clientHandler : clientHandlers){
            try{
                if(!clientHandler.clientUsername.equals(clientUsername)){
                    clientHandler.bufferedWriter.write(messageToSend);
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.flush();
                }
            }catch (IOException e){
                closeEverything(socket , bufferedReader , bufferedWriter);
            }
        }
    }

    private void broadcastUserList() {
        StringBuilder sb = new StringBuilder("__USER_LIST__:");
        for (int i = 0; i < clientHandlers.size(); i++) {
            sb.append(clientHandlers.get(i).clientUsername);
            if (i < clientHandlers.size() - 1) sb.append(",");
        }
        String payload = sb.toString();

        for (ClientHandler ch : clientHandlers) {
            try {
                ch.bufferedWriter.write(payload);
                ch.bufferedWriter.newLine();
                ch.bufferedWriter.flush();
            } catch (IOException e) {
                ch.closeEverything(ch.socket, ch.bufferedReader, ch.bufferedWriter);
            }
        }
    }

    public void removeClientHandler(){
        clientHandlers.remove(this);
        userMap.remove(clientUsername);
        broadcastMessage("Server : " + clientUsername + " has left the chat!");
        broadcastUserList();
    }

    public void closeEverything(Socket socket , BufferedReader bufferedReader , BufferedWriter bufferedWriter){
        removeClientHandler();
        try{
            if(bufferedReader != null){
                bufferedReader.close();
            }
            if (bufferedWriter != null){
                bufferedWriter.close();
            }
            if (socket != null){
                socket.close();
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
