/**
 * 
 */
package test;

import java.io.File;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.buf.ByteChunk;
import org.eclipse.jdt.internal.core.Assert;

/**
 * <p>Project:TomcatAnalysis
 * <p>Package:test
 * <p>Title:TomcatServerStartupTest
 * <p>Description:
 * <p>Company:ShangCheng Tech
 * @author huangyejun
 * Create Date:2017-11-24 下午01:52:07
 * @version
 */
public class TomcatServerStartupTest {
	
	private static Tomcat tomcat = null;
	
	private static Tomcat getInstance(){
		if(tomcat == null)
			tomcat = new Tomcat();
		return tomcat;
	}

	public static void main(String[] args) throws Exception {
		
		Tomcat tomcat = getInstance();
		tomcat.addWebapp(null, "\\examples", "D:\\tomcat7\\apache-tomcat-7.0.79\\webapps\\examples");
		tomcat.start();
//		ByteChunk res = getUrl("http://localhost:8080/examples/servlets/HelloWorld"); 
//		assert res.toString().indexOf("<h1>Hello World!</h1>") > 0;
	}
	
	
	
}
