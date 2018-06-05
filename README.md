# Cordova File Chooser Plugin

Plugin that returns the file path choosen from filemanager and also get filesize.

## Using

Create a new Cordova Project

    $ cordova create hello com.example.helloapp Hello
    
Install the plugin

    $ cd hello
    $ cordova plugin add https://github.com/Manishkotian/cordova_fileChooser.git
    

Edit `www/js/index.js` and add the following code inside `onDeviceReady`

```js

    hello.fileChooser( function success(path){
		console.log("success--file path->"+path);
	}, function failure(err){
		console.log("failure--error message->"+err);
    });


	
      hello.fileSize(path, function success(fileSizeBytes){
		console.log("success--file size->"+fileSizeBytes);
	}, function failure(err){
		console.log("failure--error message->"+err);
    });

