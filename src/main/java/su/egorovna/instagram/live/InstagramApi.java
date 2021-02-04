package su.egorovna.instagram.live;

import com.github.instagram4j.instagram4j.IGClient;
import com.github.instagram4j.instagram4j.exceptions.IGLoginException;
import com.github.instagram4j.instagram4j.models.media.timeline.Comment;
import com.github.instagram4j.instagram4j.models.user.Profile;
import com.github.instagram4j.instagram4j.requests.live.*;
import com.github.instagram4j.instagram4j.responses.IGResponse;
import com.github.instagram4j.instagram4j.responses.live.LiveBroadcastGetCommentResponse;
import com.github.instagram4j.instagram4j.responses.live.LiveBroadcastGetViewerListResponse;
import com.github.instagram4j.instagram4j.responses.live.LiveCreateResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import su.egorovna.instagram.live.requests.LiveBroadcastMuteCommentRequest;
import su.egorovna.instagram.live.requests.LiveBroadcastUnmuteCommentRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InstagramApi {

    private static final Logger LOG = LoggerFactory.getLogger(InstagramApi.class);
    private static final String BROADCAST_TYPE_RTMP = "RTMP";

    private static IGClient client;
    private static String broadcastUrl;
    private static String broadcastKey;
    private static String broadcastId;

    public InstagramApi() throws Exception {
        throw new Exception("Static only!");
    }

    public static IGClient getClient() {
        return client;
    }

    public static String getBroadcastUrl() {
        return broadcastUrl;
    }

    public static String getBroadcastKey() {
        return broadcastKey;
    }

    public static String getBroadcastId() {
        return broadcastId;
    }

    public static void login(String username, String password) throws IGLoginException {
        LOG.debug("Авторизация: username={}, password={}", username, password);
        client = IGClient.builder().username(username).password(password).login();
    }

    public static void logout() {
        LOG.debug("Отмена авторизации: username={}", client.getSelfProfile().getUsername());
        client = null;
        broadcastUrl = null;
        broadcastKey = null;
        broadcastId = null;
    }

    /**
     * Создание трансляции
     *
     * @param quality качество трансляции
     */
    public static void createBroadcast(Quality quality) throws Exception {
        createBroadcast(quality, client.getSelfProfile().getUsername() + " broadcast");
    }

    /**
     * Создание трансляции
     *
     * @param quality качество трансляции
     * @param message название трансляции
     * @throws Exception ошибка создания
     */
    public static void createBroadcast(Quality quality, String message) throws Exception {
        LOG.debug("Создание трансляции: width={}, height={}, message={}, type={}", quality.getWidth(), quality.getHeight(), message, BROADCAST_TYPE_RTMP);
        try {
            LiveCreateRequest createRequest = new LiveCreateRequest(quality.getWidth(), quality.getHeight(), message, BROADCAST_TYPE_RTMP);
            LiveCreateResponse createResponse = client.sendRequest(createRequest).join();
            broadcastUrl = createResponse.getBroadcastUrl();
            broadcastKey = createResponse.getBroadcastKey();
            broadcastId = createResponse.getBroadcast_id();
        } catch (Exception e) {
            LOG.error("Ошибка создания трансляции", e);
            broadcastUrl = null;
            broadcastKey = null;
            broadcastId = null;
            throw new Exception(e);
        }
    }

    /**
     * Старт трансляции
     *
     * @param notify отправка уведомления
     * @throws Exception ошибка старта
     */
    public static void startBroadcast(boolean notify) throws Exception {
        LOG.debug("Старт трансляции: notify={}", notify);
        if (broadcastId != null) {
            try {
                LiveStartRequest startRequest = new LiveStartRequest(broadcastId, notify);
                client.sendRequest(startRequest);
            } catch (Exception e) {
                LOG.error("Ошибка старта трансляции", e);
                throw new Exception(e);
            }
        }
    }

    /**
     * Остановка трансляции
     */
    public static void stopBroadcast() throws Exception {
        if (client == null) return;
        LOG.debug("Остановка трансляции");
        try {
            LiveEndBroadcastRequest endBroadcastRequest = new LiveEndBroadcastRequest(broadcastId);
            client.sendRequest(endBroadcastRequest);
        } catch (Exception e) {
            LOG.error("Ошибка остановки трансляции", e);
            throw new Exception(e);
        }
    }

    /**
     * Запрос комментариев трансляции
     *
     * @param lastTs индекс последнего запрошенного комментария
     * @return список коментариев
     * @throws Exception ошибка запроса
     */
    public static List<Comment> getComments(Long lastTs) throws Exception {
        List<Comment> comments;
        LOG.debug("Запрос комментариев трансляции: id={}, last_ts={}", broadcastId, lastTs);
        try {
            LiveBroadcastGetCommentRequest commentRequest = new LiveBroadcastGetCommentRequest(broadcastId, lastTs);
            LiveBroadcastGetCommentResponse commentResponse = client.sendRequest(commentRequest).join();
            comments = new ArrayList<>(commentResponse.getComments());
        } catch (Exception e) {
            LOG.error("Ошибка запроса комментариев трансляции", e);
            throw new Exception(e);
        }
        return comments;
    }

    /**
     * Запрос списка зрителей
     *
     * @return список зрителей
     * @throws Exception ошибка запроса
     */
    public static List<Profile> getViewers() throws Exception {
        List<Profile> viewers;
        LOG.debug("Запрос списка зрителей: id={}", broadcastId);
        try {
            LiveBroadcastGetViewerListRequest viewerListRequest = new LiveBroadcastGetViewerListRequest(broadcastId);
            LiveBroadcastGetViewerListResponse viewerListResponse = client.sendRequest(viewerListRequest).join();
            viewers = new ArrayList<>(viewerListResponse.getUsers());
        } catch (Exception e) {
            LOG.error("Ошибка запроса списка зрителей", e);
            throw new Exception(e);
        }
        return viewers;
    }

    /**
     * Запрос ссылки на плеер
     *
     * @return ссылка
     */
    public static String dashPlaybackUrl() {
        LOG.debug("Запрос ссылки на плеер");
        String url = null;
        try {
            LiveBroadcastInfoRequest infoRequest = new LiveBroadcastInfoRequest(broadcastId);
            IGResponse response = client.sendRequest(infoRequest).join();
            url = (String) response.getExtraProperties().get("dash_playback_url");
            url = removeUTFCharacters(url);
        } catch (Exception e) {
            LOG.error("Ошибка запроса ссылки на плеер", e);
        }
        return url;
    }

    public static void muteComment() {
        LOG.debug("Запрет комментирования трансляции");
        try {
            LiveBroadcastMuteCommentRequest muteCommentRequest = new LiveBroadcastMuteCommentRequest(broadcastId);
            client.sendRequest(muteCommentRequest).join();
        } catch (Exception e) {
            LOG.error("Ошибка запрета комментирования трансляции", e);
        }
    }

    public static void unmuteComment() {
        LOG.debug("Разрешение комментирования трансляции");
        try {
            LiveBroadcastUnmuteCommentRequest unmuteCommentRequest = new LiveBroadcastUnmuteCommentRequest(broadcastId);
            client.sendRequest(unmuteCommentRequest).join();
        } catch (Exception e) {
            LOG.error("Ошибка разрешения комментирования трансляции", e);
        }
    }

    /**
     * Корректировка строки (Инстраграм отдаёт ссылку с юникод символами)
     *
     * @param data строка с юникод символами
     * @return строка ASCII
     */
    private static String removeUTFCharacters(String data) {
        if (data == null) return null;
        Pattern p = Pattern.compile("\\\\u(\\p{XDigit}{4})");
        Matcher m = p.matcher(data);
        StringBuffer buf = new StringBuffer(data.length());
        while (m.find()) {
            String ch = String.valueOf((char) Integer.parseInt(m.group(1), 16));
            m.appendReplacement(buf, Matcher.quoteReplacement(ch));
        }
        m.appendTail(buf);
        return buf.toString();
    }

    /**
     * Качество транляции
     */
    public enum Quality {
        HD("HD", 720, 1280),
        FHD("FHD", 1080, 1920);

        private final String title;
        private final int width;
        private final int height;

        /**
         * Конструктор
         *
         * @param title  название
         * @param width  ширина
         * @param height высота
         */
        Quality(String title, int width, int height) {
            this.title = title;
            this.width = width;
            this.height = height;
        }

        public String getTitle() {
            return title;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }
    }

}
