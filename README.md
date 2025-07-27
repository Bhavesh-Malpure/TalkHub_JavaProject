
# **TalkHub - Real-Time Chat Application**

## **Overview**
TalkHub is a Java-based real-time chat application built using **socket programming** and **Java Swing**. It supports **group chatting** and **private messaging** between multiple users connected to a centralized server. The project demonstrates the use of **multithreading**, **client-server communication**, and a **custom GUI interface** for seamless communication.

---

## **Features**
- **Group Chat:** All connected users can send and receive messages in real-time.  
- **Private Chat:** One-to-one messaging using a dedicated private chat window.  
- **Online Users List:** Displays active users dynamically.  
- **Timestamps:** Every message shows the time it was sent.  
- **Auto-scroll Toggle:** Option to enable or disable auto-scrolling in chat.  
- **Exit Confirmation Dialog:** Prompts before exiting to prevent accidental closure.  
- **Bold Usernames and Styled Messages:** Usernames are bold, and server messages are italicized.  
- **Color Themes:** Customized UI with peach color combinations for private chat windows.  

---

## **Tools & Technologies**
- **Language:** Java (JDK 17)  
- **GUI Framework:** Java Swing  
- **Networking:** Java Socket Programming  
- **IDE:** IntelliJ IDEA  
- **Collections Framework:** For managing clients and private chat windows.

---

## **Project Structure**
```
java_group_chat/
│
├── src/
│   ├── Server.java          # Handles multiple clients and broadcasts messages
│   ├── ClientHandler.java   # Manages communication for each connected client
│   ├── Client.java          # Console-based client logic
│   └── ChatClientGUI.java   # Swing-based client with enhanced features
```
---

## **Problems Faced & Solutions**
1. **Multiple Client Instances:**  
   - **Problem:** Initially, running multiple clients from the same file was not possible.  
   - **Solution:** Modified run configurations in IntelliJ to allow multiple instances of `Client.java`.

2. **Private Chat Windows:**  
   - **Problem:** Opening multiple private chat windows led to synchronization issues.  
   - **Solution:** Implemented a **map-based structure** to track each user’s private chat window.

3. **Message Display and Formatting:**  
   - **Problem:** Usernames and messages were difficult to distinguish in the chat panel.  
   - **Solution:** Used `StyledDocument` in Swing for **bold usernames** and **italic server messages**.

4. **Auto-scrolling:**  
   - **Problem:** Chat text area did not automatically scroll to the bottom when new messages arrived.  
   - **Solution:** Implemented a `DefaultCaret` with the **ALWAYS_UPDATE** property, combined with a toggle for user control.

5. **Exit Handling:**  
   - **Problem:** Clients exiting abruptly left zombie entries in the active user list.  
   - **Solution:** Properly removed the client from the active list on disconnect with `removeClientHandler()`.

---

## **Future Enhancements**
- **Authentication (Login/Signup)**  
- **Search in Chat** feature  
- **Message encryption** for secure communication  
- **File sharing and multimedia support**  
- **Chat history storage using a database (MySQL)**  

---

## **How to Run**
1. Compile all `.java` files.  
2. Run **Server.java** first.  
3. Run multiple instances of **ChatClientGUI.java** or **Client.java**.  
4. Start chatting in group or initiate a private chat by selecting a user.

---

## **Suggestions**
- Add **screenshots** of the application interface for better presentation.  
- Include a **"Getting Started" section** with detailed setup steps.  
- Add a **"Contributors" section** to credit the developer(s).
