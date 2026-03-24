package com.mycompany.javaphone_nir2.db;

import com.mycompany.javaphone_nir2.models.*;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class DatabaseManager {
    private final String url;

    public DatabaseManager(String dbFilePath) {
        this.url = "jdbc:sqlite:" + dbFilePath;
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new DatabaseException("SQLite JDBC driver not found", e);
        }
    }

    private Connection connect() throws SQLException {
        Connection conn = DriverManager.getConnection(url);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }
    
    public void createTables() {
        String sql;
        try (InputStream is = getClass().getResourceAsStream("/0000_javaphone_create_db.sql")) {
            if (is == null) {
                throw new DatabaseException("Resource not found: /0000_javaphone_create_db.sql");
            }
            try (Scanner scanner = new Scanner(is, "UTF-8").useDelimiter("\\A")) {
                sql = scanner.hasNext() ? scanner.next() : "";
            }
        } catch (IOException e) {
            throw new DatabaseException("Failed to read schema resource", e);
        }

        if (sql.isEmpty()) {
            throw new DatabaseException("Schema resource is empty");
        }

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to create tables", e);
        }
    }
    
    public void deleteAllData() {
        String[] tables = {"attachments", "messages", "chats_users", "chats", "media", "users"};
        try (Connection conn = connect()) {
            // Disable foreign key checks temporarily to simplify deletion order
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = OFF;");
            }
            for (String table : tables) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("DELETE FROM " + table);
                }
            }
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON;");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete all data", e);
        }
    }

    public boolean checkUserExists(String publicKey) {
        String sql = "SELECT 1 FROM users WHERE public_key = ?";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, publicKey);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error checking user existence", e);
        }
    }

    public void updateUserName(String publicKey, String newName) {
        String sql = "UPDATE users SET name = ? WHERE public_key = ?";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newName);
            pstmt.setString(2, publicKey);
            int affected = pstmt.executeUpdate();
            if (affected == 0) {
                throw new DatabaseException("User not found: " + publicKey);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error updating user name", e);
        }
    }

    public void addUser(User user) {
        String sql = "INSERT INTO users (public_key, name, email, ip, avatar_id) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getPublicKey());
            pstmt.setString(2, user.getName());
            pstmt.setString(3, user.getEmail());
            pstmt.setString(4, user.getIp());
            if (user.getAvatarId() != null) {
                pstmt.setInt(5, user.getAvatarId());
            } else {
                pstmt.setNull(5, Types.INTEGER);
            }
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error adding user", e);
        }
    }

    public User findUserByPublicKey(String publicKey) {
        String sql = "SELECT public_key, name, email, ip, avatar_id FROM users WHERE public_key = ?";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, publicKey);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setPublicKey(rs.getString("public_key"));
                    user.setName(rs.getString("name"));
                    user.setEmail(rs.getString("email"));
                    user.setIp(rs.getString("ip"));
                    int avatarId = rs.getInt("avatar_id");
                    if (!rs.wasNull()) {
                        user.setAvatarId(avatarId);
                    }
                    return user;
                }
                return null;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding user by public key", e);
        }
    }

    public List<User> findUserByName(String name) {
        String sql = "SELECT public_key, name, email, ip, avatar_id FROM users WHERE name = ?";
        List<User> users = new ArrayList<>();
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    User user = new User();
                    user.setPublicKey(rs.getString("public_key"));
                    user.setName(rs.getString("name"));
                    user.setEmail(rs.getString("email"));
                    user.setIp(rs.getString("ip"));
                    int avatarId = rs.getInt("avatar_id");
                    if (!rs.wasNull()) {
                        user.setAvatarId(avatarId);
                    }
                    users.add(user);
                }
            }
            return users;
        } catch (SQLException e) {
            throw new DatabaseException("Error finding user by name", e);
        }
    }

    public List<User> getAllUsers() {
        String sql = "SELECT public_key, name, email, ip, avatar_id FROM users";
        List<User> users = new ArrayList<>();
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                User user = new User();
                user.setPublicKey(rs.getString("public_key"));
                user.setName(rs.getString("name"));
                user.setEmail(rs.getString("email"));
                user.setIp(rs.getString("ip"));
                int avatarId = rs.getInt("avatar_id");
                if (!rs.wasNull()) {
                    user.setAvatarId(avatarId);
                }
                users.add(user);
            }
            return users;
        } catch (SQLException e) {
            throw new DatabaseException("Error getting all users", e);
        }
    }

    /**
     * Get or create a direct message (dm) chat between two users.
     * @param userPublicKey1 first participant
     * @param userPublicKey2 second participant
     * @return chat id
     */
    public int getOrCreateDmChat(String userPublicKey) {
        // Check if a dm chat already exists with exactly these two participants
        String findSql = "SELECT c.id FROM chats c " +
                "INNER JOIN chats_users cu ON c.id = cu.chat_id " +
                "WHERE c.type = 'dm' ";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(findSql)) {
            pstmt.setString(1, userPublicKey);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding dm chat", e);
        }

        try (Connection conn = connect()) {
            conn.setAutoCommit(false);
            try {
                // Insert chat
                String insertChatSql = "INSERT INTO chats (type, host_public_key) VALUES ('dm', ?)";
                int chatId;
                try (PreparedStatement pstmt = conn.prepareStatement(insertChatSql, Statement.RETURN_GENERATED_KEYS)) {
                    pstmt.setString(1, userPublicKey); // host can be first user
                    pstmt.executeUpdate();
                    try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                        if (!generatedKeys.next()) {
                            throw new SQLException("Creating chat failed, no ID obtained.");
                        }
                        chatId = generatedKeys.getInt(1);
                    }
                }

                // Insert both participants
                String insertCuSql = "INSERT INTO chats_users (chat_id, user_public_key) VALUES (?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(insertCuSql)) {
//                    Add function to get own public key in key manager
//                    String myPublicKey = getOwnPublicKey()
//                    pstmt.setInt(1, chatId);
//                    pstmt.setString(2, myPublicKey);
                    pstmt.executeUpdate();
                    pstmt.setString(2, userPublicKey);
                    pstmt.executeUpdate();
                }

                conn.commit();
                return chatId;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error creating dm chat", e);
        }
    }

    public List<User> getChatParticipants(int chatId) {
        String sql = "SELECT u.public_key, u.name, u.email, u.ip, u.avatar_id " +
                "FROM users u " +
                "INNER JOIN chats_users cu ON u.public_key = cu.user_public_key " +
                "WHERE cu.chat_id = ?";
        List<User> users = new ArrayList<>();
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, chatId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    User user = new User();
                    user.setPublicKey(rs.getString("public_key"));
                    user.setName(rs.getString("name"));
                    user.setEmail(rs.getString("email"));
                    user.setIp(rs.getString("ip"));
                    int avatarId = rs.getInt("avatar_id");
                    if (!rs.wasNull()) {
                        user.setAvatarId(avatarId);
                    }
                    users.add(user);
                }
            }
            return users;
        } catch (SQLException e) {
            throw new DatabaseException("Error getting chat participants", e);
        }
    }

    public int addMessage(int chatId, String senderPublicKey, String content, long time) {
        String sql = "INSERT INTO messages (chat_id, sender_public_key, content, time) VALUES (?, ?, ?, ?)";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, chatId);
            pstmt.setString(2, senderPublicKey);
            pstmt.setString(3, content);
            pstmt.setLong(4, time);
            pstmt.executeUpdate();
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Creating message failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error adding message", e);
        }
    }

    public Message getLastMessage(int chatId) {
        String sql = "SELECT id, chat_id, sender_public_key, content, time FROM messages " +
                "WHERE chat_id = ? ORDER BY time DESC LIMIT 1";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, chatId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Message msg = new Message();
                    msg.setId(rs.getInt("id"));
                    msg.setChatId(rs.getInt("chat_id"));
                    msg.setSenderPublicKey(rs.getString("sender_public_key"));
                    msg.setContent(rs.getString("content"));
                    msg.setTime(rs.getLong("time"));
                    return msg;
                }
                return null;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error getting last message", e);
        }
    }

    public List<Message> getChatHistory(int chatId) {
        String sql = "SELECT id, chat_id, sender_public_key, content, time FROM messages " +
                "WHERE chat_id = ? ORDER BY time ASC";
        List<Message> messages = new ArrayList<>();
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, chatId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Message msg = new Message();
                    msg.setId(rs.getInt("id"));
                    msg.setChatId(rs.getInt("chat_id"));
                    msg.setSenderPublicKey(rs.getString("sender_public_key"));
                    msg.setContent(rs.getString("content"));
                    msg.setTime(rs.getLong("time"));
                    messages.add(msg);
                }
            }
            return messages;
        } catch (SQLException e) {
            throw new DatabaseException("Error getting chat history", e);
        }
    }

    public int addMedia(String path, String checksum) {
        String sql = "INSERT INTO media (path, checksum) VALUES (?, ?)";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, path);
            pstmt.setString(2, checksum);
            pstmt.executeUpdate();
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Creating media failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error adding media", e);
        }
    }

    public Media findMediaByChecksum(String checksum) {
        String sql = "SELECT id, path, checksum FROM media WHERE checksum = ?";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, checksum);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Media media = new Media();
                    media.setId(rs.getInt("id"));
                    media.setPath(rs.getString("path"));
                    media.setChecksum(rs.getString("checksum"));
                    return media;
                }
                return null;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding media by checksum", e);
        }
    }

    public void attachMediaToMessage(int messageId, int mediaId) {
        String sql = "INSERT INTO attachments (message_id, media_id) VALUES (?, ?)";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, messageId);
            pstmt.setInt(2, mediaId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error attaching media to message", e);
        }
    }

    public static class DatabaseException extends RuntimeException {

        private static final long serialVersionUID = 1L;
        public DatabaseException(String message, Throwable cause) {
            super(message, cause);
        }
        public DatabaseException(String message) {
            super(message);
        }
    }
}