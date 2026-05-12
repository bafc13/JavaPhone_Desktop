package com.mycompany.javaphone_nir2.cryptography;

import com.mycompany.javaphone_nir2.models.SettingsManager;
import javax.crypto.Cipher;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

/**
 * Singleton-класс для шифрования с открытым ключом (RSA). Получает путь к файлу
 * с ключевой парой из SettingsManager. Если файл отсутствует, getInstance()
 * вернёт null.
 */
public class MessageCryptographer {

    private static volatile MessageCryptographer instance;
    private static final Object LOCK = new Object();

    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    private MessageCryptographer(PrivateKey privateKey, PublicKey publicKey) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }

    /**
     * Возвращает единственный экземпляр класса. При первом вызове пытается
     * загрузить ключевую пару из файла, путь к которому задан в
     * SettingsManager.userKeyProperty. Если файл не существует, instance
     * устанавливается в null и возвращается null.
     */
    public static MessageCryptographer getInstance() {
        if (instance == null) {
            SettingsManager s = SettingsManager.getInstance();
            synchronized (LOCK) {
                if (instance == null) {
                    String keyFilePath = s.getUserKey();
                    File keyFile = new File(keyFilePath);
                    if (!keyFile.exists()) {
                        initializeKey(s.getNickname(), s.getNickname() + "@example.com", keyFile);
                    }
                    try {
                        KeyStoreData data = loadKeyStoreData(keyFile);
                        if (!(data.getUserName().equals(s.getNickname()) || !(data.getUserEmail()).equals(s.getEmail()))) {
                            throw new Exception("User key not verified");
                        }

                        instance = new MessageCryptographer(data.getPrivateKey(), data.getPublicKey());
                    } catch (Exception e) {
                        e.printStackTrace();
                        instance = null;
                    }
                }
            }
        }
        return instance;
    }

    /**
     * Генерирует новую пару ключей RSA (2048 бит), привязывает к ней имя и
     * email пользователя и сохраняет в файл, путь к которому берётся из
     * SettingsManager.userKeyProperty. После успешной генерации файл становится
     * доступным для загрузки через getInstance().
     *
     * @param userName имя пользователя
     * @param userEmail email пользователя
     */
    public static void initializeKey(String userName, String userEmail, File keyFile) {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            KeyPair keyPair = keyGen.generateKeyPair();

            KeyStoreData data = new KeyStoreData(
                    keyPair.getPrivate(),
                    keyPair.getPublic(),
                    userName,
                    userEmail
            );
            saveKeyStoreData(data, keyFile);
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("Failed to initialize key pair", e);
        }
    }

    /**
     * Гибридное шифрование: сообщение шифруется симметричным ключом AES, а сам
     * ключ шифруется публичным RSA-ключом получателя. Возвращает строку вида:
     * {зашифрованный AES-ключ} + ":" + {IV} + ":" + {зашифрованное сообщение},
     * где все компоненты закодированы в Base64.
     */
    public String encryptMessage(String message, PublicKey publicKey) {
        try {
            // 1. Генерация симметричного ключа AES (256 бит)
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256);
            SecretKey aesKey = keyGen.generateKey();

            // 2. Шифрование сообщения алгоритмом AES/CBC/PKCS5Padding
            Cipher aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            aesCipher.init(Cipher.ENCRYPT_MODE, aesKey);
            byte[] iv = aesCipher.getIV();  // случайный вектор инициализации
            byte[] encryptedMessage = aesCipher.doFinal(message.getBytes(StandardCharsets.UTF_8));

            // 3. Шифрование симметричного ключа публичным RSA-ключом
            Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedAesKey = rsaCipher.doFinal(aesKey.getEncoded());

            // 4. Сборка результата: ключ:IV:сообщение (все в Base64)
            String encKeyB64 = Base64.getEncoder().encodeToString(encryptedAesKey);
            String ivB64 = Base64.getEncoder().encodeToString(iv);
            String encMsgB64 = Base64.getEncoder().encodeToString(encryptedMessage);

            return encKeyB64 + ":" + ivB64 + ":" + encMsgB64;
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Hybrid encryption failed", e);
        }
    }

    /**
     * Подписывает сообщение: вычисляет SHA-256 хеш и шифрует его приватным
     * ключом.
     *
     * @param message исходное сообщение
     * @return Base64-строка зашифрованного хеша
     */
    public String signMessage(String message) {
        try {
            byte[] hash = hash(message);
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, this.privateKey);
            byte[] encryptedHash = cipher.doFinal(hash);
            return Base64.getEncoder().encodeToString(encryptedHash);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Signing failed", e);
        }
    }

    /**
     * Расшифровывает сообщение, зашифрованное гибридным методом encryptMessage.
     * Ожидает строку формата: зашифрованныйAES-ключ:IV:зашифрованноеСообщение
     * (Base64). Расшифровывает AES-ключ приватным RSA-ключом, затем
     * расшифровывает сообщение.
     */
    public String decryptMessage(String encryptedData) {
        try {
            // 1. Разбор составной строки
            String[] parts = encryptedData.split(":");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid encrypted data format");
            }
            byte[] encAesKey = Base64.getDecoder().decode(parts[0]);
            byte[] iv = Base64.getDecoder().decode(parts[1]);
            byte[] encMessage = Base64.getDecoder().decode(parts[2]);

            // 2. Расшифровка AES-ключа приватным RSA-ключом
            Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            rsaCipher.init(Cipher.DECRYPT_MODE, this.privateKey);
            byte[] aesKeyBytes = rsaCipher.doFinal(encAesKey);
            SecretKey aesKey = new javax.crypto.spec.SecretKeySpec(aesKeyBytes, "AES");

            // 3. Расшифровка сообщения
            Cipher aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            aesCipher.init(Cipher.DECRYPT_MODE, aesKey, new IvParameterSpec(iv));
            byte[] decryptedMessage = aesCipher.doFinal(encMessage);

            return new String(decryptedMessage, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Hybrid decryption failed", e);
        }
    }

    /**
     * Расшифровывает подпись (зашифрованный хеш) публичным ключом и возвращает
     * хеш в виде hex-строки. Вызывающая сторона должна сама вычислить хеш от
     * предполагаемого исходного сообщения и сравнить с полученным значением.
     *
     * @param encryptedHashBase64 Base64-строка, полученная от signMessage
     * @param publicKey публичный ключ отправителя
     * @return hex-строка расшифрованного хеша (64 символа для SHA-256)
     */
    public boolean confirmSign(String message, String encryptedHashBase64, PublicKey publicKey) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, publicKey);
            byte[] decryptedHash = cipher.doFinal(Base64.getDecoder().decode(encryptedHashBase64));
            byte[] calculatedHash = hash(message);
            return Arrays.equals(calculatedHash, decryptedHash);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Signature verification failed", e);
        }
    }

    // ---------- Вспомогательные методы для работы с файлом ----------
    private static KeyStoreData loadKeyStoreData(File file) throws IOException, GeneralSecurityException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            Object obj = ois.readObject();
            if (obj instanceof KeyStoreData) {
                return (KeyStoreData) obj;
            } else {
                throw new IOException("Invalid key store format");
            }
        } catch (ClassNotFoundException e) {
            throw new IOException("Class not found when deserializing key store", e);
        }
    }

    private static void saveKeyStoreData(KeyStoreData data, File file) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(data);
        }
    }

    public static String publicKeyToString(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    public static PublicKey stringToPublicKey(String keyStr) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(keyStr);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA"); // или "EC", в зависимости от алгоритма
            return keyFactory.generatePublic(new X509EncodedKeySpec(keyBytes));
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Invalid public key string", e);
        }
    }

    private byte[] hash(String message) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(message.getBytes(StandardCharsets.UTF_8));
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public String getPublicKeyString() {
        return publicKeyToString(publicKey);
    }

    /**
     * Внутренний класс-контейнер для хранения пары ключей и метаданных.
     * Сериализуется в файл.
     */
    private static class KeyStoreData implements Serializable {

        private static final long serialVersionUID = 1L;

        private final PrivateKey privateKey;
        private final PublicKey publicKey;
        private final String userName;
        private final String userEmail;

        public KeyStoreData(PrivateKey privateKey, PublicKey publicKey, String userName, String userEmail) {
            this.privateKey = privateKey;
            this.publicKey = publicKey;
            this.userName = userName;
            this.userEmail = userEmail;
        }

        public PrivateKey getPrivateKey() {
            return privateKey;
        }

        public PublicKey getPublicKey() {
            return publicKey;
        }

        public String getUserName() {
            return userName;
        }

        public String getUserEmail() {
            return userEmail;
        }
    }
}
