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

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Encapsulates the result and/or status of uploading a file to a remote server.
 */
public class FileDownloadResult extends FileTransferResult {
    public static final String CAST_CODE=
    		"function(x) { if(x.fileEntry) x.fileEntry=window.localFileSystem._castEntry(x.fileEntry); return x }";
    
    private long bytesReceived = 0;         // bytes sent
    private long bytesTotal = 0;
    private JSONObject fileEntry;
    private boolean completed = false;
    
    public long getBytesReceived() {
        return bytesReceived;
    }
    
    public void setBytesReceived(long bytes) {
        this.bytesReceived = bytes;
    }
    
    public long getBytesTotal() {
        return bytesTotal;
    }
    
    public void setBytesTotal(long bytes) {
        this.bytesTotal = bytes;
    }
    
    public boolean getCompleted() {
    	return completed;
    }
    
    public void setCompleted() {
    	this.completed = true;
    }
    
    public double getPercentCompleted() {
    	if(bytesTotal > 0)
    		return new Double(bytesReceived) / new Double(bytesTotal) * 100.0;
    	else return -1; // feature not available
    }
    
    public JSONObject getFileEntry() {
        return fileEntry;
    }

    public void setFileEntry(JSONObject entry) {
       this.fileEntry = entry;
    }

    public JSONObject toJSONObject() throws JSONException {
        JSONObject result = new JSONObject();
        result.put("bytesReceived", getBytesReceived());
        result.put("bytesTotal", getBytesTotal());
        result.put("percentCompleted", getPercentCompleted());
        result.put("responseCode", getResponseCode());
        result.put("completed", getCompleted());

        if(fileEntry != null)
           result.put("fileEntry", fileEntry.toString());

        return result;
    }
}
