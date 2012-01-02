/*
 *     Licensed to the Apache Software Foundation (ASF) under one
 *     or more contributor license agreements.  See the NOTICE file
 *     distributed with this work for additional information
 *     regarding copyright ownership.  The ASF licenses this file
 *     to you under the Apache License, Version 2.0 (the
 *     "License"); you may not use this file except in compliance
 *     with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing,
 *     software distributed under the License is distributed on an
 *     "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *     KIND, either express or implied.  See the License for the
 *     specific language governing permissions and limitations
 *     under the License.
 */

/**
 * FileTransfer uploads a file to a remote server.
 * @constructor
 */
var PhoneLoad = function() {
}

PhoneLoad.prototype.FileTransfer = function() {
    this.id = PhoneGap.createUUID();
};

/**
 * FileUploadResult
 * @constructor
 */
PhoneLoad.prototype.FileUploadResult = function() {
    this.bytesSent = 0;
    this.responseCode = null;
    this.response = null;
};

/**
 * FileDownloadResult
 * @constructor
 */
PhoneLoad.prototype.FileDownloadResult = function() {
    this.bytesReceived = 0;
    this.responseCode = null;
}

/**
 * FileTransferError
 * @constructor
 */
PhoneLoad.prototype.FileTransferError = function() {
    this.code = null;
};

PhoneLoad.prototype.FileTransferError.FILE_NOT_FOUND_ERR = 1;
PhoneLoad.prototype.FileTransferError.INVALID_URL_ERR = 2;
PhoneLoad.prototype.FileTransferError.CONNECTION_ERR = 3;

/**
* Given an absolute file path, uploads a file on the device to a remote server
* using a multipart HTTP request.
* @param filePath {String}           Full path of the file on the device
* @param server {String}             URL of the server to receive the file
* @param successCallback (Function}  Callback to be invoked when upload has completed
* @param errorCallback {Function}    Callback to be invoked upon error
* @param options {FileUploadOptions} Optional parameters such as file name and mimetype
*/
PhoneLoad.prototype.FileTransfer.prototype.upload = function(filePath, server, successCallback, errorCallback, options, debug) {

    // check for options
    var fileKey = null;
    var fileName = null;
    var mimeType = null;
    var params = null;
    var chunkedMode = true;
    if (options) {
        fileKey = options.fileKey;
        fileName = options.fileName;
        mimeType = options.mimeType;
        if (options.chunkedMode !== null || typeof options.chunkedMode !== "undefined") {
            chunkedMode = options.chunkedMode;
        }
        if (options.params) {
            params = options.params;
        }
        else {
            params = {};
        }
    }

    PhoneGap.exec(successCallback, errorCallback, 'PhoneLoad', 'upload', [filePath, server, fileKey, fileName, mimeType, params, debug, chunkedMode]);
};

/**
 * Downloads a file form a given URL and saves it to the specified directory.
 * @param source {String}          URL of the server to receive the file
 * @param target {String}         Full path of the file on the device
 * @param successCallback (Function}  Callback to be invoked when upload has completed
 * @param errorCallback {Function}    Callback to be invoked upon error
 */
PhoneLoad.prototype.FileTransfer.prototype.download = function(source, target, successCallback, errorCallback) {
    PhoneGap.exec(successCallback, errorCallback, 'PhoneLoad', 'download', [source, target, this.id]);
};

PhoneLoad.prototype.FileTransfer.prototype.cancel = function(successCallback) {
   PhoneGap.exec(successCallback, null, "PhoneLoad", "cancel_download", [this.id]);
};

/**
 * Options to customize the HTTP request used to upload files.
 * @constructor
 * @param fileKey {String}   Name of file request parameter.
 * @param fileName {String}  Filename to be used by the server. Defaults to image.jpg.
 * @param mimeType {String}  Mimetype of the uploaded file. Defaults to image/jpeg.
 * @param params {Object}    Object with key: value params to send to the server.
 */
PhoneLoad.prototype.FileUploadOptions = function(fileKey, fileName, mimeType, params) {
    this.fileKey = fileKey || null;
    this.fileName = fileName || null;
    this.mimeType = mimeType || null;
    this.params = params || null;
};

PhoneGap.addConstructor(function() {
    PhoneGap.addPlugin("phoneLoad", new PhoneLoad());
});
