# AndroidPhoneNamesFinder

This Library is useful to get the device name of the phone the user is using, instead of a code
name  
or any other kind of names this library aims at generating the most perfect name possible

Add it in your root build.gradle at the end of repositories:```css

	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}

To Enable this Library first Add this line on the build.gradle(:app)

	implementation 'com.github.aishik212:AndroidPhoneNamesFinder:v1.0.2'

Use this code to get the device name

	DeviceNameFinder.getPhoneValues(this, object : DeviceDetailsListener
	{  
	    override fun details(doQuery: DeviceDetailsModel?) 
	    {  
	        super.details(doQuery)  
	        Log.d(TAG, "details: "+doQuery?.calculatedName)  
	    }  
	})

These are the values you will get from DeviceDetailsModel

	val brand: String? #This is the brandName of the Device  
	val commonName: String?, #This is the most common Name of the Device  
	val codeName: String?,  #This is the codeName of the Device
	val modelName: String?,  #This is the another uncommon Name of the Device
	val calculatedName: String?, #This is the special name that this library tries to create from the above data.

EG -
> brand=Google
> commonName=Google Android Emulator
> codeName=generic_x86_arm
> modelName=sdk_gphone_x86
> calculatedName=Google Android Emulator

This is the Details of the Android Emulator

Please Contribute to the Project by providing me with issues