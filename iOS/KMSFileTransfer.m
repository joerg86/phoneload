/*
 * PhoneGap is available under *either* the terms of the modified BSD license *or* the
 * MIT License (2008). See http://opensource.org/licenses/alphabetical for full text.
 * 
 * Copyright (c) 2005-2011, Nitobi Software Inc.
 * Copyright (c) 2011, Matt Kane
 * Copyright (c) 2011, IBM Corporation
 */

#import "KMSFileTransfer.h"
#import <PhoneGap/File.h>


enum FileTransferError {
	FILE_NOT_FOUND_ERR = 1,
    INVALID_URL_ERR = 2,
    CONNECTION_ERR = 3
};
typedef int FileTransferError;

static NSMutableDictionary * g_ConnectionTable = nil;

@implementation KMSFileTransfer


/********************************************   Upload   ***********************************************************************/
- (void) upload:(NSMutableArray*)arguments withDict:(NSMutableDictionary*)options 	
{
    NSString* callbackId = [arguments objectAtIndex:0];
    NSString* fileKey = (NSString*)[options objectForKey:@"fileKey"];
    NSString* fileName = (NSString*)[options objectForKey:@"fileName"];
    NSString* mimeType = (NSString*)[options objectForKey:@"mimeType"];
    NSMutableDictionary* params = [NSMutableDictionary dictionaryWithDictionary:(NSDictionary*)[options objectForKey:@"params"]];
    NSString* filePath = (NSString*)[options objectForKey:@"filePath"];
    NSString* server = (NSString*)[options objectForKey:@"server"];
    NSString* idString = (NSString*)[options objectForKey:@"id"];
    
    PluginResult* result = nil;
    FileTransferError errorCode = 0;

    
    NSURL* file;
    NSData *fileData = nil;
    
    if ([filePath hasPrefix:@"/"]) {
        file = [NSURL fileURLWithPath:filePath];
    } else {
        file = [NSURL URLWithString:filePath];
    }
    
    NSURL *url = [NSURL URLWithString:server];
    
    
    if (!url) {
        errorCode = INVALID_URL_ERR;
        NSLog(@"File Transfer Error: Invalid server URL");
    } else if(![file isFileURL]) {
        errorCode = FILE_NOT_FOUND_ERR;
        NSLog(@"File Transfer Error: Invalid file path or URL");
    } else {
        // check that file is valid
        NSFileManager* fileMgr = [[NSFileManager alloc] init];
        BOOL bIsDirectory = NO;
        BOOL bExists = [fileMgr fileExistsAtPath:[file path] isDirectory:&bIsDirectory];
        if (!bExists || bIsDirectory) {
            errorCode = FILE_NOT_FOUND_ERR;
        } else {
            // file exists, make sure we can get the data
            fileData = [NSData dataWithContentsOfURL:file];
            
            if(!fileData) {
                errorCode =  FILE_NOT_FOUND_ERR;
                NSLog(@"File Transfer Error: Could not read file data");
            }
        }
        [fileMgr release];
    }
    
    if(errorCode > 0) {
        //result = [PluginResult resultWithStatus: PGCommandStatus_OK messageAsInt: INVALID_URL_ERR cast: @"navigator.fileTransfer._castTransferError"];
        
        result = [PluginResult resultWithStatus:PGCommandStatus_OK messageAsDictionary: [self createFileTransferError:[NSString stringWithFormat:@"%d", errorCode] AndSource:filePath AndTarget:server]];
        
        [self writeJavascript:[result toErrorCallbackString:callbackId]];
        return;
    }
    
    NSMutableURLRequest *req = [NSMutableURLRequest requestWithURL:url];
	[req setHTTPMethod:@"POST"];
	
//    Magic value to set a cookie
	if([params objectForKey:@"__cookie"]) {
		[req setValue:[params objectForKey:@"__cookie"] forHTTPHeaderField:@"Cookie"];
		[params removeObjectForKey:@"__cookie"];
		[req setHTTPShouldHandleCookies:NO];
	}
	
	NSString *boundary = @"*****com.phonegap.formBoundary";
    
	NSString *contentType = [NSString stringWithFormat:@"multipart/form-data; boundary=%@", boundary];
	[req setValue:contentType forHTTPHeaderField:@"Content-Type"];
    //Content-Type: multipart/form-data; boundary=*****com.phonegap.formBoundary
	[req setValue:@"XMLHttpRequest" forHTTPHeaderField:@"X-Requested-With"];
	NSString* userAgent = [[self.webView request] valueForHTTPHeaderField:@"User-agent"];
	if(userAgent) {
		[req setValue: userAgent forHTTPHeaderField:@"User-Agent"];
	}
	
    
	NSMutableData *postBody = [NSMutableData data];
	
	NSEnumerator *enumerator = [params keyEnumerator];
	id key;
	id val;
	
	while ((key = [enumerator nextObject])) {
		val = [params objectForKey:key];
		if(!val || val == [NSNull null]) {
			continue;	
		}
		// if it responds to stringValue selector (eg NSNumber) get the NSString
		if ([val respondsToSelector:@selector(stringValue)]) {
			val = [val stringValue];
		}
		// finally, check whether it is a NSString (for dataUsingEncoding selector below)
		if (![val isKindOfClass:[NSString class]]) {
			continue;
		}
		
		[postBody appendData:[[NSString stringWithFormat:@"--%@\r\n", boundary] dataUsingEncoding:NSUTF8StringEncoding]];
		[postBody appendData:[[NSString stringWithFormat:@"Content-Disposition: form-data; name=\"%@\"\r\n\r\n", key] dataUsingEncoding:NSUTF8StringEncoding]];
		[postBody appendData:[val dataUsingEncoding:NSUTF8StringEncoding]];
		[postBody appendData:[@"\r\n" dataUsingEncoding:NSUTF8StringEncoding]];
	}
    
	[postBody appendData:[[NSString stringWithFormat:@"--%@\r\n", boundary] dataUsingEncoding:NSUTF8StringEncoding]];
	[postBody appendData:[[NSString stringWithFormat:@"Content-Disposition: form-data; name=\"%@\"; filename=\"%@\"\r\n", fileKey, fileName] dataUsingEncoding:NSUTF8StringEncoding]];
    [postBody appendData:[[NSString stringWithFormat:@"Content-Type: %@\r\n\r\n", mimeType] dataUsingEncoding:NSUTF8StringEncoding]];
    NSLog(@"fileData length: %d", [fileData length]);
	[postBody appendData:fileData];
	[postBody appendData:[[NSString stringWithFormat:@"\r\n--%@--\r\n", boundary] dataUsingEncoding:NSUTF8StringEncoding]];
    
    //[req setValue:[[NSNumber numberWithInteger:[postBody length]] stringValue] forHTTPHeaderField:@"Content-Length"];
	[req setHTTPBody:postBody];
    
	
	KMSFileTransferDelegate* delegate = [[[KMSFileTransferDelegate alloc] init] autorelease];
    delegate.isUpDown = UPLOAD;
	delegate.command = self;
    delegate.callbackId = callbackId;
    delegate.source = server;
    delegate.target = filePath;
	delegate.idString = idString;
    
	NSURLConnection * uploadConnection = [NSURLConnection connectionWithRequest:req delegate:delegate];
    
    if( uploadConnection != nil )
    {
        if( g_ConnectionTable == nil )
            g_ConnectionTable = [[NSMutableDictionary alloc] init];
        [g_ConnectionTable setObject:uploadConnection forKey:idString];
    }
}
/********************************************   Download   ***********************************************************************/
- (void) download:(NSMutableArray*)arguments withDict:(NSMutableDictionary*)options {

    NSLog(@"File Transfer downloading file...");
    
    NSString * callbackId = [arguments objectAtIndex:0];
    NSString * sourceUrl = [arguments objectAtIndex:1];
    NSString * filePath = [arguments objectAtIndex:2];
    NSString * idString = [arguments objectAtIndex:3];
    
    NSLog(@"%@",idString);
    
    NSLog(@"Write file %@", filePath);

    NSMutableArray * results;
    
    @try {
        NSString * parentPath = [ filePath stringByDeletingLastPathComponent ];
        
        // check if the path exists => create directories if needed
        if(![[NSFileManager defaultManager] fileExistsAtPath:parentPath ]) 
            [[NSFileManager defaultManager] createDirectoryAtPath:parentPath withIntermediateDirectories:YES attributes:nil error:nil];
        
        // Create the request.
        NSURLRequest *theRequest=[NSURLRequest requestWithURL:[NSURL URLWithString:sourceUrl]
                                                  cachePolicy:NSURLRequestUseProtocolCachePolicy
                                              timeoutInterval:60.0];
        
        KMSFileTransferDelegate * downloadDelegate = [[[KMSFileTransferDelegate alloc] init] autorelease];
        downloadDelegate.isUpDown = DOWNLOAD;
        downloadDelegate.command = self;
        downloadDelegate.callbackId = callbackId;
        downloadDelegate.source = sourceUrl;
        downloadDelegate.target = filePath;
        downloadDelegate.idString = idString;
        
        // create the connection with the request
        // and start loading the data
        NSURLConnection *theConnection=[[NSURLConnection alloc] initWithRequest:theRequest delegate:downloadDelegate];
        if (!theConnection) 
        {
        	// send our results back to the main thread
            results = [NSMutableArray arrayWithObjects: callbackId, [NSString stringWithFormat:@"%d", INVALID_URL_ERR], sourceUrl, filePath, nil];
        	[self downloadFail:results];
        }
        else
        {
            if( g_ConnectionTable == nil )
                g_ConnectionTable = [[NSMutableDictionary alloc] init];
            [g_ConnectionTable setObject:theConnection forKey:idString];
        }
    }
    @catch (id exception) {
        // jump back to main thread
        results = [NSArray arrayWithObjects: callbackId, [NSString stringWithFormat:@"%d", FILE_NOT_FOUND_ERR], sourceUrl, filePath, nil];
        [self downloadFail:results];
    }
}

-(void) downloadSuccess:(NSMutableArray *)arguments
{
    NSString * callbackId = [arguments objectAtIndex:0];
    NSMutableDictionary * callbackData = [arguments objectAtIndex:1];
    NSLog(@"File Transfert Download success");
    PluginResult* result = [PluginResult resultWithStatus: PGCommandStatus_OK messageAsDictionary:callbackData];
    [self writeJavascript: [result toSuccessCallbackString:callbackId]];
}

-(void) downloadFail:(NSMutableArray *)arguments 
{
    NSString * callbackId = [arguments objectAtIndex:0];
    NSString * code = [arguments objectAtIndex:1];
    NSString * source = [arguments objectAtIndex:2];
    NSString * target = [arguments objectAtIndex:3];

    NSLog(@"File Transfer Error: %@", source);
    
    PluginResult* pluginResult = [PluginResult resultWithStatus:PGCommandStatus_OK messageAsDictionary: [self createFileTransferError:code AndSource:source AndTarget:target]];
                                    
    [self writeJavascript: [pluginResult toErrorCallbackString:callbackId]];
}

-(NSMutableDictionary*) createFileTransferError:(NSString*)code AndSource:(NSString*)source AndTarget:(NSString*)target
{
    NSMutableDictionary* result = [NSMutableDictionary dictionaryWithCapacity:3];
    [result setObject: code forKey:@"code"];
	[result setObject: source forKey:@"source"];
	[result setObject: target forKey:@"target"];
    
    return result;
}

- (void) cancel:(NSMutableArray*)arguments withDict:(NSMutableDictionary*)options
{
    NSString* callbackId = [arguments objectAtIndex:0];
    NSString* idString = [arguments objectAtIndex:1];
    
    if( g_ConnectionTable != nil )
    {
        NSURLConnection * connectToCancel = [g_ConnectionTable objectForKey:idString];
        if( connectToCancel != nil )
        {
            [connectToCancel cancel];
            [g_ConnectionTable removeObjectForKey:idString];
        }
    }
	    
    PluginResult* pluginResult = [PluginResult resultWithStatus:PGCommandStatus_OK messageAsDictionary: [NSMutableDictionary dictionaryWithObject:@"Success" forKey:@"result"]];
    NSString * javascript = [pluginResult toSuccessCallbackString:callbackId];
    [self writeJavascript: javascript];
}


@end

/*******************************************************************************************************************************/
                                    /*      The delegate for Transfering files      */

@implementation KMSFileTransferDelegate
@synthesize callbackId, source, target, idString, receivedData, command, bytesCurrent, bytesTotal, isUpDown;

- (void)connection:(NSURLConnection *)connection didReceiveResponse:(NSURLResponse *)response
{
    NSLog(@"ready to receive data");
    if( isUpDown == DOWNLOAD )
    {
        // This method is called when the server has determined that it
        // has enough information to create the NSURLResponse.
        self.bytesTotal = [response expectedContentLength];
        self.bytesCurrent = 0;
        // It can be called multiple times, for example in the case of a
        // redirect, so each time we reset the data.
        
        if( self.receivedData == nil )
        {
            self.receivedData = [[NSMutableData alloc] init];
        }
        [self.receivedData setLength:0];
    }
}

- (void)connection:(NSURLConnection *)connection didReceiveData:(NSData *)data
{
    NSLog(@"received some data");
    if( isUpDown == DOWNLOAD )
    {
        [self.receivedData appendData:data];
        self.bytesCurrent = [self.receivedData length];
        PluginResult* result = [PluginResult resultWithStatus: PGCommandStatus_OK messageAsDictionary:[self makeSuccessCallbackData:NO]];
        [result setKeepCallbackAsBool:YES];
        NSString * javascript = [result toSuccessCallbackString: self.callbackId];
        NSLog(@"%@",javascript);
        [self.command writeJavascript:javascript];
    }
    
    if( isUpDown == UPLOAD )
    {
        char * str = (char *)[data bytes];
        NSString * string = [NSString stringWithUTF8String:str];
        
        NSLog(@"%@",string);
    }
}

- (void)connection:(NSURLConnection *)connection didSendBodyData:(NSInteger)bytesWritten totalBytesWritten:(NSInteger)totalBytesWritten totalBytesExpectedToWrite:(NSInteger)totalBytesExpectedToWrite
{
    NSLog(@"some data was sent.");
    if( isUpDown == UPLOAD )
    {
        self.bytesTotal = totalBytesExpectedToWrite;
        self.bytesCurrent = totalBytesWritten;
        PluginResult* result = [PluginResult resultWithStatus: PGCommandStatus_OK messageAsDictionary:[self makeSuccessCallbackData:NO]];
        [result setKeepCallbackAsBool:YES];        
        [self.command writeJavascript:[result toSuccessCallbackString: self.callbackId]];
        
    }
}

- (void)connectionDidFinishLoading:(NSURLConnection *)connection
{
    NSLog(@"File upload or download finished.");
    if( isUpDown == DOWNLOAD )
    {
        if( [self.receivedData length] > 0 )
        {
            NSError * error = [[NSError alloc] init];
            BOOL response = [self.receivedData writeToFile:self.target options:NSDataWritingFileProtectionNone error:&error];
            if( response == NO )
            {
                //[self connection:connection didFailWithError:error];
                [self.receivedData release];
                self.receivedData = nil;
            }
            else
            {
                // create dictionary to return FileUploadResult object
                PluginResult* result = [PluginResult resultWithStatus: PGCommandStatus_OK messageAsDictionary:[self makeSuccessCallbackData:YES]];
                [result setKeepCallbackAsBool:NO];
                [self.command writeJavascript:[result toSuccessCallbackString: self.callbackId]];            
            }
            [error release];             
        }
    }
    else if( isUpDown == UPLOAD )
    {
        // create dictionary to return FileUploadResult object
        PluginResult* result = [PluginResult resultWithStatus: PGCommandStatus_OK messageAsDictionary:[self makeSuccessCallbackData:YES]];
        [self.command writeJavascript:[result toSuccessCallbackString: self.callbackId]];
    }
    [g_ConnectionTable removeObjectForKey:self.idString];
}

- (void)connection:(NSURLConnection *)connection didFailWithError:(NSError *)error 
{
    NSLog(@"File Transfer Error: %@", [error localizedDescription]);
    if( isUpDown == UPLOAD || isUpDown == DOWNLOAD )
    {
        PluginResult* result = [PluginResult resultWithStatus: PGCommandStatus_OK messageAsDictionary: [command createFileTransferError: [NSString stringWithFormat: @"%d", CONNECTION_ERR] AndSource:source AndTarget:target]];
        [self.command writeJavascript:[result toErrorCallbackString: self.callbackId]];
    }
    [g_ConnectionTable removeObjectForKey:self.idString];    
}

- (NSMutableDictionary *)makeSuccessCallbackData:(BOOL) isFinished
{
    float percentCompleted;
    if( self.bytesTotal <= 0 )
        percentCompleted = 0;
    else
        percentCompleted = ((float)self.bytesCurrent / (float)self.bytesTotal * 100.0);
    
    NSMutableDictionary * successCallbackData = [NSMutableDictionary dictionaryWithCapacity:5];
    [successCallbackData setObject:[NSNumber numberWithBool:isFinished] forKey:@"completed"];
    [successCallbackData setObject:[NSNumber numberWithInt:self.bytesCurrent] forKey:@"bytesCurrent"];
    [successCallbackData setObject:[NSNumber numberWithInt:self.bytesTotal] forKey:@"bytesTotal"];
    [successCallbackData setObject:[NSNumber numberWithFloat:percentCompleted] forKey:@"percentCompleted"];
    [successCallbackData setObject:[NSNumber numberWithInt:200] forKey: @"responseCode"];
    
    //NSLog(@"%@",successCallbackData);
    
    return successCallbackData;
}

- (id) init
{
    if ((self = [super init])) {
        self.receivedData = nil;
    }
    return self;
}
- (void) dealloc
{
    [callbackId release];
	[self.receivedData release];
	[command release];
    [idString release];
    [super dealloc];
}
@end;







