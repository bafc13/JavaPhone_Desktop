package com.mycompany.javaphone_nir2.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.annotation.JsonProperty;

import javafx.beans.property.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import com.mycompany.javaphone_nir2.webrtc.WebRTCManager;
import dev.onvoid.webrtc.media.audio.AudioDevice;
import dev.onvoid.webrtc.media.video.VideoDevice;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SettingsManager {

    private static SettingsManager instance;

    private static final String SETTINGS_FOLDER = ".javaphone";
    private static final String CONFIG_FILE = "app_settings.json";
    private static final String AVATARS_FOLDER = "avatars";

    private final Path settingsPath;
    private final ObjectMapper objectMapper; // 🔥 Jackson вместо Gson

    private boolean registered;
    private SettingsData data;

    // === REACTIVE PROPERTIES (transient для Jackson) ===
    private transient final StringProperty nicknameProperty = new SimpleStringProperty(this, "nickname");
    private transient final StringProperty userIpProperty = new SimpleStringProperty(this, "userIp");
    private transient final StringProperty userPortProperty = new SimpleStringProperty(this, "userPort");
    private transient final StringProperty userKeyProperty = new SimpleStringProperty(this, "userKey");
    private transient final StringProperty themeProperty = new SimpleStringProperty(this, "theme");
    private transient final StringProperty pathToAvatarProperty = new SimpleStringProperty(this, "pathToAvatar");
    private transient final BooleanProperty notificationsEnabledProperty = new SimpleBooleanProperty(this, "notificationsEnabled");
    private transient final StringProperty microphoneProperty = new SimpleStringProperty(this, "microphone");
    private transient final StringProperty speakerProperty = new SimpleStringProperty(this, "speaker");
    private transient final IntegerProperty microphoneVolumeProperty = new SimpleIntegerProperty(this, "microphoneVolume");
    private transient final IntegerProperty speakerVolumeProperty = new SimpleIntegerProperty(this, "speakerVolume");
    private transient final IntegerProperty audioBitrateProperty = new SimpleIntegerProperty(this, "audioBitrate");
    private transient final StringProperty cameraProperty = new SimpleStringProperty(this, "camera");
    private transient final IntegerProperty cameraBitrateProperty = new SimpleIntegerProperty(this, "cameraBitrate");
    private transient final StringProperty signalingServerIpProperty = new SimpleStringProperty(this, "signalingServerIp");

    private SettingsManager() {
        // 🔥 Инициализация Jackson ObjectMapper
        this.objectMapper = new ObjectMapper();

        this.settingsPath = Paths.get(System.getProperty("user.home"), SETTINGS_FOLDER, CONFIG_FILE);
        this.data = new SettingsData();

        // === РЕГИСТРАЦИЯ СЛУШАТЕЛЕЙ: свойство → data ===
        nicknameProperty.addListener((obs, old, newVal) -> { data.nickname = newVal; });
        userIpProperty.addListener((obs, old, newVal) -> { data.userIp = newVal; });
        userPortProperty.addListener((obs, old, newVal) -> { data.userPort = newVal; });
        userKeyProperty.addListener((obs, old, newVal) -> { data.userKey = newVal; });
        themeProperty.addListener((obs, old, newVal) -> { data.theme = newVal; });
        pathToAvatarProperty.addListener((obs, old, newVal) -> { data.pathToAvatar = newVal; });
        notificationsEnabledProperty.addListener((obs, old, newVal) -> { data.notificationsEnabled = newVal; });
        microphoneProperty.addListener((obs, old, newVal) -> { data.microphone = newVal; });
        speakerProperty.addListener((obs, old, newVal) -> { data.speaker = newVal; });

        speakerVolumeProperty.addListener((obs, oldVal, newVal) -> {
            data.speakerVolume = (int) newVal;
            WebRTCManager.getInstance().setSpeakerVolume((Integer) newVal);
        });

        microphoneVolumeProperty.addListener((obs, oldVal, newVal) -> {
            data.microphoneVolume = (int) newVal;
            WebRTCManager.getInstance().setMicrophoneVolume((Integer) newVal);
        });

        audioBitrateProperty.addListener((obs, old, newVal) -> { data.audioBitrate = (int) newVal; });
        cameraProperty.addListener((obs, old, newVal) -> { data.camera = newVal; });
        cameraBitrateProperty.addListener((obs, old, newVal) -> { data.cameraBitrate = (int) newVal; });
        signalingServerIpProperty.addListener((obs, old, newVal) -> { data.signalingServerIp = newVal; });

        loadSettings();
    }

    public static synchronized SettingsManager getInstance() {
        if (instance == null) {
            instance = new SettingsManager();
        }
        return instance;
    }

    private void syncPropertiesWithData() {
        nicknameProperty.set(data.nickname);
        userIpProperty.set(data.userIp);
        userPortProperty.set(data.userPort);
        userKeyProperty.set(data.userKey);
        themeProperty.set(data.theme);
        pathToAvatarProperty.set(data.pathToAvatar);
        notificationsEnabledProperty.set(data.notificationsEnabled);
        microphoneProperty.set(data.microphone);
        speakerProperty.set(data.speaker);
        microphoneVolumeProperty.set(data.microphoneVolume);
        speakerVolumeProperty.set(data.speakerVolume);
        audioBitrateProperty.set(data.audioBitrate);
        cameraProperty.set(data.camera);
        cameraBitrateProperty.set(data.cameraBitrate);
        signalingServerIpProperty.set(data.signalingServerIp);
    }

    // === PUBLIC API (без изменений — полная обратная совместимость) ===
    public boolean isRegistered() { return registered; }

    public String getNickname() { return data.nickname; }
    public void setNickname(String nickname) { nicknameProperty.set(nickname); }
    public StringProperty nicknameProperty() { return nicknameProperty; }

    public String getUserIp() { return data.userIp; }
    public void setUserIp(String ip) { userIpProperty.set(ip); }
    public StringProperty userIpProperty() { return userIpProperty; }

    public String getUserPort() { return data.userPort; }
    public void setUserPort(String port) { userPortProperty.set(port); }
    public StringProperty userPortProperty() { return userPortProperty; }

    public String getUserKey() { return data.userKey; }
    public void setUserKey(String key) { userKeyProperty.set(key); }
    public StringProperty userKeyProperty() { return userKeyProperty; }

    public String getTheme() { return data.theme; }
    public void setTheme(String theme) { themeProperty.set(theme); }
    public StringProperty themeProperty() { return themeProperty; }

    public String getPathToAvatar() { return data.pathToAvatar; }
    public void setPathToAvatar(String path) { pathToAvatarProperty.set(path); }
    public StringProperty pathToAvatarProperty() { return pathToAvatarProperty; }

    public boolean isNotificationsEnabled() { return data.notificationsEnabled; }
    public void setNotificationsEnabled(boolean enabled) { notificationsEnabledProperty.set(enabled); }
    public BooleanProperty notificationsEnabledProperty() { return notificationsEnabledProperty; }

    public String getMicrophone() { return data.microphone; }
    public void setMicrophone(String mic) { microphoneProperty.set(mic); }
    public StringProperty microphoneProperty() { return microphoneProperty; }

    public String getSpeaker() { return data.speaker; }
    public void setSpeaker(String speaker) { speakerProperty.set(speaker); }
    public StringProperty speakerProperty() { return speakerProperty; }

    public int getMicrophoneVolume() { return data.microphoneVolume; }
    public void setMicrophoneVolume(int vol) { microphoneVolumeProperty.set(vol); }
    public IntegerProperty microphoneVolumeProperty() { return microphoneVolumeProperty; }

    public int getSpeakerVolume() { return data.speakerVolume; }
    public void setSpeakerVolume(int vol) { speakerVolumeProperty.set(vol); }
    public IntegerProperty speakerVolumeProperty() { return speakerVolumeProperty; }

    public int getAudioBitrate() { return data.audioBitrate; }
    public void setAudioBitrate(int bitrate) { audioBitrateProperty.set(bitrate); }
    public IntegerProperty audioBitrateProperty() { return audioBitrateProperty; }

    public String getCamera() { return data.camera; }
    public void setCamera(String cam) { cameraProperty.set(cam); }
    public StringProperty cameraProperty() { return cameraProperty; }

    public int getCameraBitrate() { return data.cameraBitrate; }
    public void setCameraBitrate(int bitrate) { cameraBitrateProperty.set(bitrate); }
    public IntegerProperty cameraBitrateProperty() { return cameraBitrateProperty; }

    public String getSignalingServerIp() { return data.signalingServerIp; }
    public void setSignalingServerIp(String ip) { signalingServerIpProperty.set(ip); }
    public StringProperty signalingServerIpProperty() { return signalingServerIpProperty; }

    
    public void save() {
        try {
            Files.createDirectories(settingsPath.getParent());
            // writerWithDefaultPrettyPrinter() даёт форматирование локально для этого вызова
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
            Files.writeString(settingsPath, json);
            System.out.println("Settings saved: " + settingsPath);
        } catch (JsonProcessingException e) {
            System.err.println("Error serializing settings: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error saving settings: " + e.getMessage());
        }
    }

    private void loadSettings() {
        if (!Files.exists(settingsPath)) {
            System.out.println("Using default settings, JSON not found.");
            setDefaults();
            return;
        }

        try {
            // 🔥 Jackson: десериализация из файла
            String json = Files.readString(settingsPath);
            SettingsData loaded = objectMapper.readValue(json, SettingsData.class);

            if (loaded != null) {
                this.data = loaded;
                this.registered = true;
                syncPropertiesWithData();
            } else {
                setDefaults();
            }
        } catch (JsonProcessingException e) {
            System.err.println("Error parsing settings JSON: " + e.getMessage() + "\nSetting default values");
            setDefaults();
        } catch (IOException e) {
            System.err.println("Error loading settings: " + e.getMessage() + "\nSetting default values");
            setDefaults();
        }
    }

    private void setDefaults() {
        this.registered = false;
        SettingsData defaults = new SettingsData();
        defaults.pathToAvatar = "";
        defaults.nickname = "default Nick Name";
        defaults.userIp = "127.0.0.1";
        defaults.userPort = "443";
        defaults.userKey = "";
        defaults.theme = "light";
        defaults.notificationsEnabled = true;
        defaults.microphone = "";
        defaults.speaker = "";
        defaults.microphoneVolume = 100;
        defaults.speakerVolume = 100;
        defaults.audioBitrate = 120;
        defaults.camera = "";
        defaults.cameraBitrate = 3800;
        defaults.signalingServerIp = "127.0.0.1";
        this.data = defaults;
        syncPropertiesWithData();
    }

    // === УСТРОЙСТВА (без изменений) ===

    public List<String> getAvailableMicrophones() {
        List<String> devices = new ArrayList<>();
        devices.add("Default");
        try {
            List<AudioDevice> microphones = WebRTCManager.getInstance().getMicrophones();
            for(AudioDevice microphone : microphones) {
                String name = microphone.getName();
                if (name != null && !name.isEmpty() &&
                        !name.contains("Primary") && !name.contains("Default") &&
                        !devices.contains(name)) {
                    devices.add(name);
                }
            }
        } catch (Exception e) {
            System.err.println("Error enumerating microphones: " + e.getMessage());
        }
        return devices;
    }

    public List<String> getAvailableSpeakers() {
        List<String> devices = new ArrayList<>();
        devices.add("Default");
        try {
            List<AudioDevice> speakers = WebRTCManager.getInstance().getSpeakers();
            for(AudioDevice speaker : speakers) {
                String name = speaker.getName();
                if (name != null && !name.isEmpty() &&
                        !name.contains("Primary") && !name.contains("Default") &&
                        !devices.contains(name)) {
                    devices.add(name);
                }
            }
        } catch (Exception e) {
            System.err.println("Error enumerating speakers: " + e.getMessage());
        }
        return devices;
    }

    public List<String> getAvailableCameras() {
        List<String> devices = new ArrayList<>();
        devices.add("Default");
        try {
            List<VideoDevice> cameras = WebRTCManager.getInstance().getCameras();
            for(VideoDevice camera : cameras) {
                String name = camera.getName();
                if (name != null && !name.isEmpty() &&
                        !name.contains("Primary") && !name.contains("Default") &&
                        !devices.contains(name)) {
                    devices.add(name);
                }
            }
        } catch (Exception e) {
            System.err.println("Camera enumeration not available: " + e.getMessage());
        }
        return devices;
    }

    // === AVATAR UPLOAD (без изменений) ===

    public boolean uploadAvatar(File avatarFile) {
        try {
            Path appAvatarsDir = Path.of(System.getProperty("user.home"), SETTINGS_FOLDER, AVATARS_FOLDER);
            Files.createDirectories(appAvatarsDir);
            Path targetPath = appAvatarsDir.resolve(avatarFile.getName());
            if (Files.exists(targetPath)) {
                targetPath = appAvatarsDir.resolve("avatar_" + System.currentTimeMillis() + "_" + avatarFile.getName());
            }
            Files.copy(avatarFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            setPathToAvatar(targetPath.toString());
            return true;
        } catch (Exception e) {
            System.err.println("ERROR WHILE UPLOADING AVATAR: " + e.getMessage());
            return false;
        }
    }

    // === VALIDATION (без изменений) ===

    public Optional<String> validateNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) return Optional.of("Никнейм не может быть пустым");
        String trimmed = nickname.trim();
        if (trimmed.length() < 3) return Optional.of("Минимальная длина: 3 символа");
        if (trimmed.length() > 20) return Optional.of("Максимальная длина: 20 символов");
        if (!trimmed.matches("^[a-zA-Z0-9_\\s\\u0400-\\u04FF]+$"))
            return Optional.of("Только буквы, цифры, пробелы и подчёркивания");
        return Optional.empty();
    }

    public Optional<String> validateIpAddress(String ip) {
        if (ip == null || ip.isBlank()) return Optional.of("IP-адрес не может быть пустым");
        if (!ip.matches("^\\d{1,3}(\\.\\d{1,3}){3}$")) return Optional.of("Неверный формат IP (xxx.xxx.xxx.xxx)");
        String[] parts = ip.split("\\.");
        for (String part : parts) {
            try {
                int val = Integer.parseInt(part);
                if (val < 0 || val > 255) return Optional.of("Каждая часть IP: 0–255");
            } catch (NumberFormatException e) {
                return Optional.of("Неверное число в IP-адресе");
            }
        }
        return Optional.empty();
    }

    public Optional<String> validatePort(String port) {
        if (port == null || port.isBlank()) return Optional.of("Порт не может быть пустым");
        try {
            int val = Integer.parseInt(port.trim());
            if (val < 1) return Optional.of("Минимальный порт: 1");
            if (val > 65535) return Optional.of("Максимальный порт: 65535");
            return Optional.empty();
        } catch (NumberFormatException e) {
            return Optional.of("Порт должен быть числом");
        }
    }

    public Optional<String> validateUserKey(String key) {
        if (key == null) return Optional.empty();
        if (key.length() > 128) return Optional.of("Максимальная длина ключа: 128 символов");
        return Optional.empty();
    }

    public Optional<String> validateAudioBitrate(int bitrate) {
        if (bitrate < 32) return Optional.of("Мин. битрейт аудио: 32 кбит/с");
        if (bitrate > 320) return Optional.of("Макс. битрейт аудио: 320 кбит/с");
        return Optional.empty();
    }

    public Optional<String> validateCameraBitrate(int bitrate) {
        if (bitrate < 100) return Optional.of("Мин. битрейт видео: 100 кбит/с");
        if (bitrate > 10000) return Optional.of("Макс. битрейт видео: 10000 кбит/с");
        return Optional.empty();
    }

    public boolean isNicknameValid(String nickname) { return validateNickname(nickname).isEmpty(); }
    public boolean isIpAddressValid(String ip) { return validateIpAddress(ip).isEmpty(); }
    public boolean isPortValid(String port) { return validatePort(port).isEmpty(); }
    public boolean isUserKeyValid(String key) { return validateUserKey(key).isEmpty(); }
    public boolean isAudioBitrateValid(int bitrate) { return validateAudioBitrate(bitrate).isEmpty(); }
    public boolean isCameraBitrateValid(int bitrate) { return validateCameraBitrate(bitrate).isEmpty(); }

    public Optional<String> getValidationError(String field, Object value) {
        return switch (field) {
            case "nickname" -> validateNickname((String) value);
            case "ip" -> validateIpAddress((String) value);
            case "port" -> validatePort((String) value);
            case "key" -> validateUserKey((String) value);
            case "audioBitrate" -> validateAudioBitrate((Integer) value);
            case "cameraBitrate" -> validateCameraBitrate((Integer) value);
            default -> Optional.empty();
        };
    }

    // === Вложенный класс данных (JsonProperty вместо SerializedName) ===

    private static class SettingsData {
        @JsonProperty("path_to_avatar") private String pathToAvatar;
        @JsonProperty("nickname") private String nickname;
        @JsonProperty("user_ip") private String userIp;
        @JsonProperty("user_port") private String userPort;
        @JsonProperty("user_key") private String userKey;
        @JsonProperty("theme") private String theme;
        @JsonProperty("notifications_enabled") private boolean notificationsEnabled;
        @JsonProperty("microphone") private String microphone;
        @JsonProperty("speaker") private String speaker;
        @JsonProperty("microphone_volume") private int microphoneVolume;
        @JsonProperty("speaker_volume") private int speakerVolume;
        @JsonProperty("audio_bitrate") private int audioBitrate;
        @JsonProperty("camera") private String camera;
        @JsonProperty("camera_bitrate") private int cameraBitrate;
        @JsonProperty("signaling_server_ip") private String signalingServerIp;
    }
}