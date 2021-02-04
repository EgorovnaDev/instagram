package su.egorovna.instagram.live.requests;

import com.github.instagram4j.instagram4j.IGClient;
import com.github.instagram4j.instagram4j.models.IGBaseModel;
import com.github.instagram4j.instagram4j.models.IGPayload;
import com.github.instagram4j.instagram4j.requests.IGPostRequest;
import com.github.instagram4j.instagram4j.responses.IGResponse;

public class LiveBroadcastMuteCommentRequest extends IGPostRequest<IGResponse> {

    private String broadcast_id;

    public LiveBroadcastMuteCommentRequest(String broadcast_id) {
        this.broadcast_id = broadcast_id;
    }

    public String getBroadcast_id() {
        return broadcast_id;
    }

    public void setBroadcast_id(String broadcast_id) {
        this.broadcast_id = broadcast_id;
    }

    @Override
    protected IGBaseModel getPayload(IGClient client) {
        return new IGPayload();
    }

    @Override
    public String path() {
        return "live/"+broadcast_id+"/mute_comment/";
    }

    @Override
    public Class<IGResponse> getResponseType() {
        return IGResponse.class;
    }
}
