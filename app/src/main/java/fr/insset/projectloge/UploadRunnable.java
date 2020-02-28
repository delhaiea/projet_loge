package fr.insset.projectloge;

import android.net.Uri;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

public class UploadRunnable implements Runnable {

    private static final String LINE_END = "\r\n";
    private static final String TWO_HYPHENS = "--";
    private static final String SERVER_URL = "http://download.alexisdelhaie.ovh:3010/photo";
    private String fileUri;

    public UploadRunnable(String path) {
        fileUri = path;
    }

    private String generateBoundary() {
        UUID uuid = UUID.randomUUID();
        return String.format("NextPart_%s", uuid.toString());
    }


    @Override
    public void run() {
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1024 * 1024;
        HttpURLConnection conn = null;
        DataOutputStream dos = null;
        FileInputStream fileInputStream = null;
        if(!fileUri.isEmpty()) {
            try {
                String boundary = generateBoundary();
                File source = new File(fileUri);
                fileInputStream = new FileInputStream(source);
                URL url = new URL(SERVER_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true); // Allow Inputs
                conn.setDoOutput(true); // Allow Outputs
                conn.setUseCaches(false); // Don't use a Cached Copy
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                conn.setRequestProperty("uploaded_file", source.getName());
                dos = new DataOutputStream(conn.getOutputStream());
                dos.writeBytes(TWO_HYPHENS + boundary + LINE_END);
                dos.writeBytes("Content-Disposition: form-data; name=\"photo\";filename=\""
                        + source.getName() + "\"" + LINE_END);
                dos.writeBytes(LINE_END);
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                while (bytesRead > 0) {
                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                }
                dos.writeBytes(LINE_END);
                dos.writeBytes(TWO_HYPHENS + boundary + TWO_HYPHENS + LINE_END);
                //serverResponseCode = conn.getResponseCode();
                String serverResponseMessage = conn.getResponseMessage();
            } catch (Exception e) {
                Log.e("Photo Upload Thread", "run: Erreur", e);
            } finally {
                try {
                    if(fileInputStream != null)
                        fileInputStream.close();
                    if(dos != null) {
                        dos.flush();
                        dos.close();
                    }
                } catch (IOException e) {
                    Log.e("Photo Upload Thread", "run: Erreur", e);
                }
            }
        }
    }
}
