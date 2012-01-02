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
public class FileUploadResult extends FileTransferResult {
    
    private long bytesSent = 0;         // bytes sent
    private String response = null;     // HTTP response
       
    public long getBytesSent() {
        return bytesSent;
    }
    
    public void setBytesSent(long bytes) {
        this.bytesSent = bytes;
    }
    
    public String getResponse() {
        return response;
    }
    
    public void setResponse(String response) {
        this.response = response;
    }

    public JSONObject toJSONObject() throws JSONException {
        return new JSONObject(
                "{bytesSent:" + bytesSent + 
                ",responseCode:" + getResponseCode() + 
                ",response:" + JSONObject.quote(response) + "}");
    }
}