package com.mycompany.javaphone_nir2.models;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import javafx.beans.property.*;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Line;
import com.github.sarxos.webcam.Webcam;
import java.util.ArrayList;
import java.util.List;

public class SettingsManager {

    private static SettingsManager instance;

    private static final String SETTINGS_FOLDER = ".javaphone";
    private static final String CONFIG_FILE = "app_settings.json";
    private static final String AVATARS_FOLDER = "avatars";

    private final Path settingsPath;
    private final Gson gson;

    private boolean registered;
    private SettingsData data;

    private javax.sound.sampled.Line activeSpeakerLine; //активный динамик, который надо регать и к нему будут применяться настройки громкости
    private javax.sound.sampled.Line activeMicLine; //активный микрофон, который надо регать и к немму будут применяться настройки громкости

    // === REACTIVE PROPERTIES (transient для Gson) ===
    // Синхронизируются с полями data через listeners

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
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.settingsPath = Paths.get(System.getProperty("user.home"), SETTINGS_FOLDER, CONFIG_FILE);
        this.data = new SettingsData();

        // === РЕГИСТРАЦИЯ СЛУШАТЕЛЕЙ: свойство → data → save() ===
        nicknameProperty.addListener((obs, old, newVal) -> { data.nickname = newVal; });
        userIpProperty.addListener((obs, old, newVal) -> { data.userIp = newVal; });
        userPortProperty.addListener((obs, old, newVal) -> { data.userPort = newVal; });
        userKeyProperty.addListener((obs, old, newVal) -> { data.userKey = newVal; });
        themeProperty.addListener((obs, old, newVal) -> { data.theme = newVal; });
        pathToAvatarProperty.addListener((obs, old, newVal) -> { data.pathToAvatar = newVal; });
        notificationsEnabledProperty.addListener((obs, old, newVal) -> { data.notificationsEnabled = newVal; });
        microphoneProperty.addListener((obs, old, newVal) -> { data.microphone = newVal; });
        speakerProperty.addListener((obs, old, newVal) -> { data.speaker = newVal; });


//        microphoneVolumeProperty.addListener((obs, old, newVal) -> { data.microphoneVolume = (int) newVal; });
//        speakerVolumeProperty.addListener((obs, old, newVal) -> { data.speakerVolume = (int) newVal; });

        speakerVolumeProperty.addListener((obs, oldVal, newVal) -> {
            data.speakerVolume = (int) newVal;
//            save();
            applyGainToLine(activeSpeakerLine, (int) newVal, javax.sound.sampled.FloatControl.Type.MASTER_GAIN); // 🔥
        });

        microphoneVolumeProperty.addListener((obs, oldVal, newVal) -> {
            data.microphoneVolume = (int) newVal;
//            save();
            applyGainToLine(activeMicLine, (int) newVal, javax.sound.sampled.FloatControl.Type.MASTER_GAIN); // 🔥
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

    // === ЗАГРУЗКА: data → Properties ===
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

    // === PUBLIC API: Getters/Setters (для обратной совместимости) ===
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

    // === ЗАГРУЗКА/СОХРАНЕНИЕ ===
    public void save() {
        try {
            Files.createDirectories(settingsPath.getParent());
            try (FileWriter writer = new FileWriter(settingsPath.toFile())) {
                gson.toJson(data, writer); // Сериализуем только data (Properties — transient)
            }
            System.out.println("Settings saved: " + settingsPath);
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

        try (FileReader reader = new FileReader(settingsPath.toFile())) {
            SettingsData loaded = gson.fromJson(reader, SettingsData.class);
            if (loaded != null) {
                this.data = loaded;
                this.registered = true;
                syncPropertiesWithData(); // 🔥 Синхронизируем Properties с загруженными данными
            } else {
                setDefaults();
            }
        } catch (Exception e) {
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
        syncPropertiesWithData(); // 🔥 Синхронизируем дефолты
        // save(); // Раскомментируй, если хочешь сразу создать файл с дефолтами
    }

    /**
 * Регистрирует активную линию воспроизведения.
 * Вызывается из AudioService при создании SourceDataLine.
 */
    public void registerActiveSpeakerLine(javax.sound.sampled.Line line) {
        this.activeSpeakerLine = line;
        // Сразу применяем сохранённую настройку
        applyGainToLine(line, data.speakerVolume, javax.sound.sampled.FloatControl.Type.MASTER_GAIN);
    }

    //как то так он делает всё в javadoc..
    /**
     // Где-то в AudioManager.java или аналогичном сервисе
    SourceDataLine speaker = AudioSystem.getSourceDataLine(format);
    speaker.open(format);

    // 🔥 Регистрируем линию в SettingsManager.
    // Теперь менеджер сам будет менять её громкость при движении слайдера.
    SettingsManager.getInstance().registerActiveSpeakerLine(speaker);

    speaker.start();

    // Аналогично для микрофона:
    TargetDataLine mic = AudioSystem.getTargetDataLine(format);
    mic.open(format);
    SettingsManager.getInstance().registerActiveMicLine(mic);
    mic.start();
    */

    /**
     * Регистрирует активную линию захвата.
     * Вызывается из AudioService при создании TargetDataLine.
     */
    public void registerActiveMicLine(javax.sound.sampled.Line line) {
        this.activeMicLine = line;
        applyGainToLine(line, data.microphoneVolume, javax.sound.sampled.FloatControl.Type.MASTER_GAIN);
    }

    public List<String> getAvailableMicrophones() {
        List<String> devices = new ArrayList<>();
        devices.add("Default"); // Опция по умолчанию

        try {
            Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
            for (Mixer.Info mixerInfo : mixerInfos) {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                // Микрофон — это TargetLine (линия, в которую данные входят)
                Line.Info[] targetLines = mixer.getTargetLineInfo();
                if (targetLines.length > 0) {
                    String name = mixerInfo.getName();
                    // Фильтрация системных дублей и пустых имен
                    if (name != null && !name.isEmpty() &&
                        !name.contains("Primary") && !name.contains("Default") &&
                        !devices.contains(name)) {
                        devices.add(name);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error enumerating microphones: " + e.getMessage());
        }
        return devices;
    }

    /**
     * Возвращает список доступных динамиков (устройств воспроизведения).
     * Использует стандартный javax.sound.sampled (Java SE).
     */
    public List<String> getAvailableSpeakers() {
        List<String> devices = new ArrayList<>();
        devices.add("Default");

        try {
            Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
            for (Mixer.Info mixerInfo : mixerInfos) {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                // Динамик — это SourceLine (линия, из которой данные выходят)
                Line.Info[] sourceLines = mixer.getSourceLineInfo();
                if (sourceLines.length > 0) {
                    String name = mixerInfo.getName();
                    if (name != null && !name.isEmpty() &&
                        !name.contains("Primary") && !name.contains("Default") &&
                        !devices.contains(name)) {
                        devices.add(name);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error enumerating speakers: " + e.getMessage());
        }
        return devices;
    }

    /**
     * Возвращает список доступных веб-камер.
     * Примечание: В стандартной Java SE нет API для перечисления камер.
     * Используется библиотека webcam-capture как де-факто стандарт.
     */
    public List<String> getAvailableCameras() {
        List<String> cameras = new ArrayList<>();
        cameras.add("Default");

        try {
            // Webcam.getWebcams() выполняет нативный вызов к ОС
            List<Webcam> webcams = Webcam.getWebcams();
            for (Webcam webcam : webcams) {
                String name = webcam.getName();
                if (name != null && !cameras.contains(name)) {
                    cameras.add(name);
                }
            }
        } catch (Exception e) {
            // Если библиотека не подключена или нет прав, просто вернем "Default"
            System.err.println("Camera enumeration not available (no library or permissions): " + e.getMessage());
        }
        return cameras;
    }

    /**
     * Применяет процент громкости (0-100) к аудио-линии.
     * Безопасно обрабатывает отсутствие поддержки контроллера.
     */
    private void applyGainToLine(javax.sound.sampled.Line line, int percent, javax.sound.sampled.FloatControl.Type type) {
        if (line == null || !line.isOpen()) return;

        // Проверяем поддержку контроллера громкости
        if (!line.isControlSupported(type)) {
            // Fallback: многие устройства не поддерживают MASTER_GAIN, но поддерживают VOLUME
            type = javax.sound.sampled.FloatControl.Type.VOLUME;
            if (!line.isControlSupported(type)) return;
        }

        javax.sound.sampled.FloatControl control = (javax.sound.sampled.FloatControl) line.getControl(type);
        float min = control.getMinimum();
        float max = control.getMaximum();

        // Линейная интерполяция: 0% → min, 100% → max
        control.setValue(min + (max - min) * (percent / 100.0f));
    }


    // === AVATAR UPLOAD (без изменений, работает с Properties) ===
    public boolean uploadAvatar(File avatarFile) {
        try {
            Path appAvatarsDir = Path.of(System.getProperty("user.home"), SETTINGS_FOLDER, AVATARS_FOLDER);
            Files.createDirectories(appAvatarsDir);
            Path targetPath = appAvatarsDir.resolve(avatarFile.getName());
            if (Files.exists(targetPath)) {
                targetPath = appAvatarsDir.resolve("avatar_" + System.currentTimeMillis() + "_" + avatarFile.getName());
            }
            Files.copy(avatarFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            setPathToAvatar(targetPath.toString()); // 🔥 Автоматически вызовет save() через listener
            return true;
        } catch (Exception e) {
            System.err.println("ERROR WHILE UPLOADING AVATAR: " + e.getMessage());
            return false;
        }
    }

    // === Вложенный класс данных (без изменений) ===
    private static class SettingsData {
        @SerializedName("path_to_avatar") private String pathToAvatar;
        @SerializedName("nickname") private String nickname;
        @SerializedName("user_ip") private String userIp;
        @SerializedName("user_port") private String userPort;
        @SerializedName("user_key") private String userKey;
        @SerializedName("theme") private String theme;
        @SerializedName("notifications_enabled") private boolean notificationsEnabled;
        @SerializedName("microphone") private String microphone;
        @SerializedName("speaker") private String speaker;
        @SerializedName("microphone_volume") private int microphoneVolume;
        @SerializedName("speaker_volume") private int speakerVolume;
        @SerializedName("audio_bitrate") private int audioBitrate;
        @SerializedName("camera") private String camera;
        @SerializedName("camera_bitrate") private int cameraBitrate;
        @SerializedName("signaling_server_ip") private String signalingServerIp;
    }
}