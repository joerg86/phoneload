/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
*/
package biz.sawatzki.phoneload;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Iterator;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;
import android.util.Log;
import android.webkit.CookieManager;

import com.phonegap.api.Plugin;
import com.phonegap.api.PluginResult;
import java.util.HashMap;

import com.phonegap.FileUtils;

public class PhoneLoad extends Plugin {

    private static final String LOG_TAG = "PhoneLoad";
    private static final String LINE_START = "--";
    private static final String LINE_END = "\r\n";
    private static final String BOUNDRY =  "*****";

    public static int FILE_NOT_FOUND_ERR = 1;
    public static int INVALID_URL_ERR = 2;
    public static int CONNECTION_ERR = 3;

    private SSLSocketFactory defaultSSLSocketFactory = null;
    private HostnameVerifier defaultHostnameVerifier = null;
    
    private HashMap<String,FileDownloadThread> downloadWorkers;

    public PhoneLoad() {
    	super();
    	downloadWorkers = new HashMap<String,FileDownloadThread>();
    }
    
    /* (non-Javadoc)
    * @see com.phonegap.api.Plugin#execute(java.lang.String, org.json.JSONArray, java.lang.String)
    */
    @Override
    public PluginResult execute(String action, JSONArray args, String callbackId) {
        String source = null;
        String target = null;
        String id = null;
        if(action.equals("download") || action.equals("upload"))
        {
        	try {
        		source = args.getString(0);
        		target = args.getString(1);
        		if(args.length() > 2)
        			id = args.getString(2);
        	}
        	catch (JSONException e) {
            	Log.d(LOG_TAG, "Missing source or target");
            	return new PluginResult(PluginResult.Status.JSON_EXCEPTION, "Missing source or target");
        	}
        }
        else if(action.equals("cancel_download"))
        {
        	try {
        		id = args.getString(0);
        	}
        	catch (JSONException e) {
        		Log.d(LOG_TAG, "Missing transfer ID.");
        		return new PluginResult(PluginResult.Status.JSON_EXCEPTION, "Missing transfer ID.");
        	}
        }

        try {
            if (action.equals("upload")) {
                // Setup the options
                String fileKey = null;
                String fileName = null;
                String mimeType = null;

                fileKey = getArgument(args, 2, "file");
                fileName = getArgument(args, 3, "image.jpg");
                mimeType = getArgument(args, 4, "image/jpeg");
                JSONObject params = args.optJSONObject(5);
                boolean trustEveryone = args.optBoolean(6);
                boolean chunkedMode = args.optBoolean(7);
                FileUploadResult r = upload(source, target, fileKey, fileName, mimeType, params, trustEveryone, chunkedMode);
                Log.d(LOG_TAG, "****** About to return a result from upload");
                return new PluginResult(PluginResult.Status.OK, r.toJSONObject());
            } else if (action.equals("download")) {
                //FileDownloadResult r = download(source, target, callbackId);
            	Log.d(LOG_TAG, "***** Starting download.");
            	FileDownloadThread t = new FileDownloadThread(this, source, target, callbackId);
            	downloadWorkers.put(id, t);
            	t.start();
            	PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
            	result.setKeepCallback(true);
            	return result;
            } else if (action.equals("cancel_download")) {
            	if(id != null)
            	{
            		FileDownloadThread t = downloadWorkers.get(id);
            		if (t != null) {
            			t.requestStop();
            			try { 
            				t.join();
            			}
            			catch (InterruptedException e)
            			{
            				// ignore
            			}
            		}
            	
            		Log.d(LOG_TAG, "****** Download canceled.");
            	}
            	return new PluginResult(PluginResult.Status.OK);
      
            } else {
                return new PluginResult(PluginResult.Status.INVALID_ACTION);
            }
        } catch (FileNotFoundException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            JSONObject error = createFileTransferError(FILE_NOT_FOUND_ERR, source, target);
            return new PluginResult(PluginResult.Status.IO_EXCEPTION, error);
        } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            JSONObject error = createFileTransferError(INVALID_URL_ERR, source, target);
            return new PluginResult(PluginResult.Status.IO_EXCEPTION, error);
        } catch (SSLException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            Log.d(LOG_TAG, "Got my ssl exception!!!");
            JSONObject error = createFileTransferError(CONNECTION_ERR, source, target);
            return new PluginResult(PluginResult.Status.IO_EXCEPTION, error);
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            JSONObject error = createFileTransferError(CONNECTION_ERR, source, target);
            return new PluginResult(PluginResult.Status.IO_EXCEPTION, error);
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            return new PluginResult(PluginResult.Status.JSON_EXCEPTION);
        }
    }

    // always verify the host - don't check for certificate
    final static HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };

    /**
     * This function will install a trust manager that will blindly trust all SSL
     * certificates.  The reason this code is being added is to enable developers
     * to do development using self signed SSL certificates on their web server.
     *
     * The standard HttpsURLConnection class will throw an exception on self
     * signed certificates if this code is not run.
     */
    private void trustAllHosts() {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[] {};
            }

            public void checkClientTrusted(X509Certificate[] chain,
                            String authType) throws CertificateException {
            }

            public void checkServerTrusted(X509Certificate[] chain,
                            String authType) throws CertificateException {
            }
        } };

        // Install the all-trusting trust manager
        try {
            // Backup the current SSL socket factory
            defaultSSLSocketFactory = HttpsURLConnection.getDefaultSSLSocketFactory();
            // Install our all trusting manager
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage(), e);
        }
    }

    /**
     * Create an error object based on the passed in errorCode
     * @param errorCode 	the error
     * @return JSONObject containing the error
     */
    private JSONObject createFileTransferError(int errorCode, String source, String target) {
        JSONObject error = null;
        try {
            error = new JSONObject();
            error.put("code", errorCode);
            error.put("source", source);
            error.put("target", target);
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
        }
        return error;
    }

    /**
     * Convenience method to read a parameter from the list of JSON args.
     * @param args			the args passed to the Plugin
     * @param position		the position to retrieve the arg from
     * @param defaultString the default to be used if the arg does not exist
     * @return String with the retrieved value
     */
    private String getArgument(JSONArray args, int position, String defaultString) {
        String arg = defaultString;
        if(args.length() >= position) {
            arg = args.optString(position);
            if (arg == null || "null".equals(arg)) {
                arg = defaultString;
            }
        }
        return arg;
    }

    /**
     * Uploads the specified file to the server URL provided using an HTTP
     * multipart request.
     * @param file      Full path of the file on the file system
     * @param server        URL of the server to receive the file
     * @param fileKey       Name of file request parameter
     * @param fileName      File name to be used on server
     * @param mimeType      Describes file content type
     * @param params        key:value pairs of user-defined parameters
     * @return FileUploadResult containing result of upload request
     */
    public FileUploadResult upload(String file, String server, final String fileKey, final String fileName,
            final String mimeType, JSONObject params, boolean trustEveryone, boolean chunkedMode) throws IOException, SSLException {
        // Create return object
        FileUploadResult result = new FileUploadResult();

        // Get a input stream of the file on the phone
        InputStream fileInputStream = getPathFromUri(file);

        HttpURLConnection conn = null;
        DataOutputStream dos = null;

        int bytesRead, bytesAvailable, bufferSize;
        long totalBytes;
        byte[] buffer;
        int maxBufferSize = 8096;

        //------------------ CLIENT REQUEST
        // open a URL connection to the server
        URL url = new URL(server);

        // Open a HTTP connection to the URL based on protocol
        if (url.getProtocol().toLowerCase().equals("https")) {
            // Using standard HTTPS connection. Will not allow self signed certificate
            if (!trustEveryone) {
                conn = (HttpsURLConnection) url.openConnection();
            }
            // Use our HTTPS connection that blindly trusts everyone.
            // This should only be used in debug environments
            else {
                // Setup the HTTPS connection class to trust everyone
                trustAllHosts();
                HttpsURLConnection https = (HttpsURLConnection) url.openConnection();
                // Save the current hostnameVerifier
                defaultHostnameVerifier = https.getHostnameVerifier();
                // Setup the connection not to verify hostnames
                https.setHostnameVerifier(DO_NOT_VERIFY);
                conn = https;
            }
        }
        // Return a standard HTTP connection
        else {
            conn = (HttpURLConnection) url.openConnection();
        }

        // Allow Inputs
        conn.setDoInput(true);

        // Allow Outputs
        conn.setDoOutput(true);

        // Don't use a cached copy.
        conn.setUseCaches(false);

        // Use a post method.
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Connection", "Keep-Alive");
        conn.setRequestProperty("Content-Type", "multipart/form-data;boundary="+BOUNDRY);

        // Set the cookies on the response
        String cookie = CookieManager.getInstance().getCookie(server);
        if (cookie != null) {
            conn.setRequestProperty("Cookie", cookie);
        }

        // Should set this up as an option
        if (chunkedMode) {
            conn.setChunkedStreamingMode(maxBufferSize);
        }

        dos = new DataOutputStream( conn.getOutputStream() );

        // Send any extra parameters
        try {
            for (Iterator iter = params.keys(); iter.hasNext();) {
                Object key = iter.next();
                dos.writeBytes(LINE_START + BOUNDRY + LINE_END);
                dos.writeBytes("Content-Disposition: form-data; name=\"" +  key.toString() + "\";");
                dos.writeBytes(LINE_END + LINE_END);
                dos.write(params.getString(key.toString()).getBytes());
                dos.writeBytes(LINE_END);
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
        }

        dos.writeBytes(LINE_START + BOUNDRY + LINE_END);
        dos.writeBytes("Content-Disposition: form-data; name=\"" + fileKey + "\";" + " filename=\"" + fileName +"\"" + LINE_END);
        dos.writeBytes("Content-Type: " + mimeType + LINE_END);
        dos.writeBytes(LINE_END);

        // create a buffer of maximum size
        bytesAvailable = fileInputStream.available();
        bufferSize = Math.min(bytesAvailable, maxBufferSize);
        buffer = new byte[bufferSize];

        // read file and write it into form...
        bytesRead = fileInputStream.read(buffer, 0, bufferSize);
        totalBytes = 0;

        while (bytesRead > 0) {
            totalBytes += bytesRead;
            result.setBytesSent(totalBytes);
            dos.write(buffer, 0, bufferSize);
            bytesAvailable = fileInputStream.available();
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            bytesRead = fileInputStream.read(buffer, 0, bufferSize);
        }

        // send multipart form data necesssary after file data...
        dos.writeBytes(LINE_END);
        dos.writeBytes(LINE_START + BOUNDRY + LINE_START + LINE_END);

        // close streams
        fileInputStream.close();
        dos.flush();
        dos.close();

        //------------------ read the SERVER RESPONSE
        StringBuffer responseString = new StringBuffer("");
        DataInputStream inStream;
        try {
            inStream = new DataInputStream ( conn.getInputStream() );
        } catch(FileNotFoundException e) {
            throw new IOException("Received error from server");
        }

        String line;
        while (( line = inStream.readLine()) != null) {
            responseString.append(line);
        }
        Log.d(LOG_TAG, "got response from server");
        Log.d(LOG_TAG, responseString.toString());

        // send request and retrieve response
        result.setResponseCode(conn.getResponseCode());
        result.setResponse(responseString.toString());

        inStream.close();
        conn.disconnect();

        // Revert back to the proper verifier and socket factories
        if (trustEveryone && url.getProtocol().toLowerCase().equals("https")) {
            ((HttpsURLConnection)conn).setHostnameVerifier(defaultHostnameVerifier);
            HttpsURLConnection.setDefaultSSLSocketFactory(defaultSSLSocketFactory);
        }

        return result;
    }

    /**
     * Downloads a file form a given URL and saves it to the specified directory.
     *
     * @param source        URL of the server to receive the file
     * @param target      	Full path of the file on the file system
     * @return FileDownloadResult An object with information about the download progress.
     */
    public FileDownloadResult download(String source, String target, String callbackId) throws IOException {
        try {
            FileDownloadResult result = new FileDownloadResult();
            File file = new File(target);

            // create needed directories
            file.getParentFile().mkdirs();

            // connect to server
            URL url = new URL(source);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setDoOutput(true);
            connection.connect();
            
            Log.d(LOG_TAG, "Download file:" + url);

            // set the response code
            result.setResponseCode(connection.getResponseCode());
            // set the content length
            result.setBytesTotal(connection.getContentLength());
            
            InputStream inputStream = connection.getInputStream();
            byte[] buffer = new byte[4096];
            int bytesRead = 0;
            long totalBytes = 0;

            FileOutputStream outputStream = new FileOutputStream(file);

            // write bytes to file
            bytesRead = inputStream.read(buffer);
            PluginResult progress;
            
            // last time the progress callback was executed
            long lastTime = 0;
            long curTime = 0;

            while ( bytesRead > 0 ) {
                outputStream.write(buffer,0, bytesRead);
                totalBytes += bytesRead;
                result.setBytesReceived(totalBytes);
                bytesRead = inputStream.read(buffer);
                // fire the success callback to inform about the progress (only every 200 ms)
                curTime = System.currentTimeMillis();
                if((bytesRead == 0) || (curTime > (lastTime + 200)))
                {
                	progress = new PluginResult(PluginResult.Status.OK, result.toJSONObject(), FileDownloadResult.CAST_CODE);
                	progress.setKeepCallback(true);
                	success(progress, callbackId);
                	lastTime = curTime;
                }
            }


            // clean up
            inputStream.close();
            connection.disconnect();
            outputStream.close();

            Log.d(LOG_TAG, "Saved file: " + target);

            // create FileEntry object
            FileUtils fileUtil = new FileUtils();


            result.setFileEntry(fileUtil.getEntry(file));
            result.setCompleted();
            return result;
        } catch (Exception e) {
            Log.d(LOG_TAG, e.getMessage(), e);
            throw new IOException("Error while downloading");
        }
    }

    /**
     * Get an input stream based on file path or content:// uri
     *
     * @param path
     * @return an input stream
     * @throws FileNotFoundException
     */
    private InputStream getPathFromUri(String path) throws FileNotFoundException {
        if (path.startsWith("content:")) {
            Uri uri = Uri.parse(path);
            return ctx.getContentResolver().openInputStream(uri);
        }
        else if (path.startsWith("file://")) {
            return new FileInputStream(path.substring(7));
        }
        else {
            return new FileInputStream(path);
        }
    }

}
