/**
 * chat.js — Nexus Chat WebSocket client
 */
(function () {
    'use strict';

    // ── DOM refs ───────────────────────────────────────────────────────────────
    var msgInput           = document.getElementById('msgInput');
    var sendBtn            = document.getElementById('sendBtn');
    var messagesArea       = document.getElementById('messagesArea');
    var dynamicMessages    = document.getElementById('dynamicMessages');
    var typingIndicator    = document.getElementById('typingIndicator');
    var typingText         = document.getElementById('typingText');
    var wsDot              = document.getElementById('wsDot');
    var connStatus         = document.getElementById('connStatus');
    var disconnectedBanner = document.getElementById('disconnectedBanner');

    // ── State ──────────────────────────────────────────────────────────────────
    var stompClient = null;
    var isConnected = false;
    var typingTimer = null;
    var typingUsers = {};

    // ── Helpers ────────────────────────────────────────────────────────────────
    function scrollToBottom() {
        if (messagesArea) {
            messagesArea.scrollTop = messagesArea.scrollHeight;
        }
    }

    function esc(str) {
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    // ── Update send button — only depends on whether input has text ────────────
    // NOTE: We do NOT gate this on isConnected so the button is always
    //       clickable when there is text. If WS is down we show an alert.
    function updateSendBtn() {
        var hasText      = msgInput.value.trim().length > 0;
        sendBtn.disabled = !hasText;
    }

    // ── Connection UI ──────────────────────────────────────────────────────────
    function setConnectionUI(online) {
        isConnected = online;

        if (wsDot) {
            wsDot.className = 'ws-dot ' + (online ? 'online' : 'offline');
            wsDot.title     = online ? 'Connected' : 'Disconnected';
        }
        if (connStatus) {
            connStatus.textContent = online ? '● online' : '● offline';
            connStatus.style.color = online ? '#22c55e' : '#ef4444';
        }
        if (disconnectedBanner) {
            disconnectedBanner.style.display = online ? 'none' : 'block';
        }
    }

    // ── Connect ────────────────────────────────────────────────────────────────
    function connect() {
        stompClient = new StompJs.Client({
            webSocketFactory: function () {
                return new SockJS('/ws');
            },
            reconnectDelay: 5000,

            onConnect: function () {
                console.log('WebSocket connected ✅');
                setConnectionUI(true);
                updateSendBtn(); // re-evaluate button now that we're connected

                // Subscribe to room messages
                stompClient.subscribe('/topic/room/' + ROOM_ID, function (frame) {
                    try {
                        appendMessage(JSON.parse(frame.body));
                    } catch (e) {
                        console.error('Failed to parse message:', e);
                    }
                });

                // Subscribe to typing indicators
                stompClient.subscribe('/topic/room/' + ROOM_ID + '/typing', function (frame) {
                    try { handleTyping(JSON.parse(frame.body)); } catch (e) {}
                });

                // Announce presence
                stompClient.publish({
                    destination: '/app/presence',
                    body: JSON.stringify({ status: 'ONLINE' })
                });
            },

            onDisconnect: function () {
                console.warn('WebSocket disconnected');
                setConnectionUI(false);
            },

            onStompError: function (frame) {
                console.error('STOMP error:', frame);
                setConnectionUI(false);
            },

            onWebSocketError: function (e) {
                console.error('WebSocket error:', e);
                setConnectionUI(false);
            }
        });

        stompClient.activate();
    }

    // ── Send message ───────────────────────────────────────────────────────────
    function sendMessage() {
        var content = msgInput.value.trim();
        if (!content) return;

        if (!isConnected || !stompClient) {
            // WebSocket not ready — show error and do NOT clear input
            alert('Not connected to chat server. Please wait or refresh the page.');
            return;
        }

        stompClient.publish({
            destination: '/app/chat/' + ROOM_ID + '/send',
            body: JSON.stringify({ content: content })
        });

        msgInput.value = '';
        updateSendBtn();
        sendTyping(false);
        msgInput.focus();
    }

    // ── Append a received message to the DOM ───────────────────────────────────
    function appendMessage(msg) {
        var isOwn   = (msg.senderUsername === MY_USERNAME) || (msg.senderName === MY_NAME);
        var color   = msg.senderColor || '#4ECDC4';
        var initial = (msg.senderName || '?')[0].toUpperCase();
        var time    = '';

        if (msg.sentAt) {
            try {
                time = new Date(msg.sentAt).toLocaleTimeString([], {
                    hour: '2-digit', minute: '2-digit'
                });
            } catch (e) {}
        }

        var row = document.createElement('div');
        row.className = 'msg-row' + (isOwn ? ' own' : '');
        row.innerHTML =
            '<div class="avatar" style="background:' + color + '">' +
            initial +
            '</div>' +
            '<div class="msg-body">' +
            '<div class="msg-meta">' +
            '<span class="msg-author"' + (isOwn ? ' style="color:#22c55e"' : '') + '>' +
            esc(msg.senderName || 'Unknown') +
            '</span>' +
            '<span class="msg-time">' + time + '</span>' +
            '</div>' +
            '<div class="msg-text">' + esc(msg.content || '') + '</div>' +
            '</div>';

        dynamicMessages.appendChild(row);
        scrollToBottom();
    }

    // ── Typing ─────────────────────────────────────────────────────────────────
    function sendTyping(isTyping) {
        if (!isConnected || !stompClient) return;
        stompClient.publish({
            destination: '/app/typing/' + ROOM_ID,
            body: JSON.stringify({ typing: isTyping })
        });
    }

    function handleTyping(data) {
        if (data.username === MY_USERNAME) return;
        typingUsers[data.username] = data.typing;
        clearTimeout(typingTimer);
        if (data.typing) {
            typingTimer = setTimeout(function () {
                typingUsers[data.username] = false;
                renderTyping();
            }, 3500);
        }
        renderTyping();
    }

    function renderTyping() {
        if (!typingIndicator) return;
        var active = Object.keys(typingUsers).filter(function (u) { return typingUsers[u]; });
        if (active.length === 0) {
            typingIndicator.style.display = 'none';
        } else {
            if (typingText) {
                typingText.textContent = active.join(', ') +
                    (active.length === 1 ? ' is typing…' : ' are typing…');
            }
            typingIndicator.style.display = 'flex';
            scrollToBottom();
        }
    }

    // ── Events ─────────────────────────────────────────────────────────────────
    msgInput.addEventListener('input', function () {
        updateSendBtn();       // enable/disable based on text only
        sendTyping(msgInput.value.trim().length > 0);
    });

    msgInput.addEventListener('keydown', function (e) {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            sendMessage();
        }
    });

    sendBtn.addEventListener('click', function () {
        sendMessage();
    });

    window.addEventListener('beforeunload', function () {
        if (isConnected && stompClient) {
            stompClient.publish({
                destination: '/app/presence',
                body: JSON.stringify({ status: 'OFFLINE' })
            });
            stompClient.deactivate();
        }
    });

    // ── Boot ───────────────────────────────────────────────────────────────────
    scrollToBottom();
    connect();

})();