package gtcloud.service.websocket.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import gtcloud.service.websocket.domain.PermissionInfo;
import gtcloud.service.websocket.domain.PermissionResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PermissionInfoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionInfoService.class);

    private final String AUTH2_SERVER_ADDRESS = "http://127.0.0.1:47900";

    private PermissionInfoService() {
    }

    public static PermissionInfoService getInstance() {
        return PermissionInfoServiceHolder.instance;
    }

    private static class PermissionInfoServiceHolder {
        private static final PermissionInfoService instance = new PermissionInfoService();
    }

    private List<PermissionInfo> getPermissionInfo(String userId, String token, String category) throws Exception {
        String permission = getPermissionByUserId(userId, token, category);
        ObjectMapper mapper = JsonParserService.getInstance();
        PermissionResponse permissionResponse = mapper.readValue(permission, PermissionResponse.class);
        int retcode = permissionResponse.getRetcode();
        if (retcode != 0) {
            throw new Exception("failed to get permission");
        }
        return permissionResponse.getRetdata();
    }

    private String getPermissionByUserId(String userId, String token, String category) throws IOException {
        String auth2ServerAddress = System.getProperty("auth2.server.address", AUTH2_SERVER_ADDRESS);
        OkHttpClient client = new OkHttpClient();
        String url = String.format("%s/auth2/v3/objperms/list/byuser?userid=%s&sessiontoken=%s&opcate=%s",
                auth2ServerAddress, userId, token, category);
        LOGGER.info("auth2 server url: {}",url);
        Request request = new Request.Builder()
            .url(url)
            .build();

        try (Response response = client.newCall(request).execute()) {
            return response.body().string();
        }
    }

    public Set<String> getTargets(String userId, String token, String category) throws Exception {
        Set<String> result = new HashSet<>();
        List<PermissionInfo> permissionInfoList = getPermissionInfo(userId, token, category);
        for (PermissionInfo permissionInfo : permissionInfoList) {
            String objectIdStr = permissionInfo.getObjProps().get("objectIDs");
            LOGGER.info("opCategory: {}, objectIDs: {}", category, objectIdStr);
            if (objectIdStr != null) {
                String[] objectIdArray = objectIdStr.split(","); // 215515,215514,120487,00742756
                List<String> objectIds = Arrays.asList(objectIdArray);
                result.addAll(objectIds);
            }
        }
        return result;
    }
}
