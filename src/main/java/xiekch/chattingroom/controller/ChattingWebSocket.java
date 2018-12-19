package xiekch.chattingroom.controller;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.servlet.http.HttpSession;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;

import org.springframework.web.bind.annotation.*;
import xiekch.chattingroom.domain.*;
import xiekch.chattingroom.service.ChattingService;

@ServerEndpoint("/websocket")
public class ChattingWebSocket {

    private Session userSession;
    private Room room;
    private User user;
    private static CopyOnWriteArraySet<ChattingWebSocket> webSocketSet = new CopyOnWriteArraySet<ChattingWebSocket>();
    private static ConcurrentHashMap<Session, ChattingWebSocket> webSocketMap = new ConcurrentHashMap<Session, ChattingWebSocket>();

    @OnOpen
    public void OnOpen(@RequestParam("roomName") String roomName, Session session, HttpSession httpSession) {
        this.userSession = session;
        webSocketSet.add(this);
        webSocketMap.put(session, this);

        this.room = ChattingService.getInstance().getRoom(roomName);
        this.user = (User) httpSession.getAttribute("user");
        ChattingService.getInstance().userEnterRoom(room, user);

        System.out.println("new user entered room: " + roomName);
    }

    @OnClose
    public void OnClose(Session session) {
        webSocketSet.remove(this);
        webSocketMap.remove(session);

        System.out.println("a user quit the room" + this.room.getName());
    }

    @OnMessage
    public void OnMessage(String message, Session session) throws Exception {
        sendMessageToRoom(session, message);;
    }

    @OnError
    public void OnError(Session session, Throwable error) {
        error.printStackTrace();
    }

    public void sendMessageToRoom(Session session, String message) {
        ChattingWebSocket myWebSocket = webSocketMap.get(session);
        String userName = myWebSocket.user.getName();
        String roomName = myWebSocket.room.getName();
        Message mess = ChattingService.getInstance().userSpeak(userName, roomName, message);

        for (ChattingWebSocket chat : webSocketMap.values()) {
            if (chat.room.getName().equals(roomName)) {
                chat.sendMessage(mess.getName()+" "+mess.getDate()+" "+mess.getMessage());
            }
        }
    }

    public void sendMessage(String message) {
        try {
            if (this.userSession.isOpen()) {
                this.userSession.getBasicRemote().sendText(message);
            }
        } catch (IOException error) {
            error.printStackTrace();
        }
    }
}