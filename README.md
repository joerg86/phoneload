# PhoneLoad plugin #
This plugin provides you with an advanced FileTransfer interface. It makes it possible to retrieve status information of a currently running download through a callback and allows you to cancel a running transfer as well. We have added that functionality to the upload function as well, but currently only for the iOS version. Use at your own risk. :o)

## Add the plugin to your project ##

### Android ###
Load the JavaScript code in your HTML file:
&lt;script src="phonegap-phoneload-android.js"&gt;&lt;/script&gt;<br/>

Add this line to res/plugins.xml:
&lt;plugin name="PhoneLoad" value="biz.sawatzki.phoneload.PhoneLoad"/&gt;<br/>

Add the "src" folder to your java classpath.

### iOS ###
Load the JavaScript code in your HTML file:
&lt;script src="phonegap-phoneload-ios.js"&gt;&lt;/script&gt;<br/>

Add "PhoneLoad.m" and "PhoneLoad.h" to the "Plugins" directory in your Xcode project.
Open PhoneGap.plist in your project's "Supporting Files" folder and add a new entry to the "Plugins" dictionary: Use "biz.sawatzki.phoneload" as key and "PhoneLoad" as value (type is "String").


## How to use the plugin ##

var ft = new window.plugins.phoneLoad.FileTransfer();<br/>
ft.download(url, target, successCB, errorCB);<br/>

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


