package com.example.controller;

import com.example.model.Human;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void notifyHumanCreated(Human human) {
        messagingTemplate.convertAndSend("/topic/human-s", 
            new WebSocketMessage("CREATED", human));
    }

    public void notifyHumanUpdated(Human human) {
        messagingTemplate.convertAndSend("/topic/human-s", 
            new WebSocketMessage("UPDATED", human));
    }

    public void notifyHumanDeleted(Long id) {
        messagingTemplate.convertAndSend("/topic/human-s", 
            new WebSocketMessage("DELETED", id));
    }

    public static class WebSocketMessage {
        private String type;
        private Object data;

        public WebSocketMessage() {}

        public WebSocketMessage(String type, Object data) {
            this.type = type;
            this.data = data;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Object getData() {
            return data;
        }

        public void setData(Object data) {
            this.data = data;
        }
    }
}
