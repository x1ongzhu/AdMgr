package cn.x1ongzhu.admgr;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import fi.iki.elonen.NanoHTTPD;
import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.RealmResults;

public class WebServer extends NanoHTTPD {

    private final static String MIME_JSON = "application/json";
    private final static String MIME_JS = "text/javascript";
    private final static String MIME_CSS = "text/css";

    private File directory;
    private Context context;

    public WebServer(int port, Context context) {
        super(port);
        directory = Environment.getExternalStoragePublicDirectory("ads");
        this.context = context;
    }

    public WebServer(String hostname, int port, Context context) {
        super(hostname, port);
        directory = Environment.getExternalStoragePublicDirectory("ads");
        this.context = context;
    }

    @Override
    public Response serve(IHTTPSession session) {
        String authString = session.getHeaders().get("authorization");
        if (TextUtils.isEmpty(authString)) {
            Response response = newFixedLengthResponse(Response.Status.UNAUTHORIZED, MIME_HTML, "UNAUTHORIZED");
            response.addHeader("WWW-Authenticate", "Basic realm='.'");
            return response;
        }
        if (session.getUri().equals("/")) {
            try {
                InputStream inputStream = context.getResources().getAssets().open("www/index.html");
                return newChunkedResponse(Response.Status.OK, MIME_HTML, inputStream);
            } catch (IOException e) {
                e.printStackTrace();
                return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "未找到");
            }
        } else if (session.getUri().startsWith("/css/") || session.getUri().startsWith("/js/")) {
            String fileName = "www" + session.getUri();
            String mimeType = session.getUri().startsWith("/css/") ? MIME_CSS : MIME_JS;
            try {
                InputStream inputStream = context.getResources().getAssets().open(fileName);
                return newChunkedResponse(Response.Status.OK, mimeType, inputStream);
            } catch (IOException e) {
                e.printStackTrace();
                return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "未找到");
            }
        } else if (session.getUri().equals("/fileList")) {
            return getFileList(session);
        } else if (session.getUri().equals("/upload")) {
            return uploadFile(session);
        } else if (session.getUri().equals("/del")) {
            return deleteFile(session);
        }
        return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "错误的请求");
    }

    private Response getFileList(IHTTPSession session) {
        try {
            if (!directory.exists()) {
                directory.mkdir();
            }
            Realm realm = Realm.getDefaultInstance();
            RealmResults<Adv> advList = realm.where(Adv.class).findAll().sort("createTime");
            Result result = new Result(true, realm.copyFromRealm(advList));
            return newFixedLengthResponse(Response.Status.OK, MIME_JSON, result.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return newFixedLengthResponse(Response.Status.OK, MIME_JSON, new Result(false, null).toString());
        }
    }

    private Response uploadFile(IHTTPSession session) {
        try {
            if (!directory.exists()) {
                directory.mkdir();
            }
            ContentType ct = new ContentType(session.getHeaders().get("content-type")).tryUTF8();
            session.getHeaders().put("content-type", ct.getContentTypeHeader());

            Map<String, String> tmpFiles = new HashMap<>();
            session.parseBody(tmpFiles);

            String tmpFilePath = tmpFiles.get("file");
            String fileName = session.getParms().get("file");
            fileName = UUID.randomUUID().toString() + fileName.replaceAll(".*\\.", ".");
            String name = session.getParms().get("name");
            String startTime = session.getParms().get("startTime");
            String endTime = session.getParms().get("endTime");
            Integer duration = Integer.valueOf(session.getParms().get("duration"));

            File tmpFile = new File(tmpFilePath);
            File targetFile = new File(directory, fileName);
            copyFile(tmpFile, targetFile);

            Realm realm = Realm.getDefaultInstance();
            realm.beginTransaction();
            Adv adv = new Adv();
            adv.setFileName(fileName);
            adv.setName(name);
            adv.setDuration(duration);
            if (!TextUtils.isEmpty(startTime)) {
                adv.setStartTime(new Date(Long.valueOf(startTime)));
            }
            if (!TextUtils.isEmpty(endTime)) {
                adv.setEndTime(new Date(Long.valueOf(endTime)));
            }
            realm.insert(adv);
            realm.commitTransaction();
            Result result = new Result(true, null);
            return newFixedLengthResponse(Response.Status.OK, MIME_JSON, result.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return newFixedLengthResponse(Response.Status.OK, MIME_JSON, new Result(false, null).toString());
        }
    }

    private Response deleteFile(IHTTPSession session) {
        try {
            if (!directory.exists()) {
                directory.mkdir();
            }
            session.parseBody(new HashMap<>());
            String id = session.getParms().get("id");
            if (!TextUtils.isEmpty(id)) {
                Realm realm = Realm.getDefaultInstance();
                RealmResults<Adv> advList = realm.where(Adv.class).equalTo("id", id).findAll();
                for (Adv adv : advList) {
                    File file = new File(directory, adv.getFileName());
                    if (file.exists()) {
                        file.delete();
                    }
                }
                realm.beginTransaction();
                advList.deleteAllFromRealm();
                realm.commitTransaction();
            }
            Result result = new Result(true, null);
            return newFixedLengthResponse(Response.Status.OK, MIME_JSON, result.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return newFixedLengthResponse(Response.Status.OK, MIME_JSON, new Result(false, null).toString());
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

        public Result(boolean success, Object data) {
            this.success = success;
            this.data = data;
        }

        @Override
        public String toString() {
            StringBuilder stringBuilder = new StringBuilder("{\"success\":" + this.success);

            if (this.data != null) {
                if (this.success) {

                    Gson gson = new GsonBuilder()
                            .registerTypeAdapter(Date.class, (JsonDeserializer<Date>) (json, typeOfT, context) -> new Date(json.getAsJsonPrimitive().getAsLong()))
                            .registerTypeAdapter(Date.class, (JsonSerializer<Date>) (date, type, jsonSerializationContext) -> new JsonPrimitive(date.getTime()))
                            .create();
                    String data = gson.toJson(this.data);
                    stringBuilder.append(",\"data\":").append(data);
                } else {
                    stringBuilder.append(",\"error\"").append(data.toString());
                }
            }
            stringBuilder.append("}");
            return stringBuilder.toString();
        }
    }

    public String encodeStr(String str) {

        if (str == null) {
            return null;
        }
        try {
            return new String(str.getBytes("ISO-8859-1"), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }
}
