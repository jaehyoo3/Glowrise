import SockJS from 'sockjs-client';
import {Client} from '@stomp/stompjs';

let stompClient = null;
const subscribers = [];

export const websocketService = {
    connect(userId, onMessageReceived) {
        if (stompClient && stompClient.connected) {
            console.log('WebSocket already connected');
            return;
        }

        const socketFactory = () => new SockJS('http://localhost:8080/ws');
        const token = localStorage.getItem('accessToken');

        stompClient = new Client({
            webSocketFactory: socketFactory,
            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
            connectHeaders: {
                Authorization: `Bearer ${token}`, // JWT 토큰 추가
            },
            debug: (str) => {
                console.log('STOMP Debug:', str);
            },
        });

        stompClient.onConnect = () => {
            console.log('WebSocket connected');
            stompClient.subscribe(`/topic/notifications/${userId}`, (message) => {
                const notification = JSON.parse(message.body);
                console.log('Received notification:', notification);
                onMessageReceived(notification);
            });
        };

        stompClient.onStompError = (frame) => {
            console.error('STOMP Error:', frame);
        };

        stompClient.onDisconnect = () => {
            console.log('WebSocket disconnected');
        };

        stompClient.activate();
    },

    disconnect() {
        if (stompClient && stompClient.connected) {
            stompClient.deactivate(() => {
                console.log('WebSocket disconnected');
            });
        }
    },

    addSubscriber(callback) {
        subscribers.push(callback);
    },

    removeSubscriber(callback) {
        const index = subscribers.indexOf(callback);
        if (index !== -1) {
            subscribers.splice(index, 1);
        }
    },
};