package cn.x1ongzhu.admgr;

import android.content.Context;
import android.text.TextUtils;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class WebServer extends NanoHTTPD {
    private Context mContext;

    public WebServer(int port, Context context) {
        super(port);
        this.mContext = context;
    }

    public WebServer(String hostname, int port, Context context) {
        super(hostname, port);
        this.mContext = context;
    }

    @Override
    public Response serve(IHTTPSession session) {
        if (session.getUri().equals("/")) {
            try {
                InputStream inputStream = mContext.getResources().getAssets().open("www/index.html");
                return newChunkedResponse(Response.Status.OK, MIME_HTML, inputStream);
            } catch (IOException e) {
                e.printStackTrace();
                return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "未找到");
            }
        } else if (session.getUri().startsWith("/css/") || session.getUri().startsWith("/js/")) {
            String fileName = "www" + session.getUri();
            String mimeType = session.getUri().startsWith("/css/") ? "text/css" : "text/javascript";
            try {
                InputStream inputStream = mContext.getResources().getAssets().open(fileName);
                return newChunkedResponse(Response.Status.OK, mimeType, inputStream);
            } catch (IOException e) {
                e.printStackTrace();
                return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "未找到");
            }
        } else if (session.getUri().equals("/fileList")) {
            return getFileList(session);
        } else if (session.getUri().equals("/upload")) {
            return uploadFile(session);
        }
        return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "错误的请求");
    }

    private Response getFileList(IHTTPSession session) {
        String mimeType = "application/json";
        try {
            File directory = mContext.getExternalFilesDir("ads");
            if (!directory.exists()) {
                directory.mkdir();
            }
            String[] fileList = directory.list();
            Result result = new Result(true, fileList);
            return newFixedLengthResponse(Response.Status.OK, mimeType, result.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return newFixedLengthResponse(Response.Status.OK, mimeType, new Result(false, null).toString());
        }
    }

    private Response uploadFile(IHTTPSession session) {
        String mimeType = "application/json";
        try {
            File directory = mContext.getExternalFilesDir("ads");
            if (!directory.exists()) {
                directory.mkdir();
            }

            Map<String, String> files = new HashMap<>();
            session.parseBody(files);

            Map<String, String> params = session.getParms();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                final String paramsKey = entry.getKey();
                final String tmpFilePath = files.get(paramsKey);
                final String fileName = paramsKey;
                final File tmpFile = new File(tmpFilePath);
                final File targetFile = new File(directory, fileName);
                copyFile(tmpFile, targetFile);
            }
            Result result = new Result(true, null);
            return newFixedLengthResponse(Response.Status.OK, mimeType, result.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return newFixedLengthResponse(Response.Status.OK, mimeType, new Result(false, null).toString());
        }
    }

    public void copyFile(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        try {
            OutputStream out = new FileOutputStream(dst);
            try {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }

    private class Result {
        private boolean success;
        private Object data;
        private String error;

        public Result(boolean success, Object data) {
            this.success = success;
            this.data = data;
        }

        @Override
        public String toString() {
            StringBuilder stringBuilder = new StringBuilder("{\"success\":" + this.success);

            if (this.data != null) {
                if (this.success) {
                    Gson gson = new Gson();
                    String data = gson.toJson(this.data);
                    stringBuilder.append("\"data\":").append(data);
                } else {
                    stringBuilder.append("\"error\"").append(data.toString());
                }
            }
            stringBuilder.append("}");
            return stringBuilder.toString();
        }
    }
}
