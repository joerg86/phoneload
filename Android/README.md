# PhoneLoad plugin #
This plugin provides you with an advanced FileTransfer interface

## Add the plugin to your project ##

Load the JavaScript code in your HTML file:
&lt;script src="phonegap-phoneload-android.js"&gt;&lt;/script&gt;<br/>

Add this line to res/plugins.xml:
&lt;plugin name="StatusBarNotification" value="com.phonegap.plugins.statusBarNotification.StatusBarNotification"/&gt;<br/>

Add the "src" folder to your java classpath.

## How to use the plugin ##

var ft = new window.plugins.phoneLoad.FileTransfer();<br/>
ft.download(url, successCB, errorCB);<br/>

The successCB callback is periodically provided with a FileDownloadResult object that looks like this:

{
   completed: false, // whether the download is still in progress or not<br/>
   bytesReceived: 1000, // the amount of bytes already downloaded<br/>
   bytesTotal: 2000, // the total amount of bytes to load<br/>
   percentCompleted: 50.0, // for convenience: the overall progress in percent <br/>
   responseCode: 200, // the HTTP response code <br/>
}

Cancel a running download with the cancel() function:

ft.cancel()

This will cancel the download and call the errorCB once.


