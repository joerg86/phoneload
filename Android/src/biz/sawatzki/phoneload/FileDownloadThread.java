package biz.sawatzki.phoneload;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.util.Log;

import com.phonegap.api.PluginResult;
import com.phonegap.FileUtils;

public class FileDownloadThread extends Thread {
	private static final String LOG_TAG = "FileDownloader";
	private String source, target, callbackId;
	private PhoneLoad fileTransfer;
	private boolean stopRequested = false;
	
	public FileDownloadThread(PhoneLoad fileTransfer, String source, String target, String callbackId) {
		this.source = source;
		this.target = target;
		this.callbackId = callbackId;
		this.fileTransfer = fileTransfer;
	}
	@Override
	public void run() {
		try {
			download(source, target, callbackId);
		}
		catch (IOException e)
		{
			fileTransfer.error("IO error while downloading", callbackId);
		}
	}
	public void requestStop() {
		stopRequested = true;
	}
	  /**
     * Downloads a file form a given URL and saves it to the specified directory.
     *
     * @param source        URL of the server to receive the file
     * @param target      	Full path of the file on the file system
     * @param callbackId 	ID of the JS callback
     */
    public void download(String source, String target, String callbackId) throws IOException {
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
                	fileTransfer.success(progress, callbackId);
                	lastTime = curTime;
                }
                if(stopRequested)
                {
                	fileTransfer.error("Download canceled.", callbackId);
                	return;
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
        	PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, result.toJSONObject(), FileDownloadResult.CAST_CODE);

            fileTransfer.success(pluginResult, callbackId);
         
        } catch (Exception e) {
            Log.d(LOG_TAG, e.getMessage(), e);
            throw new IOException("Error while downloading");
        }
    }

}
