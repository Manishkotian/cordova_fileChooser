/*global cordova, module*/

module.exports = {
    fileChooser: function (successCallback, errorCallback) {
    	console.log("Calling filechooser from custom plugin: ");
	cordova.exec(successCallback, errorCallback, "Hello", "fileChooser"); 
    },
    
    fileSize: function (filePath, successCallback, errorCallback) {
    	console.log("Calling fileSize from custom plugin: ");
	cordova.exec(successCallback, errorCallback, "Hello", "fileSize", [filePath]); 
    }
};
