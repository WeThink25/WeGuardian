package me.wethink.weGuardian.web;

import me.wethink.weGuardian.WeGuardian;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import java.util.concurrent.CompletableFuture;

@ServerEndpoint("/ws")
public class WebSocketEndpoint {
     
     private static WeGuardian plugin;
     private static WebDashboardService webDashboardService;
     private Session session;
     private String sessionId;
     
     public WebSocketEndpoint() {
     }
     
     public static void setPlugin(WeGuardian plugin) {
         WebSocketEndpoint.plugin = plugin;
     }
     
     public static void setWebDashboardService(WebDashboardService webDashboardService) {
         WebSocketEndpoint.webDashboardService = webDashboardService;
     }
     
     @OnOpen
     public void onOpen(Session session) {
         this.session = session;
         
         plugin.getLogger().info("WebSocket connection established");
         
         CompletableFuture.runAsync(() -> {
             try {
                 webDashboardService.getWebSocketHandler().addConnection(session);
             } catch (Exception e) {
                 plugin.getLogger().severe("Error adding WebSocket connection: " + e.getMessage());
             }
         });
     }
     
     @OnClose
     public void onClose(Session session) {
         plugin.getLogger().info("WebSocket connection closed");
         
         CompletableFuture.runAsync(() -> {
             try {
                 webDashboardService.getWebSocketHandler().removeConnection(session);
             } catch (Exception e) {
                 plugin.getLogger().severe("Error removing WebSocket connection: " + e.getMessage());
             }
         });
     }
     
     @OnError
     public void onError(Session session, Throwable cause) {
         plugin.getLogger().severe("WebSocket error: " + cause.getMessage());
         cause.printStackTrace();
     }
     
     @OnMessage
     public void onMessage(String message) {
         try {
             if ("ping".equals(message)) {
                 if (session != null && session.isOpen()) {
                     session.getBasicRemote().sendText("pong");
                 }
             }
         } catch (Exception e) {
             plugin.getLogger().severe("Error handling WebSocket message: " + e.getMessage());
         }
     }
     
     public void onWebSocketBinary(byte[] payload, int offset, int len) {
         plugin.getLogger().warning("Binary WebSocket messages are not supported");
     }
 }
