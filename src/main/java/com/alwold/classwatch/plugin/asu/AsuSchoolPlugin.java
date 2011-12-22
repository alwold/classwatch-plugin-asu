package com.alwold.classwatch.plugin.asu;

import com.alwold.classwatch.model.Status;
import com.alwold.classwatch.school.ClassInfo;
import com.alwold.classwatch.school.RetrievalException;
import com.alwold.classwatch.school.SchoolPlugin;
import com.alwold.classwatch.school.ScrapeException;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.transform.TransformerException;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.apache.xpath.XPathAPI;
import org.cyberneko.html.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author alwold
 */
public class AsuSchoolPlugin implements SchoolPlugin {
	private static Logger log = Logger.getLogger(AsuSchoolPlugin.class);

	public ClassInfo getClassInfo(String termCode, String classNumber) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public Status getClassStatus(String termCode, String classNumber) throws RetrievalException {
		try {
			HttpClient client = new HttpClient();
			client.getState().addCookie(new Cookie("webapp4.asu.edu", "onlineCampusSelection", "C"));
			GetMethod get = new GetMethod("https://webapp4.asu.edu/catalog/classlist?&k=" + classNumber + "&t=" + termCode + "&e=all&init=false&nopassive=true");
			client.executeMethod(get);
			InputStream is = get.getResponseBodyAsStream();
			DOMParser parser = new DOMParser();
			// somehow this prevents errors when using xpath
			parser.setFeature("http://xml.org/sax/features/namespaces", false);
			parser.parse(new InputSource(is));
			Document doc = parser.getDocument();
			Node node = XPathAPI.selectSingleNode(doc, "//TR[TD/@class='classNbrColumnValue']/TD[@class='availableSeatsColumnValue']/TABLE/TR/TD/SPAN/SPAN[@class='tt']");
			String status = node.getAttributes().getNamedItem("rel").getTextContent();
			if (status.equals("#tt_seats-open")) {
				log.info("Class is open");
				return Status.OPEN;
			} else if (status.equals("#tt_seats-reserved")) {
				log.info("Class is open, but all seats are reserved");
				return Status.CLOSED;
			} else if (status.equals("#tt_seats-closed")) {
				log.info("Class is closed");
				return Status.CLOSED;
			} else {
				throw new ScrapeException("Unknown status: " + status);
			}
		} catch (IOException e) {
			throw new RetrievalException(e);
		} catch (SAXException e) {
			throw new RetrievalException(e);
		} catch (TransformerException e) {
			throw new RetrievalException(e);
		}
	}
	
}
