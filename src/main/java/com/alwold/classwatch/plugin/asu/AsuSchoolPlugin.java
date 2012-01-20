package com.alwold.classwatch.plugin.asu;

import com.alwold.classwatch.model.Status;
import com.alwold.classwatch.school.BaseSchoolPlugin;
import com.alwold.classwatch.school.ClassInfo;
import com.alwold.classwatch.school.RetrievalException;
import com.alwold.classwatch.school.ScrapeException;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.transform.TransformerException;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.log4j.Logger;
import org.apache.xpath.XPathAPI;
import org.cyberneko.html.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author alwold
 */
public class AsuSchoolPlugin extends BaseSchoolPlugin {
	private static Logger logger = Logger.getLogger(AsuSchoolPlugin.class);

	public ClassInfo getClassInfo(String termCode, String classNumber) throws RetrievalException {
		try {
			Document doc = fetchInfo(termCode, classNumber);
			ClassInfo classInfo = new ClassInfo();
			String name = ((Text) XPathAPI.selectSingleNode(doc, "//TR[TD/@class='classNbrColumnValue']/TD[@class='titleColumnValue']/A/text()")).getTextContent();
			String days = ((Text) XPathAPI.selectSingleNode(doc, "//TR[TD/@class='classNbrColumnValue']/TD[@class='dayListColumnValue']/text()")).getTextContent();
			String startTime = ((Text) XPathAPI.selectSingleNode(doc, "//TR[TD/@class='classNbrColumnValue']/TD[@class='startTimeDateColumnValue']/text()")).getTextContent();
			String endTime = ((Text) XPathAPI.selectSingleNode(doc, "//TR[TD/@class='classNbrColumnValue']/TD[@class='endTimeDateColumnValue']/text()")).getTextContent();
			classInfo.setName(name.trim());
			classInfo.setSchedule(days.trim()+" "+startTime.trim()+" - "+endTime.trim());
			logger.trace("name = "+classInfo.getName());
			logger.trace("schedule = "+classInfo.getSchedule());
			return classInfo;
		} catch (TransformerException e) {
			throw new RetrievalException(e);
		}
	}

	public Status getClassStatus(String termCode, String classNumber) throws RetrievalException {
		try {
			Document doc = fetchInfo(termCode, classNumber);
			Node node = XPathAPI.selectSingleNode(doc, "//TR[TD/@class='classNbrColumnValue']/TD[@class='availableSeatsColumnValue']/TABLE/TBODY/TR/TD/SPAN/SPAN[@class='icontip']");
			if (node == null) {
				logger.trace("couldn't find the node, returning null");
				return null;
			}
			String status = node.getAttributes().getNamedItem("rel").getTextContent();
			if (status.equals("#tt_seats-open")) {
				logger.info("Class is open");
				return Status.OPEN;
			} else if (status.equals("#tt_seats-reserved")) {
				logger.info("Class is open, but all seats are reserved");
				return Status.CLOSED;
			} else if (status.equals("#tt_seats-closed")) {
				logger.info("Class is closed");
				return Status.CLOSED;
			} else {
				throw new ScrapeException("Unknown status: " + status);
			}
		} catch (TransformerException e) {
			throw new RetrievalException(e);
		}
	}
	
	private Document fetchInfo(String termCode, String classNumber) throws RetrievalException {
		try {
			DefaultHttpClient client = new DefaultHttpClient();
			client.getParams().setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 30000);
			// TODO get the campus/online status from the term code or something
			BasicClientCookie cookie = new BasicClientCookie("onlineCampusSelection", "C");
			cookie.setDomain("webapp4.asu.edu");
			cookie.setPath("/catalog");
			cookie.setSecure(true);
			CookieStore cookieStore = new BasicCookieStore();
			cookieStore.addCookie(cookie);
			client.setCookieStore(cookieStore);
			HttpGet get = new HttpGet("https://webapp4.asu.edu/catalog/classlist?&k=" + classNumber + "&t=" + termCode + "&e=all&init=false&nopassive=true");
			HttpResponse response = client.execute(get);
			InputStream is = response.getEntity().getContent();
			DOMParser parser = new DOMParser();
			// somehow this prevents errors when using xpath
			parser.setFeature("http://xml.org/sax/features/namespaces", false);
			parser.parse(new InputSource(is));
			Document doc = parser.getDocument();
			serializeDoc(doc);
			return doc;
		} catch (IOException e) {
			throw new RetrievalException(e);
		} catch (SAXException e) {
			throw new RetrievalException(e);
		}
	}
}
