package org.up.cd.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class Config {

    private static final Logger logger = LogManager.getLogger(Config.class);

    private String cellId;
    private String targetNodeHost;
    private int    targetNodePort;
    private int    minAcks;

    private Config() {}

    public static Config getInstance() {
        return Holder.INSTANCE;
    }

    private static class Holder {
        private static final Config INSTANCE = new Config();
    }

    public boolean load(String path) {
        try {
            InputStream is;
            if (path != null) {
                File f = new File(path);
                if (!f.exists()) { logger.error("[Config] File not found: {}", path); return false; }
                is = new FileInputStream(f);
            } else {
                File cwd = new File("config.json");
                if (cwd.exists()) {
                    is = new FileInputStream(cwd);
                } else {
                    logger.error("[Config] config.json not found in CWD");
                    return false;
                }
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(is);
            is.close();

            cellId         = root.get("cellId").asText();
            targetNodeHost = root.get("targetNodeHost").asText();
            targetNodePort = root.get("targetNodePort").asInt();
            minAcks        = root.has("minAcks") ? root.get("minAcks").asInt() : 2;

            logger.info("[Config] Loaded: cellId={}, node={}:{}, minAcks={}",
                    cellId, targetNodeHost, targetNodePort, minAcks);
            return true;

        } catch (Exception e) {
            logger.error("[Config] Load error: {}", e.getMessage());
            return false;
        }
    }

    public String getCellId()         { return cellId; }
    public String getTargetNodeHost() { return targetNodeHost; }
    public int    getTargetNodePort() { return targetNodePort; }
    public int    getMinAcks()        { return minAcks; }
}
