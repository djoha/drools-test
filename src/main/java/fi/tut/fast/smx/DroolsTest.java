/**
 *   DroolsTest.java
 *   
 *   Johannes Minor  (johannes.minor@tut.fi)
 *   
 *   This bean:
 *  	- sets up and configures the Drools session
 *  	- handles incoming XML messages on the HTTP endpoint
 *  	- receives XSD and DRL files in the hot deploy folder in the servicemix directory for reconfiguring the session 
 */

package fi.tut.fast.smx;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.JAXBIntrospector;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.Produce;
import org.apache.commons.io.IOUtils;
import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseConfiguration;
import org.drools.KnowledgeBaseFactory;
import org.drools.KnowledgeBaseFactoryService;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderError;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.KnowledgeBuilderFactoryService;
import org.drools.builder.ResourceType;
import org.drools.builder.help.KnowledgeBuilderHelper;
import org.drools.conf.EventProcessingOption;
import org.drools.io.ResourceFactory;
import org.drools.io.ResourceFactoryService;
import org.drools.runtime.Channel;
import org.drools.runtime.StatefulKnowledgeSession;
import org.osgi.framework.BundleContext;
import org.xml.sax.InputSource;

import com.sun.tools.xjc.Language;
import com.sun.tools.xjc.Options;

import fi.tut.fast.fastory.Order;
import fi.tut.fast.fastory.Process;
import fi.tut.fast.fastory.ProcessStep;

public class DroolsTest implements Processor {

	private static final transient Logger logger = Logger
			.getLogger(DroolsTest.class.getName());
	// System ID used for compiling schema types
	public static final String JAXBSYSTEM_ID = "DROOLSXSD";
	// Constants to define expected file extensions for schema and rules files
	public static final String XSD_FILE_EXT = "xsd";
	public static final String DRL_FILE_EXT = "drl";
	// Output Channel Name
	public static final String OUTPUT_CHANNEL_NAME = "PC_OUTPUT_CHANNEL";
	public static final String CONTROLLER_OUTPUT_CHANNEL = "CONTROLLER_OUTPUT_CHANNEL";

	private StatefulKnowledgeSession ksession;
	private Marshaller marshaller;
	private Unmarshaller unmarshaller;
	private JAXBIntrospector introspector;

	// For interacting with servicemix
	private BundleContext context;

	@Produce(uri="direct:schedulerToController")
	ToController toController;
	
	interface ToController{
		public void sendMessage(Object o);
	}
	
	// Shutdown DroolsTest
	public void destroy() throws Exception {
    	logger.info("OSGi Bundle Stopping.");
    	ksession.dispose();
    	
	}

	// Setting up JAXB Schema Compiler Options
	private Options getOptions() throws IOException {
		// Configure XJC options
		// Simple Binding for adding XMLRootElement annotations to Elements.
		URL bindingUrl = context.getBundle().getResource("/types/binding.xjb");
		InputSource bind = new InputSource(ResourceFactory.newUrlResource(
				bindingUrl).getInputStream());
		bind.setSystemId(JAXBSYSTEM_ID);

		Options xjcOpts = new Options();
		xjcOpts.setSchemaLanguage(Language.XMLSCHEMA);
		xjcOpts.compatibilityMode = Options.EXTENSION;
		xjcOpts.addBindFile(bind);

		return xjcOpts;
	}

	// Initialize the drools Block
	public void init() throws Exception {

		System.out.println("Rules Block Started...");
		try {
			refreshRulesSession();
		} catch (JAXBException e) {
			System.out.println("Error Parsing Schema. Check logs...\n"
					+ e.getMessage());
			logger.log(Level.SEVERE, "Error Parsing Schema. Check logs...\n"
					+ e.getMessage(), e);
		} catch (Exception e) {
			System.out
					.println("Exception Caught while refreshing rules. Check logs...\n"
							+ e.getMessage());
			logger.log(Level.SEVERE,
					"Exception Caught while refreshing rules. Check logs...\n"
							+ e.getMessage(), e);
		}

	}

	// Process an incoming XML message from the HTTP Endpoint
	@Override
	public void process(Exchange exchange) throws Exception {

		// Convert XML to Java object
		Object o = unmarshaller.unmarshal(exchange.getIn().getBody(
				InputStream.class));

		// Inject the object into the Drools Working Memory
		if (o instanceof JAXBElement) {
			System.out.println("Recieved JAXB Element: "
					+ ((JAXBElement) o).getDeclaredType().getCanonicalName());
			ksession.insert(((JAXBElement) o).getValue());
		} else {
			System.out.println("Recieved Element: "
					+ o.getClass().getCanonicalName());
			ksession.insert(o);
		}

		// Evaluate all rules
		ksession.fireAllRules();
		// Send output response
		exchange.getOut().setBody("Accepted.");

	}

	// Refresh rules session when new XSD or DRL files are received.
	private void refreshRulesSession() throws IOException, JAXBException,
			Exception {

		// Dispose of the old session if it exists.
		if (ksession != null) {
			ksession.dispose();
			ksession = null;
		}

		// Get all the cached files
		List<URL> modelFiles = getCachedFiles(XSD_FILE_EXT);
		List<URL> rulesFiles = getCachedFiles(DRL_FILE_EXT);

		// If there is no schema, do nothing.
		if (modelFiles.isEmpty()) {
			System.out.println("No schemas loaded.");
			return;
		}

		// If there are no rules, do nothing.
		if (rulesFiles.isEmpty()) {
			System.out.println("No Rules loaded.");
			return;
		}

		// Loading XSDs with Type Definitions
		KnowledgeBuilder builder = KnowledgeBuilderFactory
				.newKnowledgeBuilder();
		List<String> classList = new ArrayList<String>();
		for (URL url : modelFiles) {
			String[] classNames = KnowledgeBuilderHelper.addXsdModel(
					ResourceFactory.newUrlResource(url), builder, getOptions(),
					JAXBSYSTEM_ID);
			classList.addAll(Arrays.asList(classNames));
		}

		// Set Up Knowledge Base
		KnowledgeBaseConfiguration kbaseConfig = KnowledgeBaseFactory
				.newKnowledgeBaseConfiguration();
		kbaseConfig.setOption(EventProcessingOption.STREAM);
		KnowledgeBase knowledgeBase = KnowledgeBaseFactory
				.newKnowledgeBase(kbaseConfig);

		// Add Rules Definitions Files
		for (URL url : rulesFiles) {
			builder.add(ResourceFactory.newUrlResource(url), ResourceType.DRL);
			// Check Errors
			for (KnowledgeBuilderError err : builder.getErrors()) {
				System.err.println(err);
			}
		}

		// Set up knowledge session
		knowledgeBase.addKnowledgePackages(builder.getKnowledgePackages());
		ksession = knowledgeBase.newStatefulKnowledgeSession();

		// Get Marshaller, Unmarshaller, and Introspector.
		JAXBContext jcontext = KnowledgeBuilderHelper.newJAXBContext(
				classList.toArray(new String[] {}), knowledgeBase);
		introspector = jcontext.createJAXBIntrospector();
		marshaller = jcontext.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		unmarshaller = jcontext.createUnmarshaller();

		System.out.println("Rule Engine Initialized!");

		// Set up Output Channel to recieve and marshall objects and messages
		// from rules
		ksession.registerChannel(CONTROLLER_OUTPUT_CHANNEL, new Channel() {
			@Override
			public void send(Object obj) {

				if (obj instanceof String) {
					// Simple String
					System.out.println("Output To Controller: "
							+ obj);
					toController.sendMessage(obj);
				} else {
					// Object to be unmarshalled
					StringWriter writer = new StringWriter();
					QName name = introspector.getElementName(obj);
					if (name == null) {
						name = new QName(obj.getClass().getPackage().getName(),
								obj.getClass().getSimpleName());
					}
					try {
						marshaller.marshal(new JAXBElement(name,
								obj.getClass(), obj), writer);
						System.out
								.println("Object Recieved on Controller Output Channel:");
						System.out.println(writer.toString());

						toController.sendMessage(writer.toString());
						
					} catch (JAXBException e) {
						logger.log(Level.SEVERE,
								"Cannot Unmashall object on Controller Output Channel.", e);
					}
				}
			}
		});

		ksession.fireAllRules();

	}

	// Store Schemas and rules in folder
	private void cacheFile(Exchange exchange) throws FileNotFoundException,
			IOException {
		// System.out.println(Arrays.toString(exchange.getIn().getHeaders().keySet().toArray(new
		// String[]{})));
		File rulesFile = context.getDataFile(exchange.getIn().getHeader(
				Exchange.FILE_NAME, String.class));
		if (rulesFile.exists()) {
			rulesFile.delete();
		}
		InputStream fileIs = exchange.getIn().getBody(InputStream.class);
		IOUtils.copy(fileIs, new FileOutputStream(rulesFile));
		fileIs.close();
	}

	// Handles new DRL and XSD files dropped into the hot folder
	public void newFileDropped(Exchange exchange) {

		try {
			cacheFile(exchange);
		} catch (FileNotFoundException e1) {
			System.out
					.println("FileNotFoundException caught while caching file. Check logs...\n"
							+ e1.getMessage());
			logger.log(Level.SEVERE,
					"FileNotFoundException caught while caching file. Check logs...\n"
							+ e1.getMessage(), e1);
			return;
		} catch (IOException e1) {
			System.out
					.println("IOException caught while caching file. Check logs...\n"
							+ e1.getMessage());
			logger.log(Level.SEVERE,
					"IOException caught while caching file. Check logs...\n"
							+ e1.getMessage(), e1);
			return;
		}

		System.out.println("[Drools] New File loaded: "
				+ exchange.getIn().getHeader(Exchange.FILE_NAME, String.class));
		try {
			refreshRulesSession();
		} catch (JAXBException e) {
			System.out.println("Error Parsing Schema. Check logs...\n"
					+ e.getMessage());
			logger.log(Level.SEVERE, "Error Parsing Schema. Check logs...\n"
					+ e.getMessage(), e);
		} catch (Exception e) {
			System.out
					.println("Exception Caught while refreshing rules. Check logs...\n"
							+ e.getMessage());
			logger.log(Level.SEVERE,
					"Exception Caught while refreshing rules. Check logs...\n"
							+ e.getMessage(), e);
		}
	}

	// Search cache for files of a certian extension
	private List<URL> getCachedFiles(final String ext) {
		List<URL> files = new ArrayList<URL>();
		File dir = context.getDataFile("");

		for (File f : dir.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pathname.getName().endsWith(ext);
			}
		})) {
			try {
				files.add(f.toURI().toURL());
			} catch (MalformedURLException ex) {
				logger.log(Level.WARNING, "Ignoring File: ", ex);
			}
		}

		return files;
	}

	// Clear the file cache
	public void clearCachedFiles(Exchange ex) {
		clearFileCache();
		ex.getOut().setBody("CACHE CLEARED.");
	}

	private void clearFileCache() {
		File dir = context.getDataFile("");

		for (File f : dir.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pathname.getName().endsWith(XSD_FILE_EXT)
						|| pathname.getName().endsWith(DRL_FILE_EXT);
			}
		})) {
			f.delete();
		}
	}

	/**
	 * 
	 * Getters and Setters for Bean Injection
	 * 
	 */

	public BundleContext getContext() {
		return context;
	}

	public void setContext(BundleContext context) {
		this.context = context;
	}

}
