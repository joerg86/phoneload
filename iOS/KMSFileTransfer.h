/*
 * PhoneGap is available under *either* the terms of the modified BSD license *or* the
 * MIT License (2008). See http://opensource.org/licenses/alphabetical for full text.
 * 
 * Copyright (c) 2005-2011, Nitobi Software Inc.
 * Copyright (c) 2011, Matt Kane
 * Copyright (c) 2011, IBM Corporation
 */


#import <Foundation/Foundation.h>
#import <PhoneGap/PGPlugin.h>

#define UPLOAD      0
#define DOWNLOAD    1

@interface KMSFileTransfer : PGPlugin 
{
    
}
- (void) upload:(NSMutableArray*)arguments withDict:(NSMutableDictionary*)options;

- (void) download:(NSMutableArray*)arguments withDict:(NSMutableDictionary*)options;

- (void) cancel:(NSMutableArray*)arguments withDict:(NSMutableDictionary*)options;

- (void) downloadFail:(NSMutableArray*)arguments;
- (void) downloadSuccess:(NSMutableArray*)arguments;

- (NSMutableDictionary*) createFileTransferError:(NSString*)code AndSource:(NSString*)source AndTarget:(NSString*)target;

@end;


@interface KMSFileTransferDelegate : NSObject 
{
    BOOL                isUpDown;               // if 0 then upload, if 1 then download
    NSMutableData *     receivedData;
	KMSFileTransfer*    command;
	NSString*           callbackId;
	NSString*           source;
	NSString*           target;
    NSString*           idString;
    NSInteger           bytesCurrent;
    NSInteger           bytesTotal;
}

- (NSMutableDictionary *)makeSuccessCallbackData:(BOOL) isFinished;

@property (nonatomic, retain) NSMutableData* receivedData;
@property (nonatomic, retain) KMSFileTransfer* command;
@property (nonatomic, retain) NSString* callbackId;
@property (nonatomic, retain) NSString* source;
@property (nonatomic, retain) NSString* target;
@property (nonatomic, retain) NSString* idString;

@property NSInteger bytesCurrent;
@property NSInteger bytesTotal;
@property BOOL      isUpDown;
@end;

