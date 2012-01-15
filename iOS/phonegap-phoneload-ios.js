/*
 * PhoneGap is available under *either* the terms of the modified BSD license *or* the
 * MIT License (2008). See http://opensource.org/licenses/alphabetical for full text.
 *  
 * Copyright (c) 2005-2011, Nitobi Software Inc.
 * Copyright (c) 2011, Matt Kane
 */

/*
 * FileTransfer uploads a file to a remote server.
 */

    var PhoneLoad = function() 
    {
        
    };

    PhoneLoad.prototype.FileTransfer = function()
    {
        this.id = "";
    };

/**
* Given an absolute file path, uploads a file on the device to a remote server 
* using a multipart HTTP request.
* @param filePath {String}           Full path of the file on the device
* @param server {String}             URL of the server to receive the file
* @param successCallback (Function}  Callback to be invoked after some data did send.
* @param errorCallback {Function}    Callback to be invoked upon error
* @param options {FileUploadOptions} Optional parameters such as file name and mimetype           
*/
    PhoneLoad.prototype.FileTransfer.prototype.upload = function(filePath, server, successCallback, errorCallback, options) 
    {
        if(!options.params) 
        {
            options.params = {};
        }
        options.filePath = filePath;	
        options.server = server;
        
        var microtime = new Date().getTime();
        this.id = PhoneGap.createUUID() + microtime;

        options.id = this.id;
        
        if(!options.fileKey) 
        {
            options.fileKey = 'file';
        }
        if(!options.fileName) 
        {
            options.fileName = 'image.jpg';
        }
        if(!options.mimeType) 
        {
            options.mimeType = 'image/jpeg';
        }
	
        // successCallback required
        if (typeof successCallback != "function") 
        {
            console.log("FileTransfer Error: successCallback is not a function");
            return;
        }


        // errorCallback optional
        if (errorCallback && (typeof errorCallback != "function")) 
        {
            console.log("FileTransfer Error: errorCallback is not a function");
            return;
        }
	
        PhoneGap.exec(successCallback, errorCallback, 'biz.sawatzki.phoneload', 'upload', [options]);
    };


/**
 * Downloads a file form a given URL and saves it to the specified directory.
 * @param source {String}          URL of the server to receive the file
 * @param target {String}         Full path of the file on the device
 * @param successCallback (Function}  Callback to be invoked when receiving some data.
 * @param errorCallback {Function}    Callback to be invoked upon error
 */
    PhoneLoad.prototype.FileTransfer.prototype.download = function(source, target, successCallback, errorCallback) 
    {
        var microtime = new Date().getTime();
        this.id = PhoneGap.createUUID() + microtime;
        
        PhoneGap.exec(successCallback, errorCallback, 'biz.sawatzki.phoneload', 'download', [source, target, this.id]);
    };

/**
 *  Cancel upload or download
 */
    PhoneLoad.prototype.FileTransfer.prototype.cancel = function(successCallback)
    {
        PhoneGap.exec(successCallback, null, 'biz.sawatzki.phoneload','cancel',[this.id]);
    };


/**
 * Options to customize the HTTP request used to upload files.
 * @param fileKey {String}   Name of file request parameter.
 * @param fileName {String}  Filename to be used by the server. Defaults to image.jpg.
 * @param mimeType {String}  Mimetype of the uploaded file. Defaults to image/jpeg.
 * @param params {Object}    Object with key: value params to send to the server.
 */
    PhoneLoad.prototype.FileUploadOptions = function(fileKey, fileName, mimeType, params) 
    {
        this.fileKey = fileKey || null;
        this.fileName = fileName || null;
        this.mimeType = mimeType || null;
        this.params = params || null;
    };


    PhoneGap.addConstructor(function()
                            {
                                //if(typeof navigator.phoneLoad == "undefined") navigator.phoneLoad = new PhoneLoad();
                                if(typeof window.plugins == "undefined")
                                    window.plugins = new Object();
                                window.plugins.phoneLoad = new PhoneLoad();
                            }
    );

