README.TXT - ServiceMix Drools Test Block
Johannes Minor (johannes.minor@tut.fi)
	


Drools Block


Instructions:  

1) Download and run Apache ServiceMix
		As of Feb 2013, ServiceMix requires Java 6.  So make sure Java 6 is installed, and the JAVA_HOME environment variable points to jre6 home.

2) Start ServiceMix, and install required features and bundles:
	
	features:install servicemix-drools
	features:install camel-jetty
	osgi:install -s mvn:org.apache.servicemix.bundles/org.apache.servicemix.bundles.commons-io

3) Edit the fi.tut.fast.smx.droolstest.cfg file if you want to change any default parameters.

4) Drop drools-test-0.0.1-SNAPSHOT.jar into the apache-servicemix-4.4.2/deploy folder, and start the bundle

5) Copy pcdemo.xsd and pcrules.drl into the apache-servicemix-4.4.2/DroolsTestDeployFolder to compile the schema and rules, and initiate a drools knowledge session

6) Use some HTTP Client to post messages to the address configured in the config file in Step 3. (default is http://localhost:8008/droolsTest ).  
Some HTTP Clients : 	REST Tool :  http://code.google.com/p/rest-client/
			Fiddler2 : http://www.fiddler2.com/fiddler2/
			Fetcher (OSX) : https://itunes.apple.com/us/app/fetcher/id440113616?mt=12

7) To overwrite the schema or rules file, simply copy another file with the same name into the DroolsTestDeployFolder directory.  No bundle restart is required. 

8) To clear the rules and schema cache, go to http://localhost:8008/droolsTest/clear in a browser.
	
	